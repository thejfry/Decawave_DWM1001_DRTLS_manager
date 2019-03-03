/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.AutoPositioningState;
import com.decawave.argomanager.components.impl.AutoPositioningAlgorithm;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listener of auto positioning manager.
 *
 * @see AutoPositioningManager
 */
public interface IhAutoPositioningManagerListener extends InterfaceHubHandler {

    /**
     * Call {@link AutoPositioningManager#getNodeRunningState(long)},
     * {@link AutoPositioningManager#getNodeInitiatorCheckStatus(long)},
     * {@link AutoPositioningManager#getNodeDistanceCollectionStatus(long)}
     * and {@link AutoPositioningManager#getNodePositionSaveStatus(long)} to get more info.
     * @param nodeId identified the node
     */
    void onNodeStateChanged(long nodeId);

    /**
     * Call {@link AutoPositioningManager#getApplicationState()} to get more info.
     * @param state application state
     * @param computationResult if the application state change led also to position recompute
     */
    void onApplicationStateChanged(AutoPositioningState.ApplicationState state, AutoPositioningAlgorithm.ResultCode computationResult);

    /**
     * Node set (or order - which is less significant) has changed.
     * Might also invalidate any previously saved application state ({@link #onApplicationStateChanged(AutoPositioningState.ApplicationState, AutoPositioningAlgorithm.ResultCode)}.
     * @param computationResult is non-null if the reorder led to position re-compute (because we have successfully collected distances)
     */
    void onNodeSetChange(AutoPositioningAlgorithm.ResultCode computationResult);

}
