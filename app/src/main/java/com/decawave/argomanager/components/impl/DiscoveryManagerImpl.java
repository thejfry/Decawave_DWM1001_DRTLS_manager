/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.ServiceData;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.DiscoveryApiBleImpl;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhNodeDiscoveryListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 */
public class DiscoveryManagerImpl implements DiscoveryManager {
    private static final int DEFAULT_DISCOVERY_DURATION = 15000;
    private static final ComponentLog log = new ComponentLog(DiscoveryManager.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "DSRY");
    // dependencies
    private final DiscoveryApi discoveryApi;
    private final NetworkNodeManager networkNodeManager;
    // members
    private boolean ignoreStopDiscoveryRequests;
    private Object lastStopRequestTag;
    private Set<String> sessionLocalDiscoveredNodes;

    // service data cache - so that the discovery API does not connect to a node if there wasn't a change
    private Map<String, ServiceData> serviceDataCache;
    // keeps the set of recently discovered transient nodes (automatically removed if these nodes are missing for some time)
    // this is in contrast with network node manager, which keeps even transient nodes in memory forever
    private Set<String> discoveredNodes;

    @Inject
    DiscoveryManagerImpl(DiscoveryApi discoveryApi, NetworkNodeManager networkNodeManager, BlePresenceApi blePresenceApi) {
        this.discoveryApi = discoveryApi;
        this.networkNodeManager = networkNodeManager;
        this.discoveredNodes = new HashSet<>();
        this.serviceDataCache = new LinkedHashMap<>();
        this.sessionLocalDiscoveredNodes = new HashSet<>();
        // hack to network node manager to be called about transient node events
        ((NetworkNodeManagerImpl) networkNodeManager).setTransientNodeChangeListener(new TransientNodeChangeHandler() {

            // translate the internal transient node callback events to discovery events
            @Override
            public void onNodeUpdatedAndBecameTransient(NetworkNodeEnhanced node) {
                if (Constants.DEBUG) {
                    log.d("onNodeUpdatedAndBecameTransient: " + "node = [" + node + "]");
                }
                handleNodeBecameTransient(node);
            }

            @Override
            public boolean nodeAboutToBePersisted(String bleAddress) {
                if (Constants.DEBUG) {
                    log.d("nodeAboutToBePersisted: " + "bleAddress = [" + bleAddress + "]");
                }
                boolean isDiscovered = discoveredNodes.contains(bleAddress);
                // we have to remove the node in each case
                onObsoleteTransientNode(bleAddress);
                // we allow only discovered (present) nodes to be added to the network
                return isDiscovered;
            }

            @Override
            public void onNodeUpdatedAndBecamePersistent(String bleAddress) {
                if (Constants.DEBUG) {
                    log.d("onNodeUpdatedAndOrBecamePersistent() called with: " + "bleAddress = [" + bleAddress + "]");
                }
                onObsoleteTransientNode(bleAddress);
            }

            @Override
            public void onNodeUpdated(NetworkNodeEnhanced node) {
                if (Constants.DEBUG) {
                    log.d("onNodeUpdated: " + "node = [" + node + "]");
                }
                String bleAddress = node.getBleAddress();
                if (discoveredNodes.contains(bleAddress)) {
                    InterfaceHub.getHandlerHub(IhNodeDiscoveryListener.class).onDiscoveredNodeUpdate(node.asPlainNode());
                } else {
                    // prepare the place for service data
                    discoveredNodes.add(bleAddress);
                    if (discoveryApi.isDiscovering()) sessionLocalDiscoveredNodes.add(bleAddress);
                    InterfaceHub.getHandlerHub(IhNodeDiscoveryListener.class).onNodeDiscovered(node.asPlainNode());
                }
            }

            @Override
            public void onNetworkRemovedNodeBecameTransient(NetworkNodeEnhanced node) {
                if (Constants.DEBUG) {
                    log.d("onNetworkRemovedNodeBecameTransient: " + "node = [" + node + "]");
                }
                handleNodeBecameTransient(node);
            }

            private void handleNodeBecameTransient(NetworkNodeEnhanced node) {
                String bleAddress = node.getBleAddress();
                // we need to also check nodes' presence
                if (blePresenceApi.isNodePresent(bleAddress) && discoveredNodes.add(bleAddress)) {
                    if (discoveryApi.isDiscovering()) sessionLocalDiscoveredNodes.add(bleAddress);
                    InterfaceHub.getHandlerHub(IhNodeDiscoveryListener.class).onNodeDiscovered(node.asPlainNode());
                }
            }
        });
        // hack to presence API to be called when particular node disappears
        ((BlePresenceApiImpl) blePresenceApi).setNodeMissingCallback((bleAddress) -> {
            if (discoveredNodes.contains(bleAddress)) {
                // we have reported this (transient) node as discovered
                onObsoleteTransientNode(bleAddress);
            }
        });
        ((BlePresenceApiImpl) blePresenceApi).setNodePresentCallback(DiscoveryManagerImpl.this::onNodePresent);

    }

