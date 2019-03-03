/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiFunction;
import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnection;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.AutoPositioningState;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.components.struct.NodeDistanceMatrix;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.runner.IhAutoPositioningManagerListener;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action1;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 */
public class AutoPositioningManagerImpl implements AutoPositioningManager {
    // fixed references
    private static final ComponentLog log = new ComponentLog(AutoPositioningManager.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "AUTO-POS");
    // constants
    private static final int DISTANCE_MEASURE_TIMEOUT = 90000;  // 90s
    private static final int INITIATOR_CHECK_ATTEMPT_COUNT = 3;
    private static final int MEASURE_DISTANCES_ATTEMPT_COUNT = 3;
    private static final int SAVE_POSITION_ATTEMPT_COUNT = 2;
    // dependencies
    private final BleConnectionApi bleConnectionApi;
    private final NetworkNodeManager networkNodeManager;
    //
    private AutoPositioningState state;
    private AutoPositioningAlgorithm.ResultCode resultCode;
    //
    private Map<Long,AnchorNode> nodes;
    private NodeDistanceMatrix distanceMatrix;
    private AutoPositioningState.OverallState lastOverallState;
    //
    private Map<Long,ComputedPosition> computedPositions;
    //
    private Map<String,NetworkNodeBleConnection> managedConnections;
    private int zAxis;

    // task type
    private enum TaskType {
        INITIATOR_CHECK, DISTANCE, COMPUTATION, POSITION
    }
    private TaskType lastRunningTaskType;
    //
    private Object tag;

    @Inject
    AutoPositioningManagerImpl(BleConnectionApi bleConnectionApi, NetworkNodeManager networkNodeManager) {
        // dependencies
        this.bleConnectionApi = bleConnectionApi;
        this.networkNodeManager = networkNodeManager;
        this.managedConnections = new HashMap<>();
        this.computedPositions = new HashMap<>();
        this.zAxis = 0;
        // state
        this.state = new AutoPositioningStateImpl(new AutoPositioningStateImpl.StateListener() {

            @Override
            public void onOverallStateChanged(AutoPositioningState.OverallState overallState) {
                // check if we can compute the positions now
                AutoPositioningAlgorithm.ResultCode r = null;
                if (lastOverallState == AutoPositioningState.OverallState.COLLECTING_DISTANCES && overallState == AutoPositioningState.OverallState.IDLE) {
                    if (state.getDistanceCollectionState() == AutoPositioningState.TaskState.SUCCESS) {
                        // compute positions only if the distance collection succeeded - notification is done few lines below
                        r = computePositions();
                    }
                }
                // save the last state
                lastOverallState = overallState;
                // raise a transformed event
                InterfaceHub.getHandlerHub(IhAutoPositioningManagerListener.class).onApplicationStateChanged(getApplicationState(), r);
            }

            @Override
            public void onNodeStatusChanged(long nodeId) {
                InterfaceHub.getHandlerHub(IhAutoPositioningManagerListener.class).onNodeStateChanged(nodeId);
            }

            @Override
            public void onReset() {
                InterfaceHub.getHandlerHub(IhAutoPositioningManagerListener.class).onNodeSetChange(null);
            }

        });
        this.distanceMatrix = new NodeDistanceMatrix();
    }

    private AutoPositioningAlgorithm.ResultCode computePositions() {
        if (Constants.DEBUG) {
            log.d("computePositions on distance matrix: " + distanceMatrix);
        }
        lastRunningTaskType = TaskType.COMPUTATION;
        AutoPositioningAlgorithm.Result result = AutoPositioningAlgorithm.computePositions(
                Stream.of(nodes.keySet()).collect(Util.toArrayList(nodes.size())),
                distanceMatrix,
                zAxis);
        //
        log.i("compute positions result: " + result);
        resultCode = result.code;
        computedPositions = result.positions;
        return result.code;
    }

    @Override
    public AutoPositioningState.NodeState getNodeRunningState(long nodeId) {
        return state.getRunningNodeState(nodeId);
    }

    @Override
    public AutoPositioningState.TaskState getNodeInitiatorCheckStatus(long nodeId) {
        return state.getInitiatorCheckState(nodeId);
    }

