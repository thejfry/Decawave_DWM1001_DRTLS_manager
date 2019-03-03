/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.os.SystemClock;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.interaction.ProxyPosition;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnection;
import com.decawave.argomanager.ble.ConnectionSpeed;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.PresenceStatus;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;
import static java.lang.Math.signum;

/**
 * The only responsibility of this observer is to get more frequent location
 * data updates of TAG positions than the default ones (caused by discovery).
 * The updates are passed onto network repository from where they can be
 * intercepted by {@link IhPersistedNodeChangeListener}.
 *
 * It does by:
 * 1. starting stateful discovery on the background (if necessary)
 * 2. if suitable ({@link #observableCandidate(NetworkNodeEnhanced)} establish connection and
 *    intercept position and distance changes
 * 3. the changes are automatically propagated to network node repository as well as
 *    location data logger (the updates are therefore interceptable on network via
 *    {@link IhPersistedNodeChangeListener})
 */
@Singleton
public class LocationDataObserverImpl implements LocationDataObserver {
    private static final ComponentLog log = new ComponentLog(LocationDataObserverImpl.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newPositionLog(log);
    private static final int RETRY_OBSERVE_EACH_MS = 5000;
    // dependencies
    private final BleConnectionApi bleConnectionApi;
    private final DiscoveryManager discoveryManager;
    private final NetworkNodeManager networkNodeManager;
    private final BlePresenceApi blePresenceApi;
    private final AppPreferenceAccessor appPreferenceAccessor;
    private final Comparator<NetworkNodeEnhanced> observableCandidateComparator;
    private final NodeFitnessEvaluator fitnessEvaluator;
    // members
    private Map<Long, NetworkNodeConnection> connectionByNetworkNodeId = Maps.newHashMap();
    private boolean observing = false;
    private Object tag;
    private int shortenObservePeriodBy;
    private String preferentiallyObservedNode;
    private long preferentiallyObservedNodeTimestamp;

    private Runnable rescanObserveAndSchedule = new Runnable() {
        @Override
        public void run() {
            if (Constants.DEBUG) log.d("rescanObserveAndSchedule");
            Stream.of(blePresenceApi.getPresentNodes())
                    .map(networkNodeManager::getNode)
                    // bugfix: it is possible that the node is not registered/persisted in repository yet
                    .filter(LocationDataObserverImpl::nonNull)
                    .filter(LocationDataObserverImpl.this::observableCandidate)
                    .sorted(observableCandidateComparator)
                    .forEach(LocationDataObserverImpl.this::startLocationObservation);
            uiHandler.postDelayed(this, RETRY_OBSERVE_EACH_MS - shortenObservePeriodBy);
            // exponential back-off
            shortenObservePeriodBy /= 2;
        }
    };

    private static boolean nonNull(Object o) {
        return o != null;
    }

    @Inject
    LocationDataObserverImpl(BleConnectionApi bleConnectionApi,
                             DiscoveryManager discoveryManager,
                             NetworkNodeManager networkNodeManager,
                             BlePresenceApi blePresenceApi,
                             AppPreferenceAccessor appPreferenceAccessor) {
        this.bleConnectionApi = bleConnectionApi;
        this.discoveryManager = discoveryManager;
        this.networkNodeManager = networkNodeManager;
        this.blePresenceApi = blePresenceApi;
        this.appPreferenceAccessor = appPreferenceAccessor;
        this.fitnessEvaluator = new NodeFitnessEvaluator(blePresenceApi);
        //
        this.observableCandidateComparator = (o1, o2) -> {
            // passive UWB anchor nodes (position proxies) go first
            NetworkNode op1 = o1.asPlainNode();
            NetworkNode op2 = o2.asPlainNode();
            UwbMode uwbMode1 = op1.getUwbMode();
            UwbMode uwbMode2 = op2.getUwbMode();
            if (op1.isAnchor() && op2.isAnchor()) {
                // both are anchors, UWB must be passive
                if (Constants.DEBUG) {
                    Preconditions.checkState(uwbMode1 == UwbMode.PASSIVE);
                    Preconditions.checkState(uwbMode2 == UwbMode.PASSIVE);
                }
                // evaluate/compare fitness
                return compareFitness(op1, op2);
            } else if (op1.isAnchor()) {
                // isAnchor == Uwb mode PASSIVE
                return -1;
            } else if (op2.isAnchor()) {
                // isAnchor == Uwb mode PASSIVE
                return 1;
            } // else: both are tags
            // tags with RA tracked go first
            boolean tra1 = o1.getTrackMode() == TrackMode.TRACKED_POSITION_AND_RANGING;
            boolean tra2 = o2.getTrackMode() == TrackMode.TRACKED_POSITION_AND_RANGING;
            if (Constants.DEBUG) {
                Preconditions.checkState(!tra1 || o1.isTag(), "directly tracked can be only tag: " + o1);
                Preconditions.checkState(!tra2 || o2.isTag(), "directly tracked can be only tag: " + o2);
            }
            if (tra1 != tra2) {
                return tra1 ? -1 : 1;
            } // else: prefer the preferentially observed node
            if (preferentiallyObservedNodeTimestamp + 60000 > SystemClock.uptimeMillis()) {
                if (o1.getBleAddress().equals(preferentiallyObservedNode)) {
                    return -1;
                } else if (o2.getBleAddress().equals(preferentiallyObservedNode)) {
                    return 1;
                }
            }
            // else: compare by fitness
            return compareFitness(op1, op2);
        };
    }

    private int compareFitness(NetworkNode n1, NetworkNode n2) {
        float f1 = fitnessEvaluator.getNodeFitness(n1.getBleAddress());
        float f2 = fitnessEvaluator.getNodeFitness(n2.getBleAddress());
        // prefer larger values
        return (int) signum(f2 - f1);
    }


    @Override
    public void setPreferentiallyObservedNode(String bleAddress) {
        this.preferentiallyObservedNode = bleAddress;
        this.preferentiallyObservedNodeTimestamp = SystemClock.uptimeMillis();
    }

    @Override
    public void startObserve() {
        if (Constants.DEBUG) {
            log.d("startObserve()");
            Preconditions.checkState(!observing);
        }
        // generate new tag per each observe
        tag = new Object();
        if (discoveryManager.isStopping()) {
            // revert the stopping state
            discoveryManager.continueDiscovery();
        } else if (!discoveryManager.isDiscovering()) {
            // start the node discovery
            discoveryManager.startDiscovery();
        } // else: we are discovering
        // make sure that the discovery will not stop
        discoveryManager.ignoreDiscoveryStopRequests(true);
        observing = true;
        // initial run
        shortenObservePeriodBy = RETRY_OBSERVE_EACH_MS / 2;
        rescanObserveAndSchedule.run();
    }

    @Override
    public boolean isObserving() {
        if (Constants.DEBUG) {
            if (observing) {
                Preconditions.checkState(discoveryManager.isDiscovering(), "discovery manager state: " + ((DiscoveryManagerImpl) discoveryManager).getState());
            }
        }
        return observing;
    }

    @Override
    public void stopObserve() {
        if (Constants.DEBUG) {
            log.d("stopObserve()");
            Preconditions.checkState(observing);
            Preconditions.checkState(discoveryManager.isDiscovering());
            Preconditions.checkState(!discoveryManager.isStopping());
        }
        // set flag
        observing = false;
        uiHandler.removeCallbacks(rescanObserveAndSchedule);
        // cancel stop discovery request ignore
        discoveryManager.ignoreDiscoveryStopRequests(false);
        // schedule discovery stop in 2s
        discoveryManager.scheduleDiscoveryStop(2000);
        // stop connections one by one
        for (NetworkNodeConnection connection : connectionByNetworkNodeId.values()) {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        }
        connectionByNetworkNodeId.clear();
    }


    private boolean observableCandidate(NetworkNodeEnhanced nodeEnhanced) {
        NetworkNode node = nodeEnhanced.asPlainNode();
        // either it is a tag with UWB PASSIVE or ACTIVE or it is an anchor in UWB PASSIVE mode
        String bleAddress = node.getBleAddress();
        return ((node.isTag() && node.getUwbMode() == UwbMode.ACTIVE && nodeEnhanced.getTrackMode().tracked) || (node.isAnchor() && node.getUwbMode() == UwbMode.PASSIVE))
                // the node must be PRESENT (not PROBABLY_PRESENT)
                && blePresenceApi.getNodeStatus(bleAddress) == PresenceStatus.PRESENT
                // we must not have a connection to this node
                && !connectionByNetworkNodeId.containsKey(node.getId())
                // the node must belong to the active network
                && java.util.Objects.equals(node.getNetworkId(), appPreferenceAccessor.getActiveNetworkId());
    }


    private void startLocationObservation(final NetworkNodeEnhanced nodeEnhanced) {
        if (Constants.DEBUG) {
            log.d("startLocationObservation() called with: " + "node = [" + nodeEnhanced + "]");
            Preconditions.checkState(!connectionByNetworkNodeId.containsKey(nodeEnhanced.getId()), "there already is a connection made to " + nodeEnhanced);
        }
        // check how many connections are there already running
        List<NetworkNodeConnection> tagConnections = getConnections(NetworkNodeEnhanced::isTag);
        List<NetworkNodeConnection> proxyConnections = getConnections(NetworkNodeEnhanced::isAnchor);
        long tagConnectionCount = tagConnections.size();
        long proxyConnectionCount = proxyConnections.size();
        //
        NetworkNode node = nodeEnhanced.asPlainNode();
        String bleAddress = node.getBleAddress();
        if (nodeEnhanced.isAnchor()) {
            // this is an anchor = proxy
            if (Constants.DEBUG) {
                // check that this is a proxy
                Preconditions.checkState(node.getUwbMode() == UwbMode.PASSIVE, "how comes that this node was not filtered out?: " + node);
            }
            // check if we have already a proxy connection request enqueued/running
            if (proxyConnectionCount == 0) {
                // connect to the proxy (immediately or enqueue the connect request)
                if (Constants.DEBUG) {
                    log.d("initiating position proxy connection " + bleAddress);
                }
                startPositionProxyObservation(node);
            } else {
                // else: there already is an initiated connection to proxy
                // optimization: check that the proxy connection is already established
                NetworkNodeConnection pendingProxyConnection = proxyConnections.get(0);
                if (pendingProxyConnection.getState() == ConnectionState.PENDING) {
                    // evaluate fitness
                    String pendingNodeAddress = pendingProxyConnection.getOtherSideAddress();
                    float pendingNodeFitness = fitnessEvaluator.getNodeFitness(pendingNodeAddress);
                    float candidateFitness = fitnessEvaluator.getNodeFitness(bleAddress);
                    NetworkNodeEnhanced pendingNode = networkNodeManager.getNode(pendingNodeAddress);
                    if (candidateFitness > pendingNodeFitness || !pendingNode.isAnchor() || pendingNode.asPlainNode().getUwbMode() != UwbMode.PASSIVE) {
                        if (Constants.DEBUG) {
                            log.d("found a new proxy candidate " + bleAddress
                                    + " , cancelling PENDING proxy connect request " + pendingNodeAddress);
                        }
                        // the signal level is significantly bigger, or the pending node changed it's properties
                        // and is not suitable for position proxying anymore
                        // cancel the pending/enqueued connect request
                        pendingProxyConnection.disconnect();
                        // enqueue the new connect request
                        startPositionProxyObservation(node);
                    }
                }
            }
        } else {
            // this node is a tag
            // check if this is a tag configured to be tracked directly
            TrackMode trackMode = networkNodeManager.getNode(bleAddress).getTrackMode();
            if (trackMode == TrackMode.TRACKED_POSITION_AND_RANGING) {
                // we must track this node directly
                startDirectLocationDataObservation(node);
            } else if (trackMode == TrackMode.TRACKED_POSITION) {
                // the node is not configured to be tracked directly
                if (tagConnectionCount < BleConstants.LOCATION_DATA_OBSERVER_MAX_TRACKED_TAGS_COUNT) {
                    // we CAN track this node directly (we have not reached the limit yet)
                    startDirectLocationDataObservation(node);
                } else {
                    if (Constants.DEBUG) {
                        log.d("skipping location data observe candidate " + bleAddress + " no free connection slot");
                    }
                }
            }
        }
    }

    private List<NetworkNodeConnection> getConnections(Predicate<NetworkNodeEnhanced> filter) {
        return Stream.of(connectionByNetworkNodeId.values())
                .filter((conn) -> filter.test(networkNodeManager.getNode(conn.getOtherSideAddress())))
                .toList();
    }

    private void startDirectLocationDataObservation(NetworkNode node) {
        if (Constants.DEBUG) {
            log.d("startDirectLocationDataObservation: " + "node = [" + node + "]");
        }
        boolean[] startedObservation = { false };
        final Object tag = this.tag;
        // connect and start observation
        NetworkNodeConnection newConnection = bleConnectionApi.connect(node.getBleAddress(), ConnectPriority.MEDIUM,
                // onConnected
                (nodeConnection) -> {
                    if (tag != LocationDataObserverImpl.this.tag) {
                        // ignore
                        log.d("ignoring overlapping observe callback invocation");
                        // disconnect from this node, let it connect inside proper invocation
                        nodeConnection.disconnect();
                        return;
                    }
                    if (!observing) {
                        // just disconnect
                        nodeConnection.disconnect();
                        return;
                    }
                    // change connection speed
                    ((NetworkNodeBleConnection) nodeConnection).setConnectionSpeed(ConnectionSpeed.HIGH);
                    // check location data mode
                    nodeConnection.getOtherSideEntity((networkNode) -> {
                            // onSuccess
                            if (networkNode.getLocationDataMode() != LocationDataMode.POSITION_AND_DISTANCES) {
                                log.d("need to change location data mode of " + networkNode.getBleAddress() + " first, current mode: " + networkNode.getLocationDataMode());
                                // we need to change the mode first
                                networkNode.setLocationDataMode(LocationDataMode.POSITION_AND_DISTANCES);
                                nodeConnection.updateOtherSideEntity(networkNode, false, (writeEffect) -> {
                                    // onsuccess
                                    if (tag == LocationDataObserverImpl.this.tag) {
                                        startLocationDataObserve(node, startedObservation, tag, nodeConnection);
                                    } else {
                                        if (nodeConnection.isConnected()) {
                                            nodeConnection.disconnect();
                                        }
                                    }
                                }, (fail) -> {
                                    // onfail
                                    if (tag != LocationDataObserverImpl.this.tag) {
                                        // ignore
                                        log.d("ignoring overlapping observe observePosition.onFail() callback invocation");
                                    } else {
                                        log.w("failed to set location data mode before observing position: " + fail);
                                    }
                                    // we will get disconnected automatically
                                });
                            } else {
                                startLocationDataObserve(node, startedObservation, tag, nodeConnection);
                            }
                        }, (fail) -> {
                            // onfail
                            if (tag != LocationDataObserverImpl.this.tag) {
                                // ignore
                                log.d("ignoring overlapping observe getOtherSideEntity.onFail() callback invocation");
                            } else {
                                log.w("failed to retrieve location data mode: " + fail);
                            }
                            // we will get disconnected automatically
                        },
                        // properties
                        NetworkNodeProperty.LOCATION_DATA_MODE
                    );
                },
                // onFail
                (connection,fail) -> {
                    if (tag == LocationDataObserverImpl.this.tag) {
                        log.w("failed to connect to " + node.getBleAddress() + ", " + fail.message + ", trying later");
                    } // else: completely ignore
                },
                // onDisconnected
                (nnc,err) -> {
                    if (startedObservation[0]) {
                        appLog.imp("stopped location data observation of " + node.getBleAddress() + ", (" + Util.shortenNodeId(node.getId(), false) + ")",
                                logTag(node));
                    }
                    if (tag == LocationDataObserverImpl.this.tag) {
                        if (err != null) {
                            // let the fitness evaluator know about potential connection error
                            fitnessEvaluator.onConnectionClosed(nnc.getOtherSideAddress(), err == 0);
                        }
                        connectionByNetworkNodeId.remove(node.getId());
                    }
                }
        );
        // remember the new connection
        NetworkNodeConnection oldConn = connectionByNetworkNodeId.put(node.getId(), newConnection);
        // check validity of the connect request
        Preconditions.checkState(oldConn == null, "there is an existing connection to " + node.getBleAddress() + "?!");
    }

    private void startPositionProxyObservation(NetworkNode node) {
        boolean[] startedObservation = { false };
        final Object tag = this.tag;
        // connect and start observation
        NetworkNodeConnection newConnection = bleConnectionApi.connect(node.getBleAddress(), ConnectPriority.HIGH,
                // onConnected
                (nodeConnection) -> {
                    if (tag != LocationDataObserverImpl.this.tag) {
                        // ignore
                        log.d("ignoring overlapping observe callback invocation");
                        // disconnect from this node, let it connect inside proper invocation
                        nodeConnection.disconnect();
                        return;
                    }
                    if (!observing) {
                        // just disconnect
                        nodeConnection.disconnect();
                        return;
                    }
                    // change connection priority to get the data ASAP
                    ((NetworkNodeBleConnection) nodeConnection).setConnectionSpeed(ConnectionSpeed.HIGH);
                    // check node type and UWB mode
                    nodeConnection.getOtherSideEntity((networkNode) -> {
                                // onSuccess
                                if (!networkNode.isAnchor() || networkNode.getUwbMode() != UwbMode.PASSIVE) {
                                    //
                                    log.d("disconnecting from " + networkNode.getBleAddress() + " was position proxy before connect, now seems to have changed it's properties");
                                } else {
                                    // set up the position proxy observation
                                    startProxyPositionDataObserve(networkNode, startedObservation, tag, nodeConnection);
                                }
                            }, (fail) -> {
                                // onfail
                                if (tag != LocationDataObserverImpl.this.tag) {
                                    // ignore
                                    log.d("ignoring overlapping observe getOtherSideEntity.onFail() callback invocation");
                                } else {
                                    log.w("failed to retrieve node type and UWB mode: " + fail);
                                }
                                // we will get disconnected automatically
                            },
                            // properties
                            NetworkNodeProperty.NODE_TYPE, NetworkNodeProperty.UWB_MODE
                    );
                },
                // onFail
                (connection, fail) -> {
                    if (tag == LocationDataObserverImpl.this.tag) {
                        log.w("failed to connect to " + node.getBleAddress() + ", " + fail.message + ", trying later");
                    } // else: completely ignore
                },
                // onDisconnected
                (nnc,err) -> {
                    if (startedObservation[0]) {
                        appLog.imp("stopped proxy position data observation of " + node.getBleAddress() + ", (" + Util.shortenNodeId(node.getId(), false) + ")",
                                logTag(node));
                    }
                    if (tag == LocationDataObserverImpl.this.tag) {
                        if (err != null) {
                            // let the fitness evaluator know about potential connection error
                            fitnessEvaluator.onConnectionClosed(nnc.getOtherSideAddress(), err == 0);
                        } // else: the connection did not happen, for fitness evaluator this is useless
                        // remove the connection
                        connectionByNetworkNodeId.remove(node.getId());
                    }
                }
        );
        // remember the new connection
        NetworkNodeConnection oldConn = connectionByNetworkNodeId.put(node.getId(), newConnection);
        // check validity of the connect request
        Preconditions.checkState(oldConn == null, "there is an existing connection to " + node.getBleAddress() + "?!");
    }

    private void startLocationDataObserve(final NetworkNode node, final boolean[] startedObservation, final Object tag, final NetworkNodeConnection nodeConnection) {
        // start observation
        nodeConnection.observeLocationData(new NetworkNodeConnection.LocationDataChangedCallback() {

            @Override
            public void onFail(Fail fail) {
                if (tag != LocationDataObserverImpl.this.tag) {
                    // ignore
                    log.d("ignoring overlapping observe observePosition.onFail() callback invocation");
                } else {
                    log.w("failed to observe position: " + fail + ", trying again later");
                }
                // we will get disconnected automatically
            }

            @Override
            public void onStarted() {
                startedObservation[0] = true;
                // do nothing
                appLog.imp("started location data observation of " + node.getBleAddress(), logTag(node));
            }

            @Override
            public void onChange(LocationData locationData) {
                // do nothing special: network node manager and location data logger are already notified
            }

            @Override
            public void onStopped() {
                // it's OK, disconnect first stop observation and then disconnects
            }

        });
    }


    private void startProxyPositionDataObserve(final NetworkNode node, final boolean[] startedObservation, final Object tag, final NetworkNodeConnection nodeConnection) {
        // start observation
        nodeConnection.observeProxyPositionData(new NetworkNodeConnection.ProxyPositionDataChangedCallback() {
            @Override
            public void onStarted() {
                startedObservation[0] = true;
                // do nothing
                appLog.imp("started proxy position observation of " + node.getBleAddress(), logTag(node));
            }

            @Override
            public void onFail(Fail fail) {
                if (tag != LocationDataObserverImpl.this.tag) {
                    // ignore
                    log.d("ignoring overlapping proxy position observe onFail() callback invocation");
                } else {
                    log.w("failed to observe position: " + fail + ", trying again later");
                }
                // we will get disconnected automatically
            }

            @Override
            public void onChange(List<ProxyPosition> newData) {
                // do nothing special: network node manager and location data logger are already notified
                appLog.d("received proxy positions: " + newData, logTag(node));
            }

            @Override
            public void onStopped() {
                // it's OK, disconnect first stop observation and then disconnects
            }
        });
    }

    private static LogEntryTag logTag(NetworkNode node) {
        return LogEntryTagFactory.getDeviceLogEntryTag(node.getBleAddress());
    }
}
