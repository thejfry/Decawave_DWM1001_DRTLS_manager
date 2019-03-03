/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Argo project.
 */

public interface AutoPositioningState {

    enum OverallState {
        IDLE,
        CHECKING_INITIATOR,
        COLLECTING_DISTANCES,
        SAVING_POSITION
    }

    enum ApplicationState {
        NOT_STARTED(true),
        // initiator discovery
        CHECKING_INITIATOR(false),
        // idle/has result
        INITIATOR_CHECK_FAILED(true),
        INITIATOR_CHECK_TERMINATED(true),
        INITIATOR_CHECK_MISSING(true),
        //DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS(true),
        COLLECTING_DISTANCES(false),
        // idle/has result
        DISTANCE_COLLECTION_FAILED(true),
        DISTANCE_COLLECTION_TERMINATED(true),
        DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL(true),
        DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS(true),
        // in progress
        SAVING_POSITIONS(false),       // only from DISTANCE_COLLECTION_SUCCESS_COMPUTED_POSITIONS
        // idle/has result
        POSITIONS_SAVE_FAILED(true),
        POSITIONS_SAVE_TERMINATED(true),
        POSITIONS_SAVE_SUCCESS(true);

        public final boolean idle;

        ApplicationState(boolean idle) {
            this.idle = idle;
        }
    }

    enum NodeState {
        IDLE,
        CHECKING_INITIATOR,
        COLLECTING_DISTANCES,
        SAVING_POSITION
    }

    enum TaskState {
        NOT_STARTED,
        RUNNING,
        FAILED,
        SUCCESS,
        TERMINATED
    }

    TaskState getInitiatorCheckState();

    TaskState getDistanceCollectionState();

    TaskState getPositionSaveState();

    OverallState getOverallState();

    NodeState getRunningNodeState(long nodeId);

    TaskState getInitiatorCheckState(long nodeId);

    TaskState getNodeDistanceCollectionState(long nodeId);

    TaskState getNodePositionSaveState(long nodeId);

    void setInitiatorCheckState(long nodeId, TaskState state);

    void setNodeDistanceCollectionState(long nodeId, TaskState state);

    void setNodePositionSaveState(long nodeId, TaskState state);

    /**
     * Sets up a new node set.
     * If the set contains a previously known node, it's state is preserved (!).
     * If there is a completely new node, it's state is set to IDLE/task state to NOT_STARTED.
     */
    void reset(@NotNull Set<Long> newNodeIds);

    /**
     * Resets state of all configured nodes to NOT_STARTED.
     */
    void resetStates();

    void terminateOngoingOperation();

}