    @Override
    public AutoPositioningState.TaskState getNodeDistanceCollectionStatus(long nodeId) {
        return state.getNodeDistanceCollectionState(nodeId);
    }

    @Override
    public AutoPositioningState.TaskState getNodePositionSaveStatus(long nodeId) {
        return state.getNodePositionSaveState(nodeId);
    }

    @Override
    public void resetNodeSet(List<AnchorNode> nodes) {
        if (Constants.DEBUG) {
            log.d("resetNodeSet: " + "nodes = [" + nodes + "]");
        }
        if (Constants.DEBUG) {
            Preconditions.checkState(state.getOverallState() == AutoPositioningState.OverallState.IDLE,
                    "node configuration can only be done in IDLE state, state = " + state.getOverallState());
        }
        // create a copy of the list
        this.nodes = new LinkedHashMap<>();
        for (AnchorNode node : nodes) {
            if (Constants.DEBUG) {
                Preconditions.checkState(node.isAnchor(), "node " + node + " is not an anchor!");
            }
            // add a copy of the node with just position set
            NodeFactory.AnchorNodeBuilder builder = NodeFactory.newAnchorBuilder(node.getId());
            builder.setInitiator(node.isInitiator())
                    .setMacStats(node.getMacStats())
                    // we need initiator and MAC stats to know who is the real initiator
                    .setLabel(node.getLabel())
                    .setBleAddress(node.getBleAddress())
                    .setPosition(node.getPosition());
            this.nodes.put(node.getId(), builder.build());
        }
        // reset the last running task type
        this.lastRunningTaskType = null;
        // this also raises an IH event
        this.state.reset(Stream.of(nodes).map(AnchorNode::getId).collect(Collectors.toSet()));
        // clear also the distance matrix
        this.distanceMatrix.clear();
        // clear the computed positions
        if (computedPositions != null) this.computedPositions.clear();
        // clear the z-axis value
        zAxis = 0;
    }

    @Override
    public void reorder(List<Long> nodeIds) {
        if (Constants.DEBUG) {
            log.d("reorder: " + "nodeIds = [" + nodeIds + "]");
        }
        AutoPositioningState.ApplicationState appState = getApplicationState();
        if (Constants.DEBUG) {
            Preconditions.checkState(appState.idle, "application state is not IDLE: " + appState);
        }
        Map<Long, AnchorNode> oldNodes = this.nodes;
        //
        this.nodes = new LinkedHashMap<>();
        for (Long nodeId : nodeIds) {
            AnchorNode on = oldNodes.get(nodeId);
            Preconditions.checkNotNull(on, "node " + nodeId + " cannot be reordered, it's not present");
            nodes.put(nodeId, on);
        }
        // keep the distance matrix & compute the positions on reordered node set now
        AutoPositioningAlgorithm.ResultCode code = null;
        if (state.getDistanceCollectionState() == AutoPositioningState.TaskState.SUCCESS) {
            // we can safely recompute positions
            code = computePositions();
        }
        // just raise an event about changed node set
        InterfaceHub.getHandlerHub(IhAutoPositioningManagerListener.class).onNodeSetChange(code);
    }

    @Override
    public boolean measure() {
        if (Constants.DEBUG) {
            log.d("measure()");
            Preconditions.checkState(state.getOverallState() == AutoPositioningState.OverallState.IDLE, "can collect distances only in IDLE state! " + state.getOverallState());
        }
        // retrieve declared initiators
        if (nodes.isEmpty()) {
            return false;
        }
        // reset the last running task type
        this.lastRunningTaskType = null;
        // reset state of all nodes
        this.state.resetStates();
        // do real initiator check
        AnchorNode[] verifiedInitiator = { null };
        boolean foundInitiator = false;
        for (AnchorNode node : nodes.values()) {
            if (node.isInitiator()) {
                if (!foundInitiator) {
                    foundInitiator = true;
                    lastRunningTaskType = TaskType.INITIATOR_CHECK;
                    this.tag = new Object();
                }
                initiateInitiatorCheck(verifiedInitiator, node, 1, this.tag);
            }
        }
        return foundInitiator;
    }

