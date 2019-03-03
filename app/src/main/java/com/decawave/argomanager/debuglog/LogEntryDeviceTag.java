/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import org.jetbrains.annotations.NotNull;

/**
 * Argo project.
 */

class LogEntryDeviceTag implements LogEntryTag {
    @NotNull public final String bleAddress;

    LogEntryDeviceTag(@NotNull String bleAddress) {
        this.bleAddress = bleAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntryDeviceTag that = (LogEntryDeviceTag) o;

        return bleAddress.equals(that.bleAddress);

    }

    @Override
    public int hashCode() {
        return bleAddress.hashCode();
    }
}
