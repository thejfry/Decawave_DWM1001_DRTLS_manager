/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.firmware.Firmware;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 */

public class FirmwareUpdateRunnerImpl implements FirmwareUpdateRunner {
    private static final ComponentLog log = new ComponentLog(FirmwareUpdateRunnerImpl.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "FWUP-RNR");
    // retry attempt delays
    private static final int NEXT_CONNECT_ATTEMPT_ON_CONNECTION_FAIL = 3000;
    private static final int RECONNECT_ATTEMPT = 1500;
    private static final int CONNECT_RETRY_LIMIT = 2;

    // dependencies
    private final BleConnectionApi bleConnectionApi;
    // stateless members
    private final Firmware firmware1;
    private final Firmware firmware2;

    // stateful members
    private Queue<NodeInfo> nodeInfoQueue;
    private Map<Long, NodeInfo> nodeInfoMap;
    private OverallStatus status;

    private class NodeInfo {
        Long id;
        String bleAddress;
        NodeUpdateStatus nodeUpdateStatus;
        int connectAttemptCounter;
        int connectAttemptLimit;
        int lastConnectFailAtCounter;
        int errorCode;
        Integer uploadByteCounter;
        NetworkNodeConnection connection;
        // initial node info
        UwbMode initialUwbMode;
        OperatingFirmware initialOperatingFirmware;
        // workaround firmware issue #69
        Boolean initialLocationEngineFlag;
        //
        Integer fw1Version, fw2Version;
        Integer fw1Checksum, fw2Checksum;
        // state
        LogEntryTag tag;
        int nextConnectDelay;
        Boolean offlineSwitchDone;
        boolean fw1Updated;
        boolean fw2Updated;

        NodeInfo(Long id, String bleAddress) {
            this.id = id;
            this.bleAddress = bleAddress;
            this.connectAttemptCounter = 0;
            this.lastConnectFailAtCounter = -1;
            this.connectAttemptLimit = 1 + CONNECT_RETRY_LIMIT;
            this.nodeUpdateStatus = NodeUpdateStatus.PENDING;
            this.tag = LogEntryTagFactory.getDeviceLogEntryTag(bleAddress);
            this.fw1Updated = false;
            this.fw2Updated = false;
        }

