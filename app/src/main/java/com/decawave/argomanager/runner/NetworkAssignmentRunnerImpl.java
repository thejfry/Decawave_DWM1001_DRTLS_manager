/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import com.annimon.stream.Stream;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.impl.ConcurrentOperationQueue;
import com.decawave.argomanager.components.impl.ConcurrentOperationQueueImpl;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 * Inspired by FirmwareUpdateRunner, but simpler.
 */
public class NetworkAssignmentRunnerImpl implements NetworkAssignmentRunner {
    private static final ComponentLog log = new ComponentLog(NetworkAssignmentRunnerImpl.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "NA-RNR");
    // retry attempt delays
    private static final int NEXT_CONNECT_ATTEMPT_ON_CONNECTION_FAIL = 3000;
    private static final int CONNECT_RETRY_LIMIT = 2;
    private static final int NETWORK_ASSIGN_CONCURRENCY = BleConstants.MAX_CONCURRENT_CONNECTION_COUNT;


    // dependencies
    private final BleConnectionApi bleConnectionApi;
    private final NetworkNodeManager networkNodeManager;
    // stateless members
    private final short targetNetworkId;
    private final boolean removeNetworkOnFail;

    // stateful members
    private Map<Long, NodeInfo> nodeInfoMap;
    private OverallStatus status;
    private ConcurrentOperationQueue nodeOperationQueue;


    private class NodeInfo {
        Long id;
        String bleAddress;
        NodeAssignStatus nodeAssignStatus;
        int connectAttemptCounter;
        int connectAttemptLimit;
        int lastConnectFailAtCounter;
        int errorCode;
        NetworkNodeConnection connection;
        // state
        LogEntryTag tag;

