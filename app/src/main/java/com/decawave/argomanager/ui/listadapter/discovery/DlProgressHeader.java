/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.view.View;

import com.decawave.argomanager.R;
import com.google.common.base.Preconditions;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */

class DlProgressHeader extends AbstractFlexibleItem<DlItemViewHolder.Title> {
    // state represented by this view
    private final boolean discovering;
    private final int count;

    DlProgressHeader(boolean discovering, int count) {
        this.discovering = discovering;
        this.count = count;
        // header cannot be clicked!
        setEnabled(false);
    }

    @Override
    public boolean equals(Object o) {
        // there are no two distinct headers in the discovery list
        return o instanceof DlProgressHeader;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.li_discovery_info;
    }

    @Override
    public DlItemViewHolder.Title createViewHolder(View view, FlexibleAdapter adapter) {
        return new DlItemViewHolder.Title(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, DlItemViewHolder.Title holder, int position, List payloads) {
        // retrieve count of non-persisted nodes
        if (discovering) {
            holder.discoveryInfo.setText(daApp.getResources().getQuantityString(R.plurals.devices_found_so_far, count, count));
        } else {
            Preconditions.checkState(count == 0);
            holder.discoveryInfo.setText(R.string.no_nearby_devices_found);
        }
    }

}
