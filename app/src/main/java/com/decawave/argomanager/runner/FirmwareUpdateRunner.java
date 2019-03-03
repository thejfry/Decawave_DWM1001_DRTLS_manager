/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import com.decawave.argo.api.struct.NetworkNode;

import java.util.List;
import java.util.Map;

/**
 * Firmware update task aggregating multiple firmware update requests.
 *
 * Register as {@link IhFwUpdateRunnerListener} to get notified about FW update events.
 */
public interface FirmwareUpdateRunner {

    enum NodeUpdateStatus {
        PENDING(false),
        INITIATING(false),
        UPLOADING_FW1(false),
        UPLOADING_FW2(false),
        RESTORING_INITIAL_STATE(false),
        SUCCESS(true),
        SKIPPED_UP_TO_DATE(true),
        CANCELLED(true),
        FAIL(true);

        public final boolean terminal;

        NodeUpdateStatus(boolean terminal) {
            this.terminal = terminal;
        }
    }

    enum OverallStatus {
        NOT_STARTED(false),
        UPDATING(false),
        FINISHED(true),
        TERMINATED(true);

        public final boolean terminal;

        OverallStatus(boolean terminal) {
            this.terminal = terminal;
        }
    }

    void startFwUpdate(List<NetworkNode> nodes);

    NodeUpdateStatus getNodeUpdateStatus(long nodeId);

    OverallStatus getOverallStatus();

    void terminate();

    Integer getUploadByteCounter(long nodeId);

    Map<Long, NodeUpdateStatus> getNodeStatuses();

}
