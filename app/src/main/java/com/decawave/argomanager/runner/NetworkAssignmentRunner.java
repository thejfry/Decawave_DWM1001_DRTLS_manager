/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.runner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Firmware update task aggregating multiple firmware update requests.
 *
 * Register as {@link IhFwUpdateRunnerListener} to get notified about FW update events.
 */
public interface NetworkAssignmentRunner {

    enum NodeAssignStatus {
        PENDING(false),
        INITIATING(false),
        ASSIGNING(false),
        SUCCESS(true),
        CANCELLED(true),
        FAIL(true);

        public final boolean terminal;

        NodeAssignStatus(boolean terminal) {
            this.terminal = terminal;
        }
    }

    enum OverallStatus {
        NOT_STARTED(false),
        ASSIGNING(false),
        FINISHED(true),
        TERMINATED(true);

        public final boolean terminal;

        OverallStatus(boolean terminal) {
            this.terminal = terminal;
        }
    }

    void startAssignment(List<Long> nodeIds);

    @Nullable NodeAssignStatus getNodeAssignStatus(long nodeId);

    @NotNull OverallStatus getOverallStatus();

    void terminate();

}
