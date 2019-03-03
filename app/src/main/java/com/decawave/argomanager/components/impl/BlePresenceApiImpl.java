    /*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.os.SystemClock;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.DiscoveryApiBleImpl;
import com.decawave.argomanager.argoapi.ble.IhConnectionStateListener;
import com.decawave.argomanager.argoapi.ble.connection.BleConnectionApiImpl;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreterImpl;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.struct.PresenceStatus;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action1;

/**
 * Argo project.
 *
 * BLE presence API evaluates node presence based on:
 * 1. information about BLE address presence from the low-level discovery API
 * 2. existence of connection to that particular node
 *
 * The presence API operates on low/BLE address level.
 * The discovery API might provide more details about particular node -
 * if connection to that node succeeded and more information
 * is available.
 *
 */
@Singleton
public class BlePresenceApiImpl implements BlePresenceApi, DiscoveryApiBleImpl.DiscoveryEventListener, BleConnectionApiImpl.ObservationListener {
    private static final ComponentLog log = new ComponentLog(BlePresenceApiImpl.class);
    //
    private static final int PRESENCE_TIMEOUT_PERSISTENT = 15000;
    private static final int PRESENCE_TIMEOUT_TRANSIENT = 30000;
        // it should survive till the end of scan period
    private static final int PRESENCE_TIMEOUT_ON_DISCONNECT_PERSISTENT = 7500;
    private static final int PRESENCE_TIMEOUT_ON_DISCONNECT_TRANSIENT = 15000;
    private static final int AGING_PERIOD = 6500;
    private static final int MAX_AGING_EXPONENT = 4;
    // dependencies
    private final NetworkNodeManager networkNodeManager;
    private final DiscoveryApi discoveryApi;
    private final BleConnectionApi bleConnectionApi;

    // present nodes
    private Set<String> reportedPresentNodes;
    // nodes which are confirmed to be present in this discovery session
    private Set<String> confirmedDiscoverySessionPresentNodes;
    // node's rssi
    private Map<String, Integer> nodeRssiMap;
    // node presence info
    private Map<String, NodePresenceInfo> nodePresenceInfoMap;
    // proxied
    private Map<String, Long> proxiedExpirations;
    private Set<String> directlyTrackedTags;

    private Action1<String> nodePresentCallback;
    private Action1<String> nodeMissingCallback;

    class NodePresenceInfo {
        long expirationSysTime = 0;
        boolean lastConnectSuccessful = false;

        boolean isExpired(long now) {
            return now > expirationSysTime;
        }
    }

    @Inject
    BlePresenceApiImpl(DiscoveryApi discoveryApi, NetworkNodeManager networkNodeManager, BleConnectionApi bleConnectionApi) {
        this.reportedPresentNodes = new HashSet<>();
        this.confirmedDiscoverySessionPresentNodes = new HashSet<>();
        this.nodeRssiMap = new HashMap<>();
        this.proxiedExpirations = new HashMap<>();
        this.directlyTrackedTags = new HashSet<>();
        this.discoveryApi = discoveryApi;
        this.networkNodeManager = networkNodeManager;
        this.bleConnectionApi = bleConnectionApi;
        this.nodePresenceInfoMap = new HashMap<>();
        // hook into the discovery API
        ((DiscoveryApiBleImpl) this.discoveryApi).setDiscoveryEventListener(this);
        ((BleConnectionApiImpl) this.bleConnectionApi).setObservationListener(this);
    }

    @Override
    public void onJustProxied(String bleAddress, int timeout) {
        proxiedExpirations.put(bleAddress, SystemClock.uptimeMillis() + timeout);
    }

    @Override
    public void onDirectLocationObservationStart(String bleAddress) {
        if (Constants.DEBUG) {
            log.d("onDirectLocationObservationStart() called with: " + "bleAddress = [" + bleAddress + "]");
            Preconditions.checkState(!directlyTrackedTags.contains(bleAddress), "tag " + bleAddress + " is already tracked");
        }
        directlyTrackedTags.add(bleAddress);
        InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onTagDirectObserve(bleAddress, true);
    }

    @Override
    public void onDirectLocationObservationStop(String bleAddress) {
        if (Constants.DEBUG) log.d("onDirectLocationObservationStop() called with: " + "bleAddress = [" + bleAddress + "]");
        if (directlyTrackedTags.remove(bleAddress)) {
            InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onTagDirectObserve(bleAddress, false);
        }
    }

    @Override
    public void onDirectLocationObservationFailed(String bleAddress) {
        if (Constants.DEBUG) log.d("onDirectLocationObservationFailed() called with: " + "bleAddress = [" + bleAddress + "]");
        if (directlyTrackedTags.remove(bleAddress)) {
            InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onTagDirectObserve(bleAddress, false);
        }
    }

