/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decawave.argomanager.R;
import com.decawave.argomanager.debuglog.LogBuffer;
import com.decawave.argomanager.debuglog.LogEntry;
import com.decawave.argomanager.debuglog.Severity;
import com.decawave.argomanager.error.ErrorCodeInterpreter;

/**
 *
 */

public class DebugLogBufferEntryAdapter extends RecyclerView.Adapter<LogMessageHolder> {
    // view types
    private static final int TYPE_INFO = 0;
    private static final int TYPE_INFO_BOLD = 1;
    private static final int TYPE_WARN = 2;
    private static final int TYPE_ERROR = 3;

    private final LogBuffer logBuffer;

    public DebugLogBufferEntryAdapter(LogBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    @Override
    public LogMessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        int resId;
        switch (viewType) {
            case TYPE_INFO:
                resId = R.layout.log_message_info_entry;
                break;
            case TYPE_WARN:
                resId = R.layout.log_message_warning_entry;
                break;
            case TYPE_INFO_BOLD:
                resId = R.layout.log_message_info_bold_entry;
                break;
            case TYPE_ERROR:
                resId = R.layout.log_message_error_entry;
                break;
            default:
                throw new IllegalStateException();
        }
        view = inflater.inflate(resId, parent, false);
        view.setTag(viewType);
        return new LogMessageHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Severity s = getLogEntry(position).severity;
        switch (s) {
            case DEBUG:
            case INFO:
                return TYPE_INFO;
            case IMPORTANT:
                return TYPE_INFO_BOLD;
            case WARNING:
                return TYPE_WARN;
            case ERROR:
                return TYPE_ERROR;
            default:
                throw new IllegalStateException("unsupported severity: " + s);
        }
    }

    private LogEntry getLogEntry(int position) {
        return logBuffer.getLogEntries().get(position);
    }


    @Override
    public void onBindViewHolder(LogMessageHolder holder, int position) {
        LogEntry logEntry = getLogEntry(position);
        StringBuilder sb = new StringBuilder(logEntry.message);
        if (logEntry.errorCode != null) {
            sb.append(" [errorCode ").append(logEntry.errorCode).append(": ").append(ErrorCodeInterpreter.getName(logEntry.errorCode)).append("]");
        }
        holder.bind(logEntry.timeInMillis, sb.toString());
    }

    @Override
    public int getItemCount() {
        return logBuffer.getLogEntries().size();
    }


}