        NodeInfo(Long id, String bleAddress) {
            this.id = id;
            this.bleAddress = bleAddress;
            this.connectAttemptCounter = 0;
            this.lastConnectFailAtCounter = -1;
            this.connectAttemptLimit = 1 + CONNECT_RETRY_LIMIT;
            this.tag = LogEntryTagFactory.getDeviceLogEntryTag(bleAddress);
        }

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "id=" + Util.formatAsHexa(id) +
                    ", bleAddress='" + bleAddress + '\'' +
                    ", nodeAssignStatus=" + nodeAssignStatus +
                    ", connectAttemptCounter=" + connectAttemptCounter +
                    ", connectAttemptLimit=" + connectAttemptLimit +
                    ", errorCode=" + errorCode +
                    ", connection=" + connection +
                    ", tag=" + tag +
                    '}';
        }
    }

    public NetworkAssignmentRunnerImpl(@NotNull BleConnectionApi bleConnectionApi,
                                       @NotNull NetworkNodeManager networkNodeManager,
                                       short targetNetworkId, boolean removeNetworkOnFail) {
        this.bleConnectionApi = bleConnectionApi;
        this.networkNodeManager = networkNodeManager;
        this.targetNetworkId = targetNetworkId;
        this.status = OverallStatus.NOT_STARTED;
        this.removeNetworkOnFail = removeNetworkOnFail;
    }

    @Override
    public void startAssignment(List<Long> nodeIds) {
        if (Constants.DEBUG) {
            log.d("startAssignment: " + "nodeIds = [" + nodeIds + "]");
            Preconditions.checkState(status == OverallStatus.NOT_STARTED, "status is " + status);
            Preconditions.checkState(!nodeIds.isEmpty(), "why are you passing empty node set?");
        }
        // initialize the state
        this.nodeInfoMap = new HashMap<>();
        this.nodeOperationQueue = new ConcurrentOperationQueueImpl(NETWORK_ASSIGN_CONCURRENCY);
        // overall status
        setStatus(OverallStatus.ASSIGNING);
        // enqueue one-by-one
        for (Long nodeId : nodeIds) {
            NodeInfo nodeInfo = new NodeInfo(nodeId, networkNodeManager.idToBle(nodeId));
            nodeInfoMap.put(nodeId, nodeInfo);
            setNodeStatus(nodeInfo, NodeAssignStatus.PENDING);
            nodeOperationQueue.operationEnqueue(token -> doNodeAssign(nodeInfo, token), ConcurrentOperationQueue.Priority.HIGH, null);
        }
    }

    @Override
    public NodeAssignStatus getNodeAssignStatus(long nodeId) {
        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        return nodeInfo == null ? null : nodeInfo.nodeAssignStatus;
    }

    @Override
    public @NotNull OverallStatus getOverallStatus() {
        return status;
    }

    @Override
    public void terminate() {
        if (Constants.DEBUG) {
            Preconditions.checkState(status == OverallStatus.ASSIGNING, "status is " + status);
        }
        appLog.imp("terminated");
        // set particular task states
        for (NodeInfo nodeInfo : nodeInfoMap.values()) {
            if (nodeInfo.nodeAssignStatus == null || !nodeInfo.nodeAssignStatus.terminal) {
                setNodeStatus(nodeInfo, NodeAssignStatus.CANCELLED);
            }
        }
        // set overall status
        setStatus(OverallStatus.TERMINATED);
        // the remaining tasks in the queue will terminate by themselves sooner or later
    }

    private void finalCleanup() {
        if (!com.annimon.stream.Stream.of(nodeInfoMap.values()).anyMatch(nodeInfo -> nodeInfo.nodeAssignStatus == NodeAssignStatus.SUCCESS)
                && removeNetworkOnFail) {
            // no success at all, remove the network
            networkNodeManager.removeNetwork(targetNetworkId, false);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // internal routines
    ///////////////////////////////////////////////////////////////////////////

    private void doNodeAssign(NodeInfo nodeInfo, ConcurrentOperationQueue.Token token) {
        if (status == OverallStatus.TERMINATED) {
            log.d("quitting from startNodeAssign, we are already terminated");
            return;
        }
        // check the network id stored in the network manager
        NetworkNodeEnhanced nne = networkNodeManager.getNode(nodeInfo.id);
        Short nnid = nne.asPlainNode().getNetworkId();
        if (networkIdMatchesWithTarget(nnid)) {
            // skip this one and process the next one
            setNodeStatus(nodeInfo, NodeAssignStatus.SUCCESS);
            nodeOperationQueue.onOperationFinished(token);
            return;
        }
        if (nodeInfo.nodeAssignStatus == NodeAssignStatus.PENDING) {
            setNodeStatus(nodeInfo, NodeAssignStatus.INITIATING);
        }
        // check which firmware needs to be updated
        nodeInfo.connectAttemptCounter++;
        appLog.d("trying NETWORK ASSIGNMENT of " + nodeInfo.bleAddress + ", connect attempt number "
                + nodeInfo.connectAttemptCounter + " (limit " + nodeInfo.connectAttemptLimit + ")", nodeInfo.tag);
        boolean connected[] = { false };
        bleConnectionApi.connect(nodeInfo.bleAddress, ConnectPriority.HIGH,
            (nnc) -> {
                    // on connected
                    connected[0] = true;
                    // adjust the attempt counters
                    if (nodeInfo.lastConnectFailAtCounter == nodeInfo.connectAttemptCounter - 1) {
                        // previous connection attempt failed, something has changed, reset the limit now
                        nodeInfo.connectAttemptLimit = nodeInfo.connectAttemptCounter + CONNECT_RETRY_LIMIT;
                    }
                    // main routine
                    onConnectedToNode(nodeInfo, nnc);
            }, (connection,fail) -> {
                    // onFail
                    genericOnFail(nodeInfo, "connection to " + nodeInfo.bleAddress + " failed");
            }, (nnc,err) -> {
                    // on disconnected
                    if (!connected[0]) {
                        nodeInfo.lastConnectFailAtCounter = nodeInfo.connectAttemptCounter;
                    }
                    if (nodeInfo.nodeAssignStatus.terminal) {
                        // we are done, try the next in the row
                        nodeOperationQueue.onOperationFinished(token);
                    } else {
                        // else: we will try again
                        uiHandler.postDelayed(() -> doNodeAssign(nodeInfo, token), NEXT_CONNECT_ATTEMPT_ON_CONNECTION_FAIL);
                    }
            }
        );
    }

    private boolean networkIdMatchesWithTarget(Short nnid) {
        return Objects.equals(targetNetworkId, nnid) || (targetNetworkId == 0 && nnid == null);
    }

    private void onConnectedToNode(NodeInfo nodeInfo, NetworkNodeConnection nnc) {
        if (Constants.DEBUG) {
            log.d("onConnectedToNode: " + "nodeInfo = [" + nodeInfo + "]");
        }
        // set up the connection first
        nodeInfo.connection = nnc;
        // determine what should we do next
        log.d("node info: " + nodeInfo);
        switch (nodeInfo.nodeAssignStatus) {
            case INITIATING:
            case ASSIGNING:
                // terminated/broken assignment
                startNetworkAssign(nodeInfo);
                break;
            case CANCELLED:
                // skip this one, we are not interested anymore
                nnc.disconnect();
                break;
            case SUCCESS:
            case FAIL:
            case PENDING:
            default:
                throw new IllegalStateException("unexpected node update status: " + nodeInfo.nodeAssignStatus);
        }
    }

    private void genericOnFail(NodeInfo nodeInfo, String activityDescription) {
        if (nodeInfo.nodeAssignStatus != NodeAssignStatus.CANCELLED) {
            if (nodeInfo.connectAttemptCounter == nodeInfo.connectAttemptLimit) {
                log.d(activityDescription + ", connect attempt counter reached max level, giving up");
                setNodeStatus(nodeInfo, NodeAssignStatus.FAIL);
            } else {
                log.d(activityDescription + ", trying again later");
                // the retry is implemented in onDisconnected handler
            }
        }
    }

    private void startNetworkAssign(NodeInfo nodeInfo) {
        if (Constants.DEBUG) {
            log.d("startNetworkAssign: " + "nodeInfo = [" + nodeInfo + "]");
        }
        if (nodeInfo.nodeAssignStatus == NodeAssignStatus.CANCELLED) {
            // skip this one
            nodeInfo.connection.disconnect();
            return;
        }
        // retrieve information about the firmware
        nodeInfo.connection.getOtherSideEntity(networkNode -> {
                    // success
                    if (nodeInfo.nodeAssignStatus == NodeAssignStatus.CANCELLED) {
                        // skip this one
                        nodeInfo.connection.disconnect();
                        return;
                    }
                    if (networkIdMatchesWithTarget(networkNode.getNetworkId())) {
                        // it is already in proper network
                        setNodeStatus(nodeInfo, NodeAssignStatus.SUCCESS);
                        return;
                    }
                    networkNode.setNetworkId(targetNetworkId);
                    nodeInfo.connection.updateOtherSideEntity(networkNode, false, writeEffect -> {
                        // success
                        setNodeStatus(nodeInfo, NodeAssignStatus.SUCCESS);
                        networkNodeManager.onNodeIntercepted(networkNode);
                        nodeInfo.connection.disconnect();
                    }, fail -> {
                        // fail
                        genericOnFail(nodeInfo, "updating network id");
                    });
                }, fail -> {
                    // fail
                    genericOnFail(nodeInfo, "updating network id");
                },
                NetworkNodeProperty.NETWORK_ID
        );
    }

    private void setNodeStatus(NodeInfo nodeInfo, NodeAssignStatus newStatus) {
        if (nodeInfo.nodeAssignStatus != newStatus) {
            nodeInfo.nodeAssignStatus = newStatus;
            InterfaceHub.getHandlerHub(IhNetworkAssignmentRunnerListener.class).onNodeStatusChanged(nodeInfo.id);
            // check if this means overall state change
            if (newStatus.terminal && allNodeStatusesFinished()) {
                setStatus(OverallStatus.FINISHED);
            }
        }
    }

    private boolean allNodeStatusesFinished() {
        return !Stream.of(nodeInfoMap.values()).anyMatch((nodeInfo) -> !nodeInfo.nodeAssignStatus.terminal);
    }

    private void setStatus(OverallStatus newStatus) {
        if (Constants.DEBUG) {
            log.d("setStatus: " + "newStatus = [" + newStatus + "]");
        }
        if (status != newStatus) {
            // cleanup?
            if (!status.terminal && newStatus.terminal) {
                finalCleanup();
            }
            status = newStatus;
            // broadcast
            InterfaceHub.getHandlerHub(IhNetworkAssignmentRunnerListener.class).onNetworkAssignmentStatusChanged(status);
        }
    }

    @Override
    public String toString() {
        return "NetworkAssignmentRunnerImpl{" +
                "status=" + status +
                '}';
    }
}
