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

class LogEntryComponentTag implements LogEntryTag {
    @NotNull
    private final String componentName;

    LogEntryComponentTag(@NotNull String componentName) {
        this.componentName = componentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntryComponentTag that = (LogEntryComponentTag) o;

        return componentName.equals(that.componentName);

    }

    @Override
    public int hashCode() {
        return componentName.hashCode();
    }
}