    @Override
    public void startTimeLimitedDiscovery(boolean prolongIfRunning) {
        if (!discoveryApi.isDiscovering()) {
            // un-schedule stop (to not be stopped by previous time limited discovery)
            cancelScheduledDiscoveryStop();
            // start the discovery now
            startDiscovery();
        } else if (prolongIfRunning) {
            // we will prolong the already running discovery
            log.d("prolonging running discovery by " + (long) DEFAULT_DISCOVERY_DURATION + " ms");
            if (isStopping()) {
                continueDiscovery();
            }
            cancelScheduledDiscoveryStop();
        }
        // post runnable to stop discovery after 10 seconds
        scheduleDiscoveryStop((long) DEFAULT_DISCOVERY_DURATION);
    }

    @Override
    public void startDiscovery() {
        // reset the session local set
        sessionLocalDiscoveredNodes.clear();
        // start the discovery
        discoveryApi.startDiscovery((svcData, node) -> {
                    // adjust the session local discovered node set
                    if (!networkNodeManager.isNodePersisted(node.getId())) sessionLocalDiscoveredNodes.add(node.getBleAddress());
                    // cache the service data
                    serviceDataCache.put(node.getBleAddress(), svcData);
                },
                // on fail
                (f) -> appLog.we("TAG/ANCHOR discovery failed", f),
                // prefer nodes which we do not know at all
                (bleAddress) -> networkNodeManager.getNode(bleAddress) != null ? ConnectPriority.LOW : ConnectPriority.MEDIUM,
                // pass service data cache?
                !serviceDataCache.isEmpty() ? serviceDataCache : null);
    }

    @Override
    public void stopDiscovery() {
        if (ignoreStopDiscoveryRequests) {
            log.d("ignoring stopDiscovery() request");
            return;
        }
        if (Constants.DEBUG) {
            Preconditions.checkState(discoveryApi.isDiscovering());
        }
        discoveryApi.stopDiscovery();
    }

    @Override
    public boolean isStopping() {
        return discoveryApi.isStopping();
    }

    @Override
    public void continueDiscovery() {
        discoveryApi.continueDiscovery();
    }

    public void scheduleDiscoveryStop(long duration) {
        // generate the tag
        final Object tag = new Object();
        lastStopRequestTag = tag;
        uiHandler.postDelayed(() -> {
            if (lastStopRequestTag == tag) {
                // there has not been issued any other stop request (or cancelled)
                stopDiscoveryIfRunning();
            }
        }, duration);
    }

    public void cancelScheduledDiscoveryStop() {
        lastStopRequestTag = null;
    }

    @Override
    public void ignoreDiscoveryStopRequests(boolean ignore) {
        // cancel any possible scheduled stop request
        cancelScheduledDiscoveryStop();
        //
        this.ignoreStopDiscoveryRequests = ignore;
    }

    @Override
    public boolean isDiscovering() {
        return discoveryApi.isDiscovering();
    }

    @Override
    public void stopDiscoveryIfRunning() {
        if (ignoreStopDiscoveryRequests) {
            log.d("ignoring stopDiscovery() request from stopDiscoveryRunnable");
            return;
        }
        if (discoveryApi.isDiscovering() && !discoveryApi.isStopping()) {
            discoveryApi.stopDiscovery();
        }
    }

    private void onObsoleteTransientNode(String bleAddress) {
        if (Constants.DEBUG) {
            log.d("onObsoleteTransientNode: " + "bleAddress = [" + bleAddress + "]");
        }
        // remove particular service data from discovered node set if the node is missing on BLE presence API level
        if (discoveredNodes.remove(bleAddress)) {
            // broadcast
            Long nodeId = networkNodeManager.bleToId(bleAddress);
            if (nodeId != null) InterfaceHub.getHandlerHub(IhNodeDiscoveryListener.class).onDiscoveredNodeRemoved(nodeId);
        }
    }

    private void onNodePresent(String bleAddress) {
        if (Constants.DEBUG) {
            log.d("onNodePresent: " + "bleAddress = [" + bleAddress + "]");
        }
        // check if we know this node
        if (networkNodeManager.getNode(bleAddress) == null) {
            if (Constants.DEBUG) {
                log.d("ignoring unknown node " + bleAddress);
            }
            // ignore
            return;
        }
        // check if the node is transient
        if (!networkNodeManager.isNodePersisted(bleAddress) && discoveredNodes.add(bleAddress)) {
            // broadcast that this node has been just discovered
            NetworkNodeEnhanced nne = networkNodeManager.getNode(bleAddress);
            if (nne != null) {
                if (discoveryApi.isDiscovering()) sessionLocalDiscoveredNodes.add(bleAddress);
                InterfaceHub.getHandlerHub(IhNodeDiscoveryListener.class).onNodeDiscovered(nne.asPlainNode());
            }
        }
    }

    public @NotNull
    List<NetworkNode> getDiscoveredTransientOnlyNodes() {
        return Stream.of(discoveredNodes).
                map((bleAddress) -> networkNodeManager.getNode(bleAddress).asPlainNode()).
                collect(Collectors.toList());
    }

    @Override
    public int getNumberOfDiscoverySessionNodes() {
        return sessionLocalDiscoveredNodes.size();
    }

    @Override
    public boolean anyTransientNodeDiscovered() {
        return !discoveredNodes.isEmpty();
    }

    public Object getState() {
        return ((DiscoveryApiBleImpl) discoveryApi).getState();
    }


}
