/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util.gatt;

import android.support.annotation.Nullable;

/**
 * Argo project.
 */
class DeviceInfo {
    long nodeId;
    int hwVersion;
    int fw1Version;
    int fw2Version;
    int fw1Checksum;
    int fw2Checksum;
    @Nullable
    Boolean bridge;

    public long getId() {
        return nodeId;
    }

    public long getNodeId() {
        return nodeId;
    }

    int getHwVersion() {
        return hwVersion;
    }

    int getFw1Version() {
        return fw1Version;
    }

    int getFw2Version() {
        return fw2Version;
    }

    int getFw1Checksum() {
        return fw1Checksum;
    }

    int getFw2Checksum() {
        return fw2Checksum;
    }

    @Nullable
    Boolean getBridge() {
        return bridge;
    }
}
