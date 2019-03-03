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

/**
 * Simplistic log entry adapter - we are just logging position.
 */
public class PositionLogBufferEntryAdapter extends RecyclerView.Adapter<LogMessageHolder> {

    private final LogBuffer logBuffer;

    public PositionLogBufferEntryAdapter(LogBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    @Override
    public LogMessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.log_message_info_entry, parent, false);
        return new LogMessageHolder(view);
    }


    @Override
    public void onBindViewHolder(LogMessageHolder holder, int position) {
        LogEntry logEntry = logBuffer.getLogEntries().get(position);
        holder.bind(logEntry.timeInMillis, logEntry.message);
    }

    @Override
    public int getItemCount() {
        return logBuffer.getLogEntries().size();
    }

}
