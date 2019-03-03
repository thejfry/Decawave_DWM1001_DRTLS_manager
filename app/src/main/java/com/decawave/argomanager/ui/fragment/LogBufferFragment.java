/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.debuglog.LogBuffer;
import com.decawave.argomanager.debuglog.LogEntry;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.ui.listadapter.LogMessageHolder;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.File;

import javax.inject.Inject;

import static com.decawave.argomanager.util.Util.shareUriContent;

/**
 * Common predecessor for fragment showing debug logs.
 */
public abstract class LogBufferFragment extends AbstractArgoFragment {
    // dependencies
    @Inject
    LogEntryCollector logEntryCollector;
    // members
    private RecyclerView listView;
    private final String logTitle;
    private final String logFilename;
    RecyclerView.Adapter<LogMessageHolder> adapter;

    public LogBufferFragment(FragmentType fragmentType, String logTitle, String logFilename) {
        super(fragmentType);
        this.logTitle = logTitle;
        this.logFilename = logFilename;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_log_fragment, container, false);
        configureListView(v);
        return v;
    }

    protected void configureListView(View v) {
        listView = (RecyclerView) v.findViewById(R.id.logEntries);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(llm);
        listView.setAdapter(adapter);
        listView.setHasFixedSize(true);
    }

    abstract LogBuffer getLogBuffer();

    @Override
    public final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleOnCreate(savedInstanceState);
        adapter = createAdapter();
        setHasOptionsMenu(true);
    }

    protected void handleOnCreate(Bundle savedInstanceState) {
    }

    protected abstract RecyclerView.Adapter<LogMessageHolder> createAdapter();

    // may be overridden
    protected void formatLogEntry(StringBuilder sb, LogEntry logEntry) {
        Util.formatLogEntry(sb, logEntry.timeInMillis, logEntry.message, logEntry.errorCode, logEntry.exception);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
        menu.findItem(R.id.action_copy).setOnMenuItemClickListener((v) -> handleCopyToClipboardActionClick());
        menu.findItem(R.id.action_share).setOnMenuItemClickListener((v) -> handleShareActionClick());
        menu.findItem(R.id.action_clear).setOnMenuItemClickListener((v) -> handleClearActionClick());
    }

    private boolean handleClearActionClick() {
        getLogBuffer().clear();
        // we will receive notification via listen method onClear()
        return true;
    }

    private boolean handleCopyToClipboardActionClick() {
        StringBuilder sb = new StringBuilder();
        CircularFifoQueue<LogEntry> logEntries = getLogBuffer().getLogEntries();
        if (logEntries.isEmpty()) {
            return true;
        } // else:
        for (LogEntry logEntry : logEntries) {
            formatLogEntry(sb, logEntry);
        }
        // copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) daApp.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(logTitle, sb.toString());
        clipboard.setPrimaryClip(clip);
        ToastUtil.showToast(R.string.copied_to_clipboard);
        // done
        return true;
    }


    private boolean handleShareActionClick() {
        File parentDir = new File(daApp.getCacheDir(), "log");
        if (!parentDir.exists()) {
            if (!parentDir.mkdir()) {
                ToastUtil.showToast("cannot create output directory! " + parentDir.getAbsolutePath());
            }
        }
        File f = new File(parentDir, logFilename);
        getLogBuffer().saveLogToFile(f,
                (logEntry, firstTime, sb) -> formatLogEntry(sb, logEntry),
                (success) -> {
                    Uri providerUri = FileProvider.getUriForFile(daApp, "com.decawave.argomanager", f);
                    if (Constants.DEBUG) log.d("generated provider URI for " + logTitle + ": " + providerUri);
                    shareUriContent(providerUri, "text/plain");
                },
                (fail) -> ToastUtil.showToast(logTitle + " save failed! " + fail.getMessage()));
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //
        //noinspection unchecked
        getLogBuffer().setListener(new LogBuffer.Listener() {

            @Override
            public void onNewLogEntry(LogEntry logEntry, boolean replaceInAction) {
                if (!replaceInAction) {
                    // rebuild only the last item of the view
                    updateUi(getLogBuffer().getLogEntries().size() - 1);
                } else {
                    updateUi(-1);
                }
            }

            @Override
            public void onLogReset() {
                updateUi(-1);
            }
        });
        // set the content of the text view
        updateUi(-1);
    }

    @Override
    public void onPause() {
        super.onPause();
        getLogBuffer().setListener(null);
    }

    protected void updateUi(int newLogEntryIndex) {
        if (newLogEntryIndex == -1) {
            adapter.notifyDataSetChanged();
            listView.scrollToPosition(adapter.getItemCount() - 1);
        } else {
            adapter.notifyItemInserted(newLogEntryIndex);
            int range = listView.computeVerticalScrollRange();
            int verticalOffset = listView.computeVerticalScrollOffset();
            int verticalExtent = listView.computeVerticalScrollExtent();
            if (verticalExtent + verticalOffset >= (range - verticalExtent * 0.1)) {
                // it seems that the user is viewing tail of the log
                listView.scrollToPosition(newLogEntryIndex);
                listView.setScrollbarFadingEnabled(false);
            } else {
                // hide the scrollbar - cause we cannot force android to do recalculation on the fly
                listView.setScrollbarFadingEnabled(true);
            }
        }
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }


}
