/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listener of FW update runner.
 *
 * @see FirmwareUpdateRunner
 */
public interface IhFwUpdateRunnerListener extends InterfaceHubHandler {

    /**
     * Call {@link FirmwareUpdateRunner#getNodeUpdateStatus(long)} to get more info.
     * @param nodeId identified the node
     */
    void onNodeStatusChanged(long nodeId);

    void onNodeUploadProgressChanged(long nodeId, int uploadByteCount);

    void onFwUpdateStatusChanged(FirmwareUpdateRunner.OverallStatus status);

}