    private void initiateInitiatorCheck(AnchorNode[] verifiedInitiator,
                                        AnchorNode node,
                                        int attemptCounter,
                                        Object launchTag) {
        Boolean[] failed = {null};
        // connect & retrieve MAC status
        String bleAddress = node.getBleAddress();
        managedConnections.put(node.getBleAddress(),
                bleConnectionApi.connect(node.getBleAddress(), ConnectPriority.MEDIUM, connection -> {
                    if (this.tag != launchTag) {
                        // obsolete
                        connection.disconnect();
                    } else if (verifiedInitiator[0] != null) {
                        failed[0] = false;
                        // we already have an initiator
                        connection.disconnect();
                    } else {
                        // retrieve MAC stats
                        connection.getOtherSideEntity(fetchedNode -> {
                            // onSuccess
                            // update our internal representation
                            node.copyFrom(fetchedNode);
                            if (tag == launchTag && verifiedInitiator[0] == null && Util.isRealInitiator(fetchedNode)) {
                                // we will use our representation (not the fetched one, it might be not rich enough)
                                verifiedInitiator[0] = node;
                            }
                            //
                            failed[0] = false;
                            // disconnect now
                            connection.disconnect();
                        }, fail -> {
                            // onFail - we will get disconnected automatically
                        }, NetworkNodeProperty.ANCHOR_MAC_STATS);
                    }
                }, (connection, fail) -> {
                    // onFail
                    if (failed[0] == null) {
                        failed[0] = true;
                    }
                }, (connection, errCode) -> {
                    // onDisconnected
                    managedConnections.remove(bleAddress);
                    if (launchTag == this.tag) {
                        if (failed[0]) {
                            if (verifiedInitiator[0] == null && attemptCounter < INITIATOR_CHECK_ATTEMPT_COUNT) {
                                // try again
                                initiateInitiatorCheck(verifiedInitiator, node, attemptCounter + 1, launchTag);
                            } else {
                                state.setInitiatorCheckState(node.getId(), AutoPositioningState.TaskState.FAILED);
                            }
                        } else {
                            state.setInitiatorCheckState(node.getId(), AutoPositioningState.TaskState.SUCCESS);
                        }
                        if (verifiedInitiator[0] != null && state.getInitiatorCheckState() == AutoPositioningState.TaskState.SUCCESS) {
                            initiateDistanceCollection(verifiedInitiator);
                        }
                    }
                }));
        state.setInitiatorCheckState(node.getId(), AutoPositioningState.TaskState.RUNNING);
    }

    private void initiateDistanceCollection(AnchorNode[] verifiedInitiator) {
        // we will launch the next phase - distance collection
        lastRunningTaskType = TaskType.DISTANCE;
        // clear the matrix first
        distanceMatrix.clear();
        this.tag = new Object();
        putNetworkToApModeCollectDistances(verifiedInitiator[0], 1, tag);
    }

