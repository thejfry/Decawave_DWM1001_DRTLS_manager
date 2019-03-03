/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractSectionableItem;

/**
 * Discovery list flexible item.
 */

abstract class DlFlexibleItem<T extends DlItemViewHolder.Generic, U extends PolymorphicDiscoveryList.DiscoveryListBean> extends AbstractSectionableItem<T,DlSectionHeader> {
    public final U item;

    DlFlexibleItem(U item, DlSectionHeader header) {
        super(header);
        this.item = item;
    }

    @Override
    public boolean equals(Object o) {
        //noinspection unchecked
        return o instanceof DlFlexibleItem
                && ((DlFlexibleItem) o).item.type == item.type
                && isEqual((U) ((DlFlexibleItem) o).item);
    }

    @Override
    public final void bindViewHolder(FlexibleAdapter adapter, T holder, int position, List payloads) {
        boolean[] ev = { false };
        // call item-specific routine
        onBind(adapter, holder, position, ev);
    }

    abstract boolean isEqual(U otherItem);

    protected abstract void onBind(FlexibleAdapter adapter, T holder, int position, boolean[] enforceSeparatorVisible);

}