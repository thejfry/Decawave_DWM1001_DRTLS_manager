/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Singleton;

import eu.kryl.android.common.android.AndroidValidate;
import rx.functions.Action1;
import rx.functions.Action3;

import static com.decawave.argomanager.ArgoApp.log;
import static com.decawave.argomanager.ArgoApp.uiHandler;
import static com.decawave.argomanager.ArgoApp.workerSbHandler;

/**
 * Generic log buffer.
 */
@Singleton
class LogBufferImpl implements LogBuffer {
    private static final int DEFAULT_CAPACITY = 4000;

    // buffer for collected log entries
    private final CircularFifoQueue<LogEntry> logEntries;
    //
    private final int capacity;
    private Listener listener;

    LogBufferImpl() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("WeakerAccess")
    LogBufferImpl(int capacity) {
        this.capacity = capacity;
        this.logEntries = new CircularFifoQueue<>(capacity);
    }

    @Override
    public void clear() {
        this.logEntries.clear();
        if (listener != null) {
            listener.onLogReset();
        }
    }

    @Override
    public final void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public final void addLogEntry(LogEntry logEntry) {
        AndroidValidate.runningOnUiThread();
        boolean replace = logEntries.size() == capacity;
        logEntries.add(logEntry);
        if (listener != null) {
            listener.onNewLogEntry(logEntry, replace);
        }
    }

    @Override
    public final CircularFifoQueue<LogEntry> getLogEntries() {
        AndroidValidate.runningOnUiThread();
        // return a copy
        return logEntries;
    }

    @Override
    public final void saveLogToFile(File file, Action3<LogEntry, Long, StringBuilder> logEntryFormatter, Action1<Void> onSuccess, Action1<Throwable> onFail) {
        // create a copy for asynchronous processing
        ArrayList<LogEntry> lst = new ArrayList<>(logEntries);
        // pass onto worker handler
        workerSbHandler.post(() -> {
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(file));
                StringBuilder sb = new StringBuilder();
                if (lst.isEmpty()) {
                    // just touch the file
                    bw.write("");
                } else {
                    long firstTime = lst.get(0).timeInMillis;
                    for (LogEntry logEntry : lst) {
                        sb.setLength(0);
                        logEntryFormatter.call(logEntry, firstTime, sb);
                        bw.write(sb.toString());
                    }
                }
                // notify about the result on UI handler
                uiHandler.post(() -> onSuccess.call(null));
            } catch (IOException e) {
                // notify about the result on UI handler
                uiHandler.post(() -> onFail.call(e));
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        log.e("cannot close file, ignoring", e);
                    }
                }
            }
        });
    }

}