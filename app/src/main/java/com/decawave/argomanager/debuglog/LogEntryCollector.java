/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import org.jetbrains.annotations.NotNull;

/**
 * Routes logged messages to particular {@link LogBuffer}s.
 */
public interface LogEntryCollector {

    void add(LogEntry logEntry);

    @NotNull LogBuffer getDeviceDebugLog(String nodeBleAddress);

    LogBuffer getDebugLog();

    LogBuffer getPositionLog();

}
