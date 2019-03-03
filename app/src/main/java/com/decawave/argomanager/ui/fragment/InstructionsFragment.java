/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows recyclerView of networks and unassigned nodes.
 */
public class InstructionsFragment extends AbstractArgoFragment {
    private static final String BK_ANCHOR_NAME = "ANCHOR";

    // dependencies
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    @BindView(R.id.htmlInstructions)
    WebView webView;

    public InstructionsFragment() {
        super(FragmentType.INSTRUCTIONS);
    }

    public static Bundle getBundleForAnchor(@SuppressWarnings("SameParameterValue") String anchorName) {
        Bundle b = new Bundle();
        b.putString(BK_ANCHOR_NAME, anchorName);
        return b;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.instructions, container, false);
        ButterKnife.bind(this, v);
        Bundle b = getArguments();
        String a;
        String anchor = b != null && (a = b.getString(BK_ANCHOR_NAME)) != null ? a : null;
        // set the content
        webView.loadUrl("file:///android_asset/MDEK1001_instruction.html" + (anchor == null ? "" : "#" + anchor));
        // the adapter contents will be set later (onResume)
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        appPreferenceAccessor.setInstructionsRead();
    }

}
