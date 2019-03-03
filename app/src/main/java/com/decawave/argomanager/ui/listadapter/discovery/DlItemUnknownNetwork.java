/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.view.View;

import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.util.Util;

import eu.davidea.flexibleadapter.FlexibleAdapter;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */

class DlItemUnknownNetwork extends DlFlexibleItem<DlItemViewHolder.UnknownNetwork, PolymorphicDiscoveryList.UnknownNetworkListItem> {


    DlItemUnknownNetwork(PolymorphicDiscoveryList.UnknownNetworkListItem networkItem,
                         DlSectionHeader header) {
        super(networkItem, header);
    }

    @Override
    boolean isEqual(PolymorphicDiscoveryList.UnknownNetworkListItem otherItem) {
        return otherItem.networkId == item.networkId;
    }

    @Override
    public int hashCode() {
        return item.networkId;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.li_discovered_network;
    }

    @Override
    public DlItemViewHolder.UnknownNetwork createViewHolder(View view, FlexibleAdapter adapter) {
        return new DlItemViewHolder.UnknownNetwork(view, adapter);
    }

    @Override
    protected void onBind(FlexibleAdapter adapter, DlItemViewHolder.UnknownNetwork holder, int position, boolean[] b) {
        holder.networkName.setText(daApp.getString(R.string.discovered_panid_title, Util.formatNetworkId(item.networkId)));
        //
        int total = item.nodeContainer.getNodes(false).size();
        int tags = item.nodeContainer.getNodes(NetworkNode::isTag).size();
        // anchors is the rest
        int anchors = total - tags;
        holder.anchorNodes.setText(
                daApp.getResources().getQuantityString(R.plurals.number_of_anchors, anchors, anchors));
        holder.tagNodes.setText(
                daApp.getResources().getQuantityString(R.plurals.number_of_tags, tags, tags));
    }

}
