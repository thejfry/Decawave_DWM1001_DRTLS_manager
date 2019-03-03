/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import android.support.annotation.NonNull;

import com.decawave.argomanager.components.ErrorManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Argo project.
 */
public class LogEntryCollectorImpl implements LogEntryCollector {
    ///////////////////////////////////////////////////////////////////////
    // log buffers
    private Map<LogEntryTag,LogBuffer> logBuffersByTag = new HashMap<>();
    private LogBuffer debugLog = new LogBufferImpl();
    private LogBuffer positionLog = new LogBufferImpl();
    // error manager
    private final ErrorManager errorManager;
    private final LogBlockStatus blockStatus;
    // members - managing state/app logic
    private Set<LogEntryTag> reportedNewTags;

    @Inject
    LogEntryCollectorImpl(ErrorManager errorManager, LogBlockStatus blockStatus) {
        this.errorManager = errorManager;
        this.blockStatus = blockStatus;
        this.reportedNewTags = new HashSet<>();
    }

    @Override
    public void add(LogEntry logEntry) {
        // distribution/filtering logic
        routeEntry(logEntry);
        // error reporting logic
        checkError(logEntry);
    }

    private void routeEntry(LogEntry logEntry) {
        // check if the entry is blocked or not
        LogEntryDeviceTag deviceTag = logEntry.getDeviceTag();
        if (deviceTag != null) {
            Severity blockedSeverity = blockStatus.getBlockSeverity(deviceTag.bleAddress);
            if (blockedSeverity != null && !blockedSeverity.lessThan(logEntry.severity)) {
                // skip this entry
                return;
            }
        }
        // all entries are routed to debug log
        debugLog.addLogEntry(logEntry);
        // entries with position tag are routed to position log
        if (logEntry.hasPositionTag()) {
            positionLog.addLogEntry(logEntry);
        }
        // now check which device log buffer is affected
        for (LogEntryTag tag : logEntry.tags) {
            if (tag == null) {
                // skip this one
                continue;
            }
            // check if we have reported existence of such a tag
            if (reportedNewTags.add(tag)) {
                // this is a new tag
                onNewTag(tag);
            }
            // now lookup the log buffer by tag
            LogBuffer buffer = logBuffersByTag.get(tag);
            if (buffer != null) {
                //noinspection unchecked
                buffer.addLogEntry(logEntry);
            }
        }
    }

    private void checkError(LogEntry logEntry) {
        LogEntryDeviceTag tag;
        if (logEntry.errorCode != null && (tag = logEntry.getDeviceTag()) != null) {
            errorManager.reportError(tag.bleAddress, logEntry.exception, logEntry.message, logEntry.errorCode);
        }
    }

    private void onNewTag(LogEntryTag tag) {
        // we will process only those calls which notify us about new device tags
        if (tag instanceof LogEntryDeviceTag) {
            logBuffersByTag.put(tag, new LogBufferImpl());
        }
    }

    @NonNull
    @Override
    public LogBuffer getDeviceDebugLog(String bleAddress) {
        LogEntryTag tag = LogEntryTagFactory.getDeviceLogEntryTag(bleAddress);
        LogBuffer logBuffer = logBuffersByTag.get(tag);
        if (logBuffer == null) {
            logBuffer = new LogBufferImpl();
            logBuffersByTag.put(tag, logBuffer);
        }
        return logBuffer;
    }

    @Override
    public LogBuffer getDebugLog() {
        return debugLog;
    }

    @Override
    public LogBuffer getPositionLog() {
        return positionLog;
    }

}
