/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.File;

import rx.functions.Action1;
import rx.functions.Action3;

/**
 * Argo project.
 */

public interface LogBuffer {

    void clear();

    interface Listener {

        void onNewLogEntry(LogEntry logEntry, boolean replaceInAction);

        void onLogReset();

    }

    void setListener(Listener listener);

    void addLogEntry(LogEntry logEntry);

    /**
     * WARNING: either process the returned list immediately or create a copy first
     */
    CircularFifoQueue<LogEntry> getLogEntries();

    void saveLogToFile(File file, Action3<LogEntry, Long, StringBuilder> logEntryFormatter, Action1<Void> onSuccess, Action1<Throwable> onFail);

}
