/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decorated log entry.
 */
public class LogEntry {
    private static final LogEntryTag[] EMPTY_TAG_ARRAY = new LogEntryTag[0];
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public final LogEntryTag[] tags;
    public final long timeInMillis;
    @NotNull
    public final Severity severity;
    @NotNull
    public final String message;
    @Nullable
    public final Integer errorCode;
    @Nullable
    public final Throwable exception;

    public LogEntry(long timeInMillis, String message) {
        this(timeInMillis, message, Severity.INFO);
    }

    public LogEntry(long timeInMillis, String message, Severity severity, LogEntryTag... tags) {
        this(timeInMillis, message, severity, null, null, tags);
    }

    @SuppressWarnings("NullableProblems")
    public LogEntry(long timeInMillis, String message, @NonNull Severity severity, Integer errorCode, Throwable exception, LogEntryTag... tags) {
        this.timeInMillis = timeInMillis;
        this.severity = severity;
        this.message = message;
        this.errorCode = errorCode;
        this.exception = exception;
        if (tags != null && tags.length > 0) {
            this.tags = tags;
        } else {
            this.tags = EMPTY_TAG_ARRAY;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public LogEntryDeviceTag getDeviceTag() {
        for (LogEntryTag tag : tags) {
            if (tag instanceof LogEntryDeviceTag) {
                return (LogEntryDeviceTag) tag;
            }
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean hasPositionTag() {
        for (LogEntryTag tag : tags) {
            if (tag == LogEntryPositionTag.INSTANCE) {
                return true;
            }
        }
        return false;
    }
}
