/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argomanager.components.impl.AutoPositioningAlgorithm;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.runner.IhAutoPositioningManagerListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Auto position manager.
 *
 * How to use this:
 * - set up the node set with {@link #resetNodeSet(List)}
 * - collect distances and compute positions of these nodes {@link #measure()}
 * - if the distance collection fails, you may retry with {@link #retrieveDistancesFromFailingNodesAndComputePositions()} again
 * - save positions to nodes with {@link #savePositions()}
 *
 * Register as {@link IhAutoPositioningManagerListener} to get notified about auto-positioning events.
 */
public interface AutoPositioningManager {

    ///////////////////////////////////////////////////////////////////////////
    // application state
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @return auto-positioning application sate
     * @see IhAutoPositioningManagerListener#onApplicationStateChanged(AutoPositioningState.ApplicationState, AutoPositioningAlgorithm.ResultCode)
     */
    @NotNull AutoPositioningState.ApplicationState getApplicationState();

    AutoPositioningAlgorithm.ResultCode getPositionComputeResultCode();

    ///////////////////////////////////////////////////////////////////////////
    // node state
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return node state
     * @see IhAutoPositioningManagerListener#onNodeStateChanged(long)
     */
    AutoPositioningState.NodeState getNodeRunningState(long nodeId);

    AutoPositioningState.TaskState getNodeInitiatorCheckStatus(long nodeId);

    AutoPositioningState.TaskState getNodeDistanceCollectionStatus(long nodeId);

    AutoPositioningState.TaskState getNodePositionSaveStatus(long nodeId);

    /**
     * Resets the auto-positioning manager to an initial state when nothing has been
     * tried yet.
     */
    void resetNodeSet(List<AnchorNode> nodes);

    /**
     * Reorders nodes in the set. As a result it also recomputes positions.
     * @param nodeIds determines new order of nodes
     */
    void reorder(List<Long> nodeIds);

    /**
     * First verify that a declared and real initiator is present.
     * The put the network to AP mode - measure distances.
     * After the measurement is done, retrieves distances from network nodes.
     * If the distance collection finishes successfully, positions are computed as well.
     * If the distance collection fails however, the positions are not computed.
     *
     * The state when computed positions are valid can be detected
     * via {@link #getApplicationState()}.
     *
     * @return true if declared initiator has been found among the given nodes and we have started
     *         initiator discovery
     */
    boolean measure();

    /**
     * Presumes that the network has already stored measured distances.
     * We are just retrieving those and trying to compute positions from that.
     */
    void retrieveDistancesFromFailingNodesAndComputePositions();

    void savePositions();

    boolean anyNodeNeedsPositionSave();

    /**
     * @return read-only list of nodes (order is significant)
     */
    List<AnchorNode> getNodes();

    /**
     * @param nodeId identified the node
     * @return computed position for the given node, returns null if the node is not known
     * or the position has not been computed yet
     */
    @Nullable
    ComputedPosition getComputedPosition(long nodeId);

    /**
     * Terminates all currently ongoing operations.
     */
    void terminate();

    /**
     * Workaround for 2-step termination.
     * @param nodeBle identified the node
     */
    boolean hasInProgressConnection(String nodeBle);

    /**
     * @return uniform z-axis value (default is 0)
     */
    int getZaxis();

    /**
     * Sets up uniform z-axis value.
     * @param zAxis z-axis value in mm
     */
    void setZaxis(int zAxis);

}