    private void putNetworkToApModeCollectDistances(AnchorNode initiatorNode, int attemptCounter, Object launchTag) {
        if (this.tag != launchTag) {
            // ignore
            return;
        }
        String bleAddress = initiatorNode.getBleAddress();
        LogEntryTag deviceTag = LogEntryTagFactory.getDeviceLogEntryTag(initiatorNode.getBleAddress());
        state.setNodeDistanceCollectionState(initiatorNode.getId(), AutoPositioningState.TaskState.RUNNING);
        Boolean[] failed = {null};
        // start a new connection
        managedConnections.put(bleAddress, bleConnectionApi.connect(bleAddress,
                ConnectPriority.MEDIUM,
                // onSuccess (connected)
                (nnc) -> {
                    if (tag != launchTag) {
                        // obsolete
                        nnc.disconnect();
                        return;
                    }
                    nnc.getOtherSideEntity((networkNode) -> {
                            // onsuccess
                            if (tag == launchTag) {
                                Runnable measureDistanceTimeoutAction = () -> {
                                    if (nnc.isConnected()) {
                                        if (failed[0] == null) {
                                            failed[0] = true;
                                            appLog.we("failed to put the network to AP mode, operation timed out", ErrorCode.AP_DISTANCE_MEASURE_TIMEOUT, deviceTag);
                                        }
                                        nnc.disconnect();
                                    }
                                };
                                // initiate distance measurement = subscribe on location data
                                NetworkNodeConnection.LocationDataChangedCallback distanceCollector = new NetworkNodeConnection.LocationDataChangedCallback() {
                                    @Override
                                    public void onStarted() {
                                        // schedule disconnect
                                        uiHandler.postDelayed(measureDistanceTimeoutAction, DISTANCE_MEASURE_TIMEOUT);
                                    }

                                    @Override
                                    public void onChange(LocationData locationData) {
                                        uiHandler.removeCallbacks(measureDistanceTimeoutAction);
                                        if (locationData == null || locationData.distances == null) {
                                            appLog.we("failed to retrieve distances from initiator, null distances received?", ErrorCode.GATT_INCONSISTENT_LOCATION_DATA, deviceTag);
                                            failed[0] = true;
                                        } else {
                                            appLog.i("received measurement result from initiator: " + locationData.distances, deviceTag);
                                            // this is THE distance
                                            putDistancesToMatrix(locationData.distances, initiatorNode.getId());
                                            // start distance collection on other nodes
                                            retrieveDistancesFromNonInitiatorNodes();
                                            //
                                            failed[0] = false;
                                        }
                                        // disconnect in each case
                                        nnc.disconnect();
                                    }

                                    @Override
                                    public void onFail(Fail fail) {
                                        uiHandler.removeCallbacks(measureDistanceTimeoutAction);
                                        if (failed[0] == null) {
                                            appLog.we("failed to retrieve distances: " + fail.message, fail.errorCode, deviceTag);
                                            failed[0] = true;
                                        }
                                    }

                                    @Override
                                    public void onStopped() {
                                        // nothing
                                    }
                                };
                                // check that there is proper mode set
                                if (networkNode.getLocationDataMode() == LocationDataMode.POSITION) {
                                    if (Constants.DEBUG) {
                                        log.d("need to change location data mode first");
                                    }
                                    // we need to change the location data mode first
                                    networkNode.setLocationDataMode(LocationDataMode.POSITION_AND_DISTANCES);
                                    nnc.updateOtherSideEntity(networkNode, false, (nn) -> {
                                        // onSuccess
                                        observeDistances(nnc, distanceCollector);
                                    }, (fail) -> {
                                        // onFail
                                        appLog.we("failed to set location data mode of initiator: " + fail.message, fail.errorCode, deviceTag);
                                        failed[0] = true;
                                    });
                                } else {
                                    // there is already set proper location data mode
                                    observeDistances(nnc, distanceCollector);
                                }
                            }
                        },
                        (fail) -> {
                            // onFail
                            // we will get disconnected automatically
                            if (failed[0] == null) {
                                appLog.we("failed to retrieve distances from initiator: " + fail.message, fail.errorCode, deviceTag);
                                failed[0] = true;
                            }
                        },
                        NetworkNodeProperty.LOCATION_DATA_MODE
                    );
            },
            (connection,fail) -> {
                    // on fail (connect)
                    if (failed[0] == null) {
                        appLog.i("failed to put the network to auto-position mode: " + fail.message, deviceTag);
                        failed[0] = true;
                    }
            }, (nnc,err) -> {
                    // on disconnected
                    managedConnections.remove(bleAddress);
                    if (tag == launchTag) {
                        if (failed[0]) {
                            if (attemptCounter < MEASURE_DISTANCES_ATTEMPT_COUNT) {
                                // try again few ms later
                                putNetworkToApModeCollectDistances(initiatorNode, attemptCounter + 1, launchTag);
                                // exit now
                                return;
                            } // else:
                            appLog.i("collect distances from initiator: " + attemptCounter + " attempts exhausted", deviceTag);
                            state.setNodeDistanceCollectionState(initiatorNode.getId(), AutoPositioningState.TaskState.FAILED);
                        } else {
                            // success
                            appLog.i("successfully measured distances through initiator (AP network mode)", deviceTag);
                            state.setNodeDistanceCollectionState(initiatorNode.getId(), AutoPositioningState.TaskState.SUCCESS);
                        }
                    }
            }
        ));
    }

