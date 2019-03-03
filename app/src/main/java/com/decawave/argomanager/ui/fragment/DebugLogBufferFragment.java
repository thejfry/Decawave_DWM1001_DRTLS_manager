/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.support.v7.widget.RecyclerView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.debuglog.LogBuffer;
import com.decawave.argomanager.ui.listadapter.DebugLogBufferEntryAdapter;
import com.decawave.argomanager.ui.listadapter.LogMessageHolder;

/**
 * Fragment showing debug logs.
 */
public class DebugLogBufferFragment extends LogBufferFragment {

    public DebugLogBufferFragment() {
        super(FragmentType.DEBUG_LOG, daApp.getString(R.string.screen_title_debug_log), "debug.log");
    }

    @Override
    LogBuffer getLogBuffer() {
        return logEntryCollector.getDebugLog();
    }

    @Override
    protected RecyclerView.Adapter<LogMessageHolder> createAdapter() {
        return new DebugLogBufferEntryAdapter(getLogBuffer());
    }

}
