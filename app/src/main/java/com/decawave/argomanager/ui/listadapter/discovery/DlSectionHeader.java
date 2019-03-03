/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.view.View;

import com.decawave.argomanager.R;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;

/**
 * Discovery list section header.
 */

class DlSectionHeader extends AbstractHeaderItem<DlItemViewHolder.SectionHeader> {

    final PolymorphicDiscoveryList.ItemType sectionType;

    ///////////////////////////////////////////////////////////////////////////
    // bodies
    ///////////////////////////////////////////////////////////////////////////

    DlSectionHeader(PolymorphicDiscoveryList.ItemType sectionType) {
        this.sectionType = sectionType;
        // header cannot be clicked!
        setSelectable(false);
        setEnabled(false);
    }


    @Override
    public boolean equals(Object o) {
        // we cannot have two sections/headers of the same type
        return o instanceof DlSectionHeader
                && ((DlSectionHeader) o).sectionType == sectionType;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.li_discovery_section_title;
    }

    @Override
    public DlItemViewHolder.SectionHeader createViewHolder(View view, FlexibleAdapter adapter) {
        return new DlItemViewHolder.SectionHeader(view, adapter, true);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter,
                               DlItemViewHolder.SectionHeader holder,
                               int position,
                               List payloads) {
        // we just need to bind proper label
        int labelResourceId;
        switch (sectionType) {
            case DECLARED_NETWORK:
                labelResourceId = R.string.discovery_section_title_networks;
                break;
            case UNKNOWN_NETWORK:
                labelResourceId = R.string.discovery_section_title_unassigned_networks;
                break;
            case UNKNOWN_NODE:
                labelResourceId = R.string.discovery_section_title_unassigned_devices;
                break;
            default:
                throw new IllegalStateException("unsupported section type: " + sectionType);
        }
        holder.tvTitle.setText(labelResourceId);
    }
}