    @Override
    public void retrieveDistancesFromFailingNodesAndComputePositions() {
        tag = new Object();
        retrieveDistancesFromNodes((n) -> state.getNodeDistanceCollectionState(n.getId()) == AutoPositioningState.TaskState.FAILED, false);
    }

    private void retrieveDistancesFromNonInitiatorNodes() {
        retrieveDistancesFromNodes((n) -> !Util.isRealInitiator(n), true);
    }

    private void retrieveDistancesFromNodes(Predicate<AnchorNode> filter, boolean checkCollectingDistances) {
        if (Constants.DEBUG) {
            log.d("retrieveDistancesFromNodes: " + "filter = [" + filter + "]");
        }
        if (Constants.DEBUG) {
            if (checkCollectingDistances) Preconditions.checkState(state.getOverallState() == AutoPositioningState.OverallState.COLLECTING_DISTANCES,
                    "can retrieve distances from other nodes only in COLLECTING_DISTANCES state: " + state.getOverallState());
            Preconditions.checkNotNull(this.tag);
        }
        for (AnchorNode node : nodes.values()) {
            if (filter.test(node)) {
                retrieveDistancesFromNode(node, 1, tag);
            }
        }
    }

    private void retrieveDistancesFromNode(AnchorNode node,
                                           int attemptCounter,
                                           Object launchTag) {
        if (this.tag != launchTag) {
            // ignore
            return;
        }
        String bleAddress = node.getBleAddress();
        LogEntryTag deviceTag = LogEntryTagFactory.getDeviceLogEntryTag(node.getBleAddress());
        Long nodeId = node.getId();
        state.setNodeDistanceCollectionState(nodeId, AutoPositioningState.TaskState.RUNNING);
        Boolean[] failed = {null};
        // we can safely start a new connection
        managedConnections.put(bleAddress, bleConnectionApi.connect(bleAddress,
                ConnectPriority.MEDIUM,
                // onSuccess (connected)
                (nnc) -> {
                    if (tag != launchTag) {
                        // obsolete
                        nnc.disconnect();
                        return;
                    }
                    nnc.getOtherSideEntity((networkNode) -> {
                                // onsuccess
                                if (tag == launchTag) {
                                    // retrieve location data
                                    // check that there is proper mode set first
                                    Action1<NetworkNode> successCallback = nnode -> {
                                        // on success
                                        List<RangingAnchor> distances = ((AnchorNode) nnode).getDistances();
                                        if (distances != null) {
                                            appLog.d("successfully retrieved distances: " + distances, deviceTag);
                                            putDistancesToMatrix(distances, nodeId);
                                            failed[0] = false;
                                        } else {
                                            failed[0] = true;
                                            appLog.we("missing measured distances?!", ErrorCode.AP_MEASURED_DISTANCE_MISSING, deviceTag);
                                        }
                                        // initiate a disconnect
                                        nnc.disconnect();
                                    };
                                    if (networkNode.getLocationDataMode() != LocationDataMode.POSITION_AND_DISTANCES) {
                                        if (Constants.DEBUG) {
                                            log.d("need to change location data mode first");
                                        }
                                        // we need to change the location data mode first
                                        networkNode.setLocationDataMode(LocationDataMode.POSITION_AND_DISTANCES);
                                        // now update the entity
                                        nnc.updateOtherSideEntity(networkNode, false,
                                                (nn) -> {
                                                    // onSuccess
                                                    // and read the distances again
                                                    retrieveDistances(nnc, successCallback, fail -> {
                                                        // onFail
                                                        if (failed[0] == null) {
                                                            appLog.we("distance retrieval failed", fail, deviceTag);
                                                            failed[0] = true;
                                                        }
                                                        // we will get disconnected automatically
                                                    });
                                                },
                                                (fail) -> {
                                                    // onFail
                                                    if (failed[0] == null) {
                                                        appLog.we("failed to set location data mode: " + fail.message, fail.errorCode, deviceTag);
                                                        failed[0] = true;
                                                    }
                                                });
                                    } else {
                                        // there is already set proper location data mode, we should have also received proper distances
                                        // call success callback immediately
                                        successCallback.call(networkNode);
                                    }
                                }
                            },
                            (fail) -> {
                                // onFail
                                if (failed[0] == null) {
                                    // we will get disconnected automatically
                                    appLog.we("failed to retrieve distances", fail, deviceTag);
                                    failed[0] = true;
                                }
                            },
                            NetworkNodeProperty.LOCATION_DATA_MODE, NetworkNodeProperty.ANCHOR_DISTANCES
                    );
                },
                // on fail (connect)
                (connection,fail) -> {
                    if (failed[0] == null) {
                        appLog.i("failed to retrieve distances: " + fail.message, deviceTag);
                        failed[0] = true;
                    }
                },
                // on disconnected
                (nnc,err) -> {
                    managedConnections.remove(bleAddress);
                    if (tag == launchTag) {
                        if (failed[0]) {
                            if (attemptCounter < MEASURE_DISTANCES_ATTEMPT_COUNT) {
                                // try again
                                retrieveDistancesFromNode(node, attemptCounter + 1, launchTag);
                            } else {
                                appLog.i("collect distances: " + attemptCounter + " attempts exhausted", deviceTag);
                                state.setNodeDistanceCollectionState(nodeId, AutoPositioningState.TaskState.FAILED);
                            }
                        } else {
                            // success
                            appLog.i("successfully collected distances", deviceTag);
                            state.setNodeDistanceCollectionState(nodeId, AutoPositioningState.TaskState.SUCCESS);
                        }
                    }
                }
        ));
    }

    private void observeDistances(NetworkNodeConnection nnc, NetworkNodeConnection.LocationDataChangedCallback distanceCollector) {
        if (Constants.DEBUG) {
            log.d("observing location data - initiating and waiting for measurement result");
        }
        nnc.observeLocationData(distanceCollector);
    }

    private void retrieveDistances(NetworkNodeConnection nnc, Action1<NetworkNode> successCallback, Action1<Fail> failCallback) {
        if (Constants.DEBUG) {
            log.d("retrieving distances");
        }
        nnc.getOtherSideEntity(successCallback, failCallback, NetworkNodeProperty.ANCHOR_DISTANCES);
    }

    @NotNull
    @Override
    public AutoPositioningState.ApplicationState getApplicationState() {
        // compute the state
        AutoPositioningState.OverallState overallState = state.getOverallState();
        switch (overallState) {
            case CHECKING_INITIATOR:
                return AutoPositioningState.ApplicationState.CHECKING_INITIATOR;
            case COLLECTING_DISTANCES:
                return AutoPositioningState.ApplicationState.COLLECTING_DISTANCES;
            case SAVING_POSITION:
                return AutoPositioningState.ApplicationState.SAVING_POSITIONS;
            case IDLE:
                // what kind of idle this is?
                if (lastRunningTaskType == TaskType.INITIATOR_CHECK) {
                    AutoPositioningState.TaskState initiatorCheckState = state.getInitiatorCheckState();
                    switch (initiatorCheckState) {
                        case FAILED:
                            return AutoPositioningState.ApplicationState.INITIATOR_CHECK_FAILED;
                        case SUCCESS:
                            if (resultCode == AutoPositioningAlgorithm.ResultCode.SUCCESS) {
                                return AutoPositioningState.ApplicationState.CHECKING_INITIATOR;
                                // we will proceed to COLLECTING_DISTANCES soon
                            } else {
                                return AutoPositioningState.ApplicationState.INITIATOR_CHECK_FAILED;
                            }
                        case TERMINATED:
                            return AutoPositioningState.ApplicationState.INITIATOR_CHECK_TERMINATED;
                        case RUNNING:
                            throw new IllegalStateException("we should have overallState CHECKING_INITIATOR?");
                        case NOT_STARTED:
                        default:
                            throw new IllegalStateException("state: " + initiatorCheckState);
                    }
                } else if (lastRunningTaskType == TaskType.POSITION) {
                    AutoPositioningState.TaskState positionSaveState = state.getPositionSaveState();
                    switch (positionSaveState) {
                        case FAILED:
                            return AutoPositioningState.ApplicationState.POSITIONS_SAVE_FAILED;
                        case SUCCESS:
                            return AutoPositioningState.ApplicationState.POSITIONS_SAVE_SUCCESS;
                        case TERMINATED:
                            return AutoPositioningState.ApplicationState.POSITIONS_SAVE_TERMINATED;
                        case RUNNING:
                            throw new IllegalStateException("we should have overallState SAVING_POSITION?");
                        case NOT_STARTED:
                        default:
                            throw new IllegalStateException("state: " + positionSaveState);
                    }
                } else if (lastRunningTaskType == TaskType.DISTANCE) {
                    AutoPositioningState.TaskState distanceCollectionState = state.getDistanceCollectionState();
                    switch (distanceCollectionState) {
                        case FAILED:
                            return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_FAILED;
                        case SUCCESS:
                            if (resultCode == AutoPositioningAlgorithm.ResultCode.SUCCESS) {
                                return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS;
                            } else {
                                return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL;
                            }
                        case TERMINATED:
                            return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_TERMINATED;
                        case RUNNING:
                            throw new IllegalStateException("we should have overallState COLLECTING_DISTANCES?");
                        case NOT_STARTED:
                        default:
                            throw new IllegalStateException("state: " + distanceCollectionState);
                    }
                } else if (lastRunningTaskType == TaskType.COMPUTATION) {
                    if (resultCode == AutoPositioningAlgorithm.ResultCode.SUCCESS) {
                        return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS;
                    } else {
                        return AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL;
                    }
                } else if (lastRunningTaskType == null) {
                    // no last running task type was started
                    return AutoPositioningState.ApplicationState.NOT_STARTED;
                } else {
                    throw new IllegalStateException("lastRunningTaskType = " + lastRunningTaskType);
                }
            default:
                throw new IllegalStateException("overallState = " + overallState);
        }
    }