        int getNextConnectDelayAndDestroy(int defaultValue) {
            int r = this.nextConnectDelay;
            this.nextConnectDelay = -1;
            return r == -1 ? defaultValue : r;
        }

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "id=" + Util.formatAsHexa(id) +
                    ", bleAddress='" + bleAddress + '\'' +
                    ", nodeUpdateStatus=" + nodeUpdateStatus +
                    ", fw1Version=" + Util.formatAsHexa(fw1Version) +
                    ", fw2Version=" + Util.formatAsHexa(fw2Version) +
                    ", fw1Checksum=" + Util.formatAsHexa(fw1Checksum) +
                    ", fw2Checksum=" + Util.formatAsHexa(fw2Checksum) +
                    ", connectAttemptCounter=" + connectAttemptCounter +
                    ", connectAttemptLimit=" + connectAttemptLimit +
                    ", errorCode=" + errorCode +
                    ", uploadByteCounter=" + uploadByteCounter +
                    ", connection=" + connection +
                    ", initialUwbMode=" + initialUwbMode +
                    ", initialOperatingFirmware=" + initialOperatingFirmware +
                    ", initialLocationEngineFlag=" + initialLocationEngineFlag +
                    ", tag=" + tag +
                    ", nextConnectDelay=" + nextConnectDelay +
                    ", offlineSwitchDone=" + offlineSwitchDone +
                    ", fw1Updated=" + fw1Updated +
                    ", fw2Updated=" + fw2Updated +
                    '}';
        }
    }

    public FirmwareUpdateRunnerImpl(@NotNull BleConnectionApi bleConnectionApi,
                                    Firmware firmware1,
                                    Firmware firmware2) {
        this.bleConnectionApi = bleConnectionApi;
        this.firmware1 = firmware1;
        this.firmware2 = firmware2;
        this.status = OverallStatus.NOT_STARTED;
    }

    @Override
    public void startFwUpdate(List<NetworkNode> nodes) {
        if (Constants.DEBUG) {
            Preconditions.checkState(status == OverallStatus.NOT_STARTED, "status is " + status);
            Preconditions.checkState(!nodes.isEmpty(), "why are you passing empty node set?");
        }
        this.nodeInfoMap = new HashMap<>();
        this.nodeInfoQueue = new LinkedList<>();
        for (NetworkNode node : nodes) {
            Long nodeId = node.getId();
            NodeInfo nodeInfo = new NodeInfo(nodeId, node.getBleAddress());
            nodeInfoMap.put(nodeId, nodeInfo);
            nodeInfoQueue.add(nodeInfo);
        }
        setStatus(OverallStatus.UPDATING);
        startNextNodeUpdate();
    }

    @Override
    public NodeUpdateStatus getNodeUpdateStatus(long nodeId) {
        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        return nodeInfo == null ? null : nodeInfo.nodeUpdateStatus;
    }

    @Override
    public Integer getUploadByteCounter(long nodeId) {
        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        return nodeInfo == null ? null : nodeInfo.uploadByteCounter;
    }

    @Override
    public OverallStatus getOverallStatus() {
        return status;
    }

    @Override
    public void terminate() {
        if (Constants.DEBUG) {
            Preconditions.checkState(status == OverallStatus.UPDATING, "status is " + status);
        }
        appLog.imp("terminated");
        setStatus(OverallStatus.TERMINATED);
        NodeInfo firstNode = getFirstNode();
        if (firstNode != null) {
            // check current node connection state
            if (firstNode.connection != null && firstNode.connection.isConnected()) {
                // initiate disconnect
                firstNode.connection.disconnect();
            }
            if (!firstNode.nodeUpdateStatus.terminal) {
                setNodeStatus(firstNode, NodeUpdateStatus.CANCELLED);
            }
        }
    }

    @Override
    public Map<Long, NodeUpdateStatus> getNodeStatuses() {
        Map<Long, NodeUpdateStatus> map = new HashMap<>();
        for (Map.Entry<Long, NodeInfo> longNodeInfoEntry : nodeInfoMap.entrySet()) {
            map.put(longNodeInfoEntry.getKey(), longNodeInfoEntry.getValue().nodeUpdateStatus);
        }
        return map;
    }

    ///////////////////////////////////////////////////////////////////////////
    // internal routines
    ///////////////////////////////////////////////////////////////////////////


    private void startNextNodeUpdate() {
        if (status == OverallStatus.TERMINATED) {
            log.d("quitting from startNextNodeUpdate, we are already terminated");
            return;
        }
        if (nodeInfoQueue.isEmpty()) {
            setStatus(OverallStatus.FINISHED);
            return;
        } // else: next node update
        NodeInfo nodeInfo = getFirstNode();
        if (nodeInfo.nodeUpdateStatus == NodeUpdateStatus.PENDING) {
            setNodeStatus(nodeInfo, NodeUpdateStatus.INITIATING);
        }
        // check which firmware needs to be updated
        nodeInfo.connectAttemptCounter++;
        appLog.d("trying FW UPDATE of " + nodeInfo.bleAddress + ", connect attempt number "
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
                    // adjust attempt counters
                    if (!connected[0]) {
                        nodeInfo.lastConnectFailAtCounter = nodeInfo.connectAttemptCounter;
                    }
                    // onDisconnected
                    if (nodeInfo.nodeUpdateStatus != NodeUpdateStatus.CANCELLED) {
                        int delay;
                        if (nodeInfo.nodeUpdateStatus.terminal) {
                            // we are done, try the next in the row
                            nodeInfoQueue.remove();
                            delay = 1;
                        } else {
                            // else: we will try again with the same node
                            delay = nodeInfo.getNextConnectDelayAndDestroy(NEXT_CONNECT_ATTEMPT_ON_CONNECTION_FAIL);
                        }
                        uiHandler.postDelayed(this::startNextNodeUpdate, delay);
                    }
            }
        );
    }

    private void onConnectedToNode(NodeInfo nodeInfo, NetworkNodeConnection nnc) {
        if (Constants.DEBUG) {
            log.d("onConnectedToNode: " + "nodeInfo = [" + nodeInfo + "]");
        }
        // set up the connection first
        nodeInfo.connection = nnc;
        // determine what should we do next
        log.d("node info: " + nodeInfo);
        switch (nodeInfo.nodeUpdateStatus) {
            case INITIATING:
                onConnectedToInitiatingNode(nodeInfo);
                break;
            case UPLOADING_FW1:
            case UPLOADING_FW2:
                // terminated/broken upload
                startFirmwareUpload(nodeInfo);
                break;
            case RESTORING_INITIAL_STATE:
                onConnectedRestoringInitialState(nodeInfo);
                break;
            case CANCELLED:
                // skip this one, we are not interested anymore
                nnc.disconnect();
                break;
            case SKIPPED_UP_TO_DATE:
            case SUCCESS:
            case FAIL:
            case PENDING:
            default:
                throw new IllegalStateException("unexpected node update status: " + nodeInfo.nodeUpdateStatus);
        }
    }

    private void genericOnFail(NodeInfo nodeInfo, String activityDescription) {
        if (nodeInfo.nodeUpdateStatus != NodeUpdateStatus.CANCELLED) {
            if (nodeInfo.connectAttemptCounter == nodeInfo.connectAttemptLimit) {
                log.d(activityDescription + ", connect attempt counter reached max level, giving up");
                setNodeStatus(nodeInfo, NodeUpdateStatus.FAIL);
            } else {
                log.d(activityDescription + ", trying again later");
            }
        }
    }

    private void onConnectedToInitiatingNode(NodeInfo nodeInfo) {
        if (nodeInfo.initialUwbMode == null) {
            checkNodeUwbMode(nodeInfo, true);
        } else if (nodeInfo.offlineSwitchDone == null) {
            // we need to determine state of the node and perform the switch if necessary
            checkNodeUwbMode(nodeInfo, false);
        } else {
            onConnectedToInitiatingOfflineNode(nodeInfo);
        }
    }

    private void checkNodeUwbMode(NodeInfo nodeInfo, boolean setInitialNodeInfo) {
        if (Constants.DEBUG) {
            log.d("checkNodeUwbMode: " + "nodeInfo = [" + nodeInfo + "], setInitialNodeInfo = [" + setInitialNodeInfo + "]");
        }
        // we need to determine if the node is offline/online
        nodeInfo.connection.getOtherSideEntity(networkNode -> {
            if (nodeInfo.nodeUpdateStatus == NodeUpdateStatus.CANCELLED) {
                // skip this one
                nodeInfo.connection.disconnect();
                return;
            }
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(networkNode.getUwbMode());
            }
            if (setInitialNodeInfo) {
                nodeInfo.initialUwbMode = networkNode.getUwbMode();
                nodeInfo.initialOperatingFirmware = networkNode.getOperatingFirmware();
                nodeInfo.initialLocationEngineFlag = networkNode.isTag() ? ((TagNode) networkNode).isLocationEngineEnable() : null;
            }
            if (networkNode.getUwbMode() != UwbMode.OFF) {
                // we need to switch it to OFF first
                networkNode.setUwbMode(UwbMode.OFF);
                nodeInfo.connection.updateOtherSideEntity(networkNode, false, writeEffect -> {
                    if (writeEffect == NetworkNodeConnection.WriteEffect.WRITE_DELAYED_EFFECT) {
                        // onsuccess, disconnect
                        nodeInfo.offlineSwitchDone = true;
                        nodeInfo.connection.disconnect();
                        nodeInfo.connectAttemptLimit++;
                        nodeInfo.nextConnectDelay = RECONNECT_ATTEMPT;
                    } else {
                        // we can continue immediately
                        onConnectedToInitiatingOfflineNode(nodeInfo);
                    }
                }, fail -> {
                    // onfail, we will get disconnected automatically, retry is performed automatically
                    genericOnFail(nodeInfo, "setting UWB to OFF state");
                });
            } else {
                // the node is already offline
                nodeInfo.offlineSwitchDone = false;
                onConnectedToInitiatingOfflineNode(nodeInfo);
            }
        }, fail -> {
            // we will get disconnected and retried automatically
            genericOnFail(nodeInfo, "retrieving node online/offline status");
        }, NetworkNodeProperty.UWB_MODE, NetworkNodeProperty.OPERATING_FIRMWARE, NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE);
    }

    private void onConnectedToInitiatingOfflineNode(NodeInfo nodeInfo) {
        if (Constants.DEBUG) {
            log.d("onConnectedToInitiatingOfflineNode: " + "nodeInfo = [" + nodeInfo + "]");
            Preconditions.checkNotNull(nodeInfo.initialUwbMode);
            Preconditions.checkNotNull(nodeInfo.initialOperatingFirmware);
        }
        if (nodeInfo.nodeUpdateStatus == NodeUpdateStatus.CANCELLED) {
            // skip this one
            nodeInfo.connection.disconnect();
            return;
        }
        startFirmwareUpload(nodeInfo);
    }

    private void startFirmwareUpload(NodeInfo nodeInfo) {
        if (Constants.DEBUG) {
            log.d("startFirmwareUpload: " + "nodeInfo = [" + nodeInfo + "]");
        }
        // retrieve information about the firmware
        nodeInfo.connection.getOtherSideEntity(networkNode -> {
                    if (nodeInfo.nodeUpdateStatus == NodeUpdateStatus.CANCELLED) {
                        // skip this one
                        nodeInfo.connection.disconnect();
                        return;
                    }
                    if (networkNode.getUwbMode() != UwbMode.OFF) {
                        appLog.we("failed precondition: node UWB should be OFF!", ErrorCode.FAILED_FIRMWARE_UPLOAD_PRECONDITION);
                        nodeInfo.nodeUpdateStatus = NodeUpdateStatus.FAIL;
                        nodeInfo.connection.disconnect();
                    } // else: we are offline and can start to determine which firmware needs to get updated
                    if (Constants.DEBUG) {
                        Preconditions.checkNotNull(networkNode.getFw1Checksum());
                        Preconditions.checkNotNull(networkNode.getFw2Checksum());
                        Preconditions.checkNotNull(networkNode.getFw1Version());
                        Preconditions.checkNotNull(networkNode.getFw2Version());
                    }
                    nodeInfo.fw1Checksum = networkNode.getFw1Checksum();
                    nodeInfo.fw2Checksum = networkNode.getFw2Checksum();
                    nodeInfo.fw1Version = networkNode.getFw1Version();
                    nodeInfo.fw2Version = networkNode.getFw2Version();
                    // preferentially, try to update the other firmware
                    OperatingFirmware of = getOtherFirmware(networkNode.getOperatingFirmware());
                    if (uploadFirmwareIfNecessary(nodeInfo, networkNode, of)) {
                        // we will continue asynchronously
                        return;
                    }
                    if (uploadFirmwareIfNecessary(nodeInfo, networkNode, getOtherFirmware(of))) {
                        // we will continue asynchronously
                        return;
                    }
                    // else: we are done
                    setNodeStatus(nodeInfo, NodeUpdateStatus.RESTORING_INITIAL_STATE);
                    onFirmwareUploaded(nodeInfo, networkNode);
                }, fail ->
                    genericOnFail(nodeInfo, "retrieving firmware information")
                , NetworkNodeProperty.UWB_MODE, NetworkNodeProperty.OPERATING_FIRMWARE,
                NetworkNodeProperty.FW1_VERSION, NetworkNodeProperty.FW2_VERSION, NetworkNodeProperty.FW1_CHECKSUM, NetworkNodeProperty.FW2_CHECKSUM);
    }

    private boolean uploadFirmwareIfNecessary(NodeInfo nodeInfo, NetworkNode networkNode, OperatingFirmware firmwareType) {
        if (Constants.DEBUG) {
            log.d("uploadFirmwareIfNecessary: " + "nodeInfo = [" + nodeInfo + "], networkNode = [" + networkNode + "], firmwareType = [" + firmwareType + "]");
        }
        // no need to update
        if (firmwareType == OperatingFirmware.FW1 && nodeInfo.fw1Updated
                || firmwareType == OperatingFirmware.FW2 && nodeInfo.fw2Updated) {
            return false;
        }
        // determine which and if firmware needs to be updated
        Firmware firmware;
        int fwVersion, fwChecksum;
        OperatingFirmware firmwareToUpdate;
        if (firmwareType == OperatingFirmware.FW1) {
            // try to upload FW1
            firmwareToUpdate = OperatingFirmware.FW1;
            firmware = firmware1;
            fwVersion = nodeInfo.fw1Version;
            fwChecksum = nodeInfo.fw1Checksum;
        } else {
            // try to upload FW2
            firmwareToUpdate = OperatingFirmware.FW2;
            firmware = firmware2;
            fwVersion = nodeInfo.fw2Version;
            fwChecksum = nodeInfo.fw2Checksum;
        }
        if (needsNewFirmware(firmware.getMeta(), fwVersion, fwChecksum)) {
            if (Constants.DEBUG) {
                log.d("will upload " + firmwareToUpdate);
            }
            // check if we need to switch operating firmware
            if (networkNode.getOperatingFirmware() == firmwareToUpdate) {
                if (Constants.DEBUG) {
                    log.d("restarting the module to the other firmware first");
                }
                // we need to restart the module to the other firmware first
                OperatingFirmware otherFirmware = getOtherFirmware(firmwareToUpdate);
                networkNode.setOperatingFirmware(otherFirmware);
                nodeInfo.connection.updateOtherSideEntity(networkNode, false, writeEffect -> {
                    if (nodeInfo.nodeUpdateStatus == NodeUpdateStatus.CANCELLED) {
                        // skip this one
                        nodeInfo.connection.disconnect();
                        return;
                    }
                    // increase the connect limit
                    nodeInfo.connectAttemptLimit++;
                    nodeInfo.nextConnectDelay = RECONNECT_ATTEMPT;
                    // we need to reconnect (in order for the operating firmware change to take effect)
                    nodeInfo.connection.disconnect();
                }, fail ->
                    genericOnFail(nodeInfo, "switching node to " + otherFirmware)
                );
            } else {
                // do the upload (we are updating non-operating firmware)
                doUploadFirmware(nodeInfo, firmware,
                        firmwareToUpdate == OperatingFirmware.FW1 ? NodeUpdateStatus.UPLOADING_FW1 : NodeUpdateStatus.UPLOADING_FW2,
                        () -> {
                            if (firmwareType == OperatingFirmware.FW1) {
                                nodeInfo.fw1Updated = true;
                            } else {
                                nodeInfo.fw2Updated = true;
                            }
                            // check if we need to upload the other firmware
                            if (!uploadFirmwareIfNecessary(nodeInfo, networkNode, getOtherFirmware(firmwareType))) {
                                // no need to upload operating firmware
                                setNodeStatus(nodeInfo, NodeUpdateStatus.RESTORING_INITIAL_STATE);
                                onFirmwareUploaded(nodeInfo, networkNode);
                            } // else: we will continue asynchronously
                        });
            }
            return true;
        } // else:
        return false;
    }

    private static OperatingFirmware getOtherFirmware(OperatingFirmware operatingFirmware) {
        switch (operatingFirmware) {
            case FW1:
                return OperatingFirmware.FW2;
            case FW2:
                return OperatingFirmware.FW1;
            default:
                throw new IllegalArgumentException("OF: " + operatingFirmware);
        }
    }


    private void doUploadFirmware(NodeInfo nodeInfo,
                                           Firmware firmware,
                                           NodeUpdateStatus uploadStatus,
                                           Action0 onSuccess) {
        NetworkNodeConnection nnc = nodeInfo.connection;
        // reset the byte counter
        nodeInfo.uploadByteCounter = 0;
        // start the firmware upload
        nnc.uploadFirmware(firmware.getMeta(), firmware.getByteStream(), () -> {
            // firmware update succeeded
            if (nodeInfo.nodeUpdateStatus != NodeUpdateStatus.CANCELLED) {
                //
                onSuccess.call();
            } else {
                // we are not interested anymore
                nnc.disconnect();
            }
        }, (bytes) -> {
            if (nodeInfo.nodeUpdateStatus != NodeUpdateStatus.CANCELLED) {
                // progress listener
                setNodeStatus(nodeInfo, uploadStatus);
                nodeInfo.uploadByteCounter = bytes;
                InterfaceHub.getHandlerHub(IhFwUpdateRunnerListener.class).onNodeUploadProgressChanged(nodeInfo.id, bytes);
            } else {
                nnc.disconnect();
            }
        }, (fail) -> {
            genericOnFail(nodeInfo, "firmware upload");
            // firmware upload failed we will not try again later
            nodeInfo.errorCode = fail.errorCode;
            setNodeStatus(nodeInfo, NodeUpdateStatus.FAIL);
            // we get disconnected in each case (automatically)
        });
    }

    private void onConnectedRestoringInitialState(NodeInfo nodeInfo) {
        nodeInfo.connection.getOtherSideEntity(networkNode -> {
            if (nodeInfo.nodeUpdateStatus != NodeUpdateStatus.CANCELLED) {
                onFirmwareUploaded(nodeInfo, networkNode);
            } else {
                nodeInfo.connection.disconnect();
            }
        }, fail ->
            genericOnFail(nodeInfo, "retrieving node state after FW upload")
        );
    }

    private void onFirmwareUploaded(NodeInfo nodeInfo, NetworkNode networkNode) {
        if (Constants.DEBUG) {
            log.d("onFirmwareUploaded: " + "nodeInfo = [" + nodeInfo + "], networkNode = [" + networkNode + "]");
        }
        boolean doUpdate = false;
        // check if we need to switch FW/restore online state
        if (nodeInfo.initialUwbMode != networkNode.getUwbMode()) {
            // we need to switch the online/offline flag
            networkNode.setUwbMode(nodeInfo.initialUwbMode);
            doUpdate = true;
        }
        if (nodeInfo.initialOperatingFirmware != networkNode.getOperatingFirmware()) {
            // we also need to switch operating firmware
            networkNode.setOperatingFirmware(nodeInfo.initialOperatingFirmware);
            doUpdate = true;
        }
        // workaround for firmware issue #69
        if (nodeInfo.initialLocationEngineFlag != null
                && !Objects.equal(((TagNode) networkNode).isLocationEngineEnable(), nodeInfo.initialLocationEngineFlag)) {
            ((TagNode) networkNode).setLocationEngineEnable(nodeInfo.initialLocationEngineFlag);
            doUpdate = true;
        }
        if (doUpdate) {
            appLog.d("putting the node back to initial state");
            // do the update now
            nodeInfo.connection.updateOtherSideEntity(networkNode, false, writeEffect -> {
                // onSuccess, disconnect and reconnect again
                nodeInfo.nextConnectDelay = RECONNECT_ATTEMPT;
                nodeInfo.connectAttemptLimit++;
                nodeInfo.connection.disconnect();
            }, fail -> {
                // onFail
                genericOnFail(nodeInfo, "putting the node back to initial state");
            });
        } else {
            appLog.d("firmware update is DONE, disconnecting");
            // we are done
            setNodeStatus(nodeInfo, nodeInfo.fw1Updated || nodeInfo.fw2Updated ? NodeUpdateStatus.SUCCESS : NodeUpdateStatus.SKIPPED_UP_TO_DATE);
            // the next node is tried on the onDisconnected callback
            nodeInfo.connection.disconnect();
            //
            schedulePingNodeTask(nodeInfo);
        }
    }

    private void schedulePingNodeTask(NodeInfo nodeInfo) {
        // also initiate a reconnect - so that firmware1 information gets updated in our persisted node representation
        uiHandler.postDelayed(() -> {
            log.d("making sure that there we get persisted the most recent representation of network node: connecting and retrieving node properties [" + nodeInfo.bleAddress + "]");
            bleConnectionApi.connect(nodeInfo.bleAddress, ConnectPriority.HIGH, (conn) -> conn.getOtherSideEntity((response) -> {
                        // the network node gets updated automatically
                        // disconnect now
                        conn.disconnect();
                    }, Constants.VOID_ON_FAIL),
                    Constants.VOID_ON_CONNECTION_FAIL,
                    Constants.VOID_ON_DISCONNECT);
        }, RECONNECT_ATTEMPT);
    }

    private void setNodeStatus(NodeInfo nodeInfo, NodeUpdateStatus newStatus) {
        if (nodeInfo.nodeUpdateStatus != newStatus) {
            nodeInfo.nodeUpdateStatus = newStatus;
            InterfaceHub.getHandlerHub(IhFwUpdateRunnerListener.class).onNodeStatusChanged(nodeInfo.id);
        }
    }

    private void setStatus(OverallStatus newStatus) {
        if (Constants.DEBUG) {
            log.d("setStatus: " + "newStatus = [" + newStatus + "]");
        }
        if (status != newStatus) {
            status = newStatus;
            InterfaceHub.getHandlerHub(IhFwUpdateRunnerListener.class).onFwUpdateStatusChanged(status);
        }
    }

    private NodeInfo getFirstNode() {
        return nodeInfoQueue.peek();
    }


    public static boolean needsNewFirmware(FirmwareMeta newFirmwareMeta, Integer fwVersion,
                                           Integer fwChecksum) {
        return fwVersion != newFirmwareMeta.firmwareVersion || fwChecksum != newFirmwareMeta.firmwareChecksum;
    }


}
