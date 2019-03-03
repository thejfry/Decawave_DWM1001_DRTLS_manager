/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.support.v7.widget.RecyclerView;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.debuglog.LogBuffer;
import com.decawave.argomanager.debuglog.LogEntry;
import com.decawave.argomanager.ui.listadapter.LogMessageHolder;
import com.decawave.argomanager.ui.listadapter.PositionLogBufferEntryAdapter;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

/**
 * Fragment showing debug logs.
 */
public class PositionLogBufferFragment extends LogBufferFragment {

    public PositionLogBufferFragment() {
        super(FragmentType.POSITION_LOG, daApp.getString(R.string.screen_title_position_log), "position.log");
    }

    @Override
    LogBuffer getLogBuffer() {
        return logEntryCollector.getPositionLog();
    }

    @Override
    protected RecyclerView.Adapter<LogMessageHolder> createAdapter() {
        return new PositionLogBufferEntryAdapter(getLogBuffer());
    }

    @Override
    // simplified logging is sufficient
    protected void formatLogEntry(StringBuilder sb, LogEntry logEntry) {
        if (Constants.DEBUG) {
            Preconditions.checkState(logEntry.exception == null);
            Preconditions.checkState(logEntry.errorCode == null);
        }
        Util.formatLogEntry(sb, logEntry.timeInMillis, logEntry.message);
    }

}