    @Override
    public AutoPositioningAlgorithm.ResultCode getPositionComputeResultCode() {
        if (Constants.DEBUG) {
            AutoPositioningState.ApplicationState as = getApplicationState();
            Preconditions.checkState(as == AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL
                    || as == AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS,
                    "application state must be " + as);
        }
        return resultCode;
    }


    private void putDistancesToMatrix(List<RangingAnchor> distances, long nodeId) {
        // save distances into the matrix
        for (RangingAnchor distance : distances) {
            distanceMatrix.putDistance(distance.nodeId, nodeId, distance.distance);
        }
    }

    @Override
    public void savePositions() {
        if (Constants.DEBUG) {
            Preconditions.checkState(state.getOverallState() == AutoPositioningState.OverallState.IDLE, "can save positions only in IDLE state! " + state.getOverallState());
        }
        lastRunningTaskType = TaskType.POSITION;
        //
        this.tag = new Object();
        // start position save simultaneously
        forEachUnsavedPosition((node,position) -> {
            savePosition(node, position, 1, tag);
            return true;
        });
    }

    private void forEachUnsavedPosition(BiFunction<AnchorNode,Position,Boolean> action) {
        for (AnchorNode node : nodes.values()) {
            // check if this particular node needs to save position (the position differs)
            ComputedPosition computedPosition = computedPositions.get(node.getId());
            if (computedPosition != null && computedPosition.success
                    && !Objects.equal(node.extractPositionDirect(), computedPosition.position)) {
                if (!action.apply(node, computedPosition.position)) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean anyNodeNeedsPositionSave() {
        if (Constants.DEBUG) {
            Preconditions.checkState(getApplicationState() == AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS,
                    "query is valid only in " + AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS + " state");
        }
        boolean[] anyUnsavedPosition = new boolean[] { false };
        forEachUnsavedPosition((node,position) -> {
            anyUnsavedPosition[0] = true;
            return false;
        });
        return anyUnsavedPosition[0];
    }

    private void savePosition(AnchorNode node,
                              Position computedPosition,
                              int attemptCounter,
                              Object launchTag) {
        if (this.tag != launchTag) {
            // ignore
            return;
        }
        String bleAddress = node.getBleAddress();
        LogEntryTag deviceTag = LogEntryTagFactory.getDeviceLogEntryTag(node.getBleAddress());
        Boolean[] failed = { null };
        // create a copy of the node and inject just position
        NodeFactory.AnchorNodeBuilder b = NodeFactory.newAnchorBuilder(node.getId());
        b.setPosition(computedPosition);
        // set the save position state to RUNNING
        state.setNodePositionSaveState(node.getId(), AutoPositioningState.TaskState.RUNNING);
        // start a new connection and save the position
        managedConnections.put(bleAddress, bleConnectionApi.connect(bleAddress,
                    ConnectPriority.MEDIUM,
                    // onConnected
                    (nnc) -> {
                        if (tag != launchTag) {
                            // obsolete
                            nnc.disconnect();
                            return;
                        }
                        nnc.updateOtherSideEntity(b.build(), false, (writeEffect) -> {
                                    // onSuccess
                                    // regardless launch tag, this went through!
                                    // save the position to our node copy
                                    node.setPosition(computedPosition);
                                    // let the model manager know that there is a change
                                    networkNodeManager.updateAnchorPosition(node.getId(), computedPosition);
                                    // disconnect now
                                    nnc.disconnect();
                                    failed[0] = false;
                                }, (fail) -> {
                                    // we will get disconnected automatically
                                    appLog.we("failed to save position", fail.errorCode, deviceTag);
                                    failed[0] = true;
                                }
                        );
                    },
                    // onFail (connect)
                    (connection,fail) -> {
                        if (failed[0] == null) {
                            appLog.i("failed to save position (connect problem): " + fail.message, deviceTag);
                            failed[0] = true;
                        } // else: ignore the fail
                    }, (nnc,err) -> {
                        // onDisconnected
                        managedConnections.remove(bleAddress);
                        if (launchTag == tag) {
                            if (failed[0]) {
                                if (attemptCounter < SAVE_POSITION_ATTEMPT_COUNT) {
                                    // try again
                                    savePosition(node, computedPosition, attemptCounter + 1, launchTag);
                                } else {
                                    appLog.i("save position " + attemptCounter + " attempts exhausted", deviceTag);
                                    state.setNodePositionSaveState(node.getId(), AutoPositioningState.TaskState.FAILED);
                                }
                            } else {
                                // success
                                appLog.i("successfully saved position", deviceTag);
                                state.setNodePositionSaveState(node.getId(), AutoPositioningState.TaskState.SUCCESS);
                            }
                        } // else: the launchTag is different (might be the 'terminate' case)
                    }));
    }

    @Override
    public List<AnchorNode> getNodes() {
        return nodes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(nodes.values()));
    }

    @Override
    public ComputedPosition getComputedPosition(long nodeId) {
            return computedPositions != null ? computedPositions.get(nodeId) : null;
    }

    @Override
    public void terminate() {
        if (Constants.DEBUG) {
            AutoPositioningState.ApplicationState applicationState = getApplicationState();
            Preconditions.checkState(!applicationState.idle, "applicationState must NOT be idle when terminating: " + applicationState);
        }
        // reset the tag (so that the callbacks do not report results anymore)
        this.tag = null;
        // now close open connections one-by-one
        for (NetworkNodeConnection connection : managedConnections.values()) {
            // they will get removed once we get 'onDisconnected' callback
            connection.disconnect();
        }
        // and let the state know
        this.state.terminateOngoingOperation();
    }

    @Override
    public boolean hasInProgressConnection(String nodeBle) {
        // this is the same as checking 'CONNECTING' state (when nnc is not ready yet)
        Long nodeId = networkNodeManager.bleToId(nodeBle);
        if (nodeId == null) {
            // strange but possible (we are connected to a node which is not known yet - whose properties has not been fetched)
            log.w("BLE address " + nodeBle + " cannot be mapped to node id");
            return false;
        }
        // this is fake - we pretend that we have a connection if there is an ongoing operation
        if (state.getRunningNodeState(nodeId) != AutoPositioningState.NodeState.IDLE) {
            return true;
        }
        // else: check existing connections
        NetworkNodeConnection connection = managedConnections.get(nodeBle);
        return connection != null && connection.getState().inProgress;
    }

    @Override
    public void setZaxis(int zAxis) {
        this.zAxis = zAxis;
        // propagate the zAxis value to all positions
        for (ComputedPosition computedPosition : computedPositions.values()) {
            computedPosition.position.z = zAxis;
        }
    }

    @Override
    public int getZaxis() {
        return this.zAxis;
    }

}
