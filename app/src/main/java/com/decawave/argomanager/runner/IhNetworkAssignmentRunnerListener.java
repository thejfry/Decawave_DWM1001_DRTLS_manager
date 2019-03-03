/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listener of network assignment runner.
 *
 * @see NetworkAssignmentRunner
 */
public interface IhNetworkAssignmentRunnerListener extends InterfaceHubHandler {

    /**
     * Call {@link NetworkAssignmentRunner#getNodeAssignStatus(long)} to get more info.
     * @param nodeId identifies the node
     */
    void onNodeStatusChanged(long nodeId);

    void onNetworkAssignmentStatusChanged(NetworkAssignmentRunner.OverallStatus status);

}
