/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.view.View;

import com.decawave.argomanager.R;

import eu.davidea.flexibleadapter.FlexibleAdapter;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */

class DlItemDeclaredNetwork extends DlFlexibleItem<DlItemViewHolder.DeclaredNetwork, PolymorphicDiscoveryList.DeclaredNetworkListItem> {


    DlItemDeclaredNetwork(PolymorphicDiscoveryList.DeclaredNetworkListItem networkItem, DlSectionHeader header) {
        super(networkItem, header);
    }

    @Override
    boolean isEqual(PolymorphicDiscoveryList.DeclaredNetworkListItem otherItem) {
        return otherItem.networkId == item.networkId;
    }

    @Override
    public int hashCode() {
        return item.networkId;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.li_declared_network;
    }

    @Override
    public DlItemViewHolder.DeclaredNetwork createViewHolder(View view, FlexibleAdapter adapter) {
        return new DlItemViewHolder.DeclaredNetwork(view, adapter);
    }

    @Override
    public void onBind(FlexibleAdapter adapter,
                       DlItemViewHolder.DeclaredNetwork holder, int position, boolean[] b) {
        holder.networkName.setText(daApp.getString(R.string.declared_network_title, item.getNetworkName()));
        // tags
        int tags = item.getTagCount();
        // anchors
        int anchors = item.getAnchorCount();
        holder.anchorNodes.setText(daApp.getResources().getQuantityString(R.plurals.number_of_anchors, anchors, anchors));
        holder.tagNodes.setText(daApp.getResources().getQuantityString(R.plurals.number_of_tags, tags, tags));
    }

}