    @Override
    public boolean isTagTrackedViaProxy(String tagBleAddress) {
        Long expSysTime = proxiedExpirations.get(tagBleAddress);
        return expSysTime != null && SystemClock.uptimeMillis() <= expSysTime;
    }

    @Override
    public boolean isTagTrackedDirectly(String tagBleAddress) {
        return directlyTrackedTags.contains(tagBleAddress);
    }

    private void evaluateExpiredNodes() {
        if (Constants.DEBUG) {
            log.d("evaluateExpiredNodes");
        }
        long now = SystemClock.uptimeMillis();
        for (Map.Entry<String, NodePresenceInfo> stringNodePresenceInfoEntry : nodePresenceInfoMap.entrySet()) {
            String ble = stringNodePresenceInfoEntry.getKey();
            if (reportedPresentNodes.contains(ble) && bleConnectionApi.getConnectionState(ble).disconnected) {
                // it makes sense to evaluate expiration
                if (stringNodePresenceInfoEntry.getValue().isExpired(now)) {
                    onNodeMissing(ble);
                }
            } // multi-else: node is considered present or already expired
        }
    }

    private void onNodeMissing(String bleAddress) {
        if (Constants.DEBUG) log.d("onNodeMissing() called with: " + "bleAddress = [" + bleAddress + "]");
        boolean b = reportedPresentNodes.remove(bleAddress);
        if (Constants.DEBUG) {
            Preconditions.checkState(b, "node " + bleAddress + " not present?!");
        }
        // check callback
        if (nodeMissingCallback != null) {
            nodeMissingCallback.call(bleAddress);
        }
        // now broadcast
        InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onNodeMissing(bleAddress);
    }

    void setNodeMissingCallback(Action1<String> nodeMissingCallback) {
        if (Constants.DEBUG) {
            Preconditions.checkState(this.nodeMissingCallback == null, "double invocation of setNodeMissingCallback? FIXME!");
        }
        this.nodeMissingCallback = nodeMissingCallback;
    }

    void setNodePresentCallback(Action1<String> nodePresentCallback) {
        if (Constants.DEBUG) {
            Preconditions.checkState(this.nodePresentCallback == null, "double invocation of setNodePresentCallback? FIXME!");
        }
        this.nodePresentCallback = nodePresentCallback;
    }

    @Override
    public void init() {
        //
        InterfaceHub.registerHandler(new IhConnectionStateListener() {

            @Override
            public void onConnected(String bleAddress) {
                // just check reported nodes
                if (!reportedPresentNodes.contains(bleAddress)) {
                    // we haven't reported this node to be present, broadcast presence
                    reportedPresentNodes.add(bleAddress);
                    broadcastNodePresent(bleAddress);
                }
                // we do not need to set expiration/timeout - it will be set in onDisconnected
                getOrCreatePresenceInfo(bleAddress).lastConnectSuccessful = true;
            }

            @Override
            public void onDisconnected(String bleAddress, Boolean sessionSuccess) {
                long timeout = -1;
                if (sessionSuccess != null) {
                    // the connection happened
                    timeout = isPersistent(bleAddress) ? PRESENCE_TIMEOUT_ON_DISCONNECT_PERSISTENT
                            : PRESENCE_TIMEOUT_ON_DISCONNECT_TRANSIENT;
                }
                if (timeout != -1) {
                    setPresenceInfoExpiration(bleAddress, timeout);
                }
            }

            @Override
            public void onConnecting(String bleAddress) {
                // handle connect status
                getOrCreatePresenceInfo(bleAddress).lastConnectSuccessful = false;
            }

            @Override
            public void onDisconnecting(String address) {
                // nothing
            }
        });
    }

    private NodePresenceInfo getOrCreatePresenceInfo(String ble) {
        NodePresenceInfo info = nodePresenceInfoMap.get(ble);
        if (info == null) {
            info = new NodePresenceInfo();
            nodePresenceInfoMap.put(ble, info);
        }
        return info;
    }

    private void setPresenceInfoExpiration(String ble, long timeout) {
        getOrCreatePresenceInfo(ble).expirationSysTime = SystemClock.uptimeMillis() + timeout;
    }

    @Override
    public void onJustSeen(String bleAddress, int rssi) {
        if (Constants.DEBUG) {
            log.d("onJustSeen: " + "bleAddress = [" + bleAddress + "], rssi = [" + rssi + "]");
        }
        // our internal processing
        boolean contains = reportedPresentNodes.contains(bleAddress);
        if (!contains) {
            reportedPresentNodes.add(bleAddress);
        }
        //
        setPresenceInfoExpiration(bleAddress, networkNodeManager.isNodePersisted(bleAddress) ? PRESENCE_TIMEOUT_PERSISTENT : PRESENCE_TIMEOUT_TRANSIENT);
        Integer oldRssi = nodeRssiMap.put(bleAddress, rssi);
        // confirm this node
        confirmedDiscoverySessionPresentNodes.add(bleAddress);
        // set last seen
        networkNodeManager.updateLastSeen(bleAddress);
        // broadcast
        if (!contains) {
            broadcastNodePresent(bleAddress);
        }
        // let the network repository know (if it has the representation already)
        if (!Objects.equal(oldRssi, rssi)) {
            InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onNodeRssiChanged(bleAddress, rssi);
        }
    }

    @Override
    public void onDiscoveryStarted() {
        // reset the confirmed discovery session nodes
        confirmedDiscoverySessionPresentNodes.clear();
    }

    @Override
    public void onDiscoveryStopped() {
        // do nothing
    }

    @Override
    public void onScanStarted() {
        // do nothing
    }

    @Override
    public void onScanStopped() {
        // simply evaluate which nodes are considered expired
        evaluateExpiredNodes();
    }

    private void broadcastNodePresent(String bleAddress) {
        // check callback
        if (nodePresentCallback != null) {
            nodePresentCallback.call(bleAddress);
        }
        // report the new node
        InterfaceHub.getHandlerHub(IhPresenceApiListener.class).onNodePresent(bleAddress);
    }

    @Override
    public boolean isNodePresent(String nodeBleAddress) {
        return getNodeStatus(nodeBleAddress).present;
    }

    @Override
    public Set<String> getPresentNodes() {
        return Collections.unmodifiableSet(reportedPresentNodes);
    }

    @Override
    public Integer getNodeRssi(String bleAddress) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(bleAddress);
        }
        return nodeRssiMap.get(bleAddress);
    }

    @Override
    public Integer getAgingNodeRssi(@NotNull String bleAddress) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(bleAddress);
        }
        Integer rssi = nodeRssiMap.get(bleAddress);
        if (rssi != null) {
            // adjust it based on the last-seen (we must have last-seen if we have non-null rssi)
            long seenBeforeExtraPlus = networkNodeManager.getNode(bleAddress).getLastSeen() + BleConstants.BLE_DISCOVERY_NOSCAN_MIN_PERIOD_MS - System.currentTimeMillis();
            if (seenBeforeExtraPlus > 0 && rssi > SignalStrengthInterpreterImpl.VERY_LOW_RSSI) {
                // the stored rssi is now a bit deprecated (begin aging)
                if (seenBeforeExtraPlus > AGING_PERIOD) {
                    // return the lowest value
                    rssi = SignalStrengthInterpreterImpl.VERY_LOW_RSSI;
                } else {
                    int rssiDiff = rssi - SignalStrengthInterpreterImpl.VERY_LOW_RSSI;
                    // the farther we are from lastSeen the closer we are to VERY_LOW_RSSI
                    float exponent = seenBeforeExtraPlus / (((float) AGING_PERIOD) / MAX_AGING_EXPONENT);
                    rssi = SignalStrengthInterpreterImpl.VERY_LOW_RSSI + (int) (rssiDiff / Math.pow(2, exponent));
                }
            }
        }
        if (Constants.DEBUG) {
            log.d("getAgingNodeRssi() " + "bleAddress = [" + bleAddress + "] returning: " + rssi);
        }
        return rssi;
    }

    /**
     * Here is a finer grained presence model - not just YES/NO.
     */
    public PresenceStatus getNodeStatus(String nodeBleAddress) {
        boolean present = reportedPresentNodes.contains(nodeBleAddress);
        if (discoveryApi.isDiscovering()) {
            // if the node is present and is not confirmed yet
            if (present && !confirmedDiscoverySessionPresentNodes.contains(nodeBleAddress)) {
                // we will return PROBABLY_PRESENT
                return PresenceStatus.PROBABLY_PRESENT;
            }
            // we will return YES/NO
            return present ? PresenceStatus.PRESENT : PresenceStatus.MISSING;
        } else {
            // because discovery is stopped now, we cannot be sure about node status
            return present ? PresenceStatus.PROBABLY_PRESENT : PresenceStatus.PROBABLY_MISSING;
        }
    }

    // helper function
    private boolean isPersistent(String nodeBleAddress) {
        return networkNodeManager.isNodePersisted(nodeBleAddress);
    }

}
