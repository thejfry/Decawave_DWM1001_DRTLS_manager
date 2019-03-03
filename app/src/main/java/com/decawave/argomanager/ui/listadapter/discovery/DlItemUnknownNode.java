/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.view.View;

import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.runner.NetworkAssignmentRunner;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.util.Util;

import java.util.Objects;

import eu.davidea.flexibleadapter.FlexibleAdapter;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */

class DlItemUnknownNode extends DlFlexibleItem<DlItemViewHolder.DiscoveredNode, PolymorphicDiscoveryList.NodeListItem> {
    private final Supplier<NetworkAssignmentRunner> networkAssignmentRunnerSupplier;

    DlItemUnknownNode(PolymorphicDiscoveryList.NodeListItem item,
                      DlSectionHeader header,
                      Supplier<NetworkAssignmentRunner> networkAssignmentRunnerSupplier) {
        super(item, header);
        this.networkAssignmentRunnerSupplier = networkAssignmentRunnerSupplier;
    }

    @Override
    boolean isEqual(PolymorphicDiscoveryList.NodeListItem otherItem) {
        return Objects.equals(otherItem.node.getId(), item.node.getId());
    }

    @Override
    public int hashCode() {
        return (int) (long) item.node.getId();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.li_discovered_node;
    }

    @Override
    public DlItemViewHolder.DiscoveredNode createViewHolder(View view, FlexibleAdapter adapter) {
        return new DlItemViewHolder.DiscoveredNode(view, adapter);
    }

    @Override
    protected void onBind(FlexibleAdapter adapter, DlItemViewHolder.DiscoveredNode holder, int position, boolean[] enforceSeparatorVisible) {
        NetworkNode networkNode = item.node;
        String label = networkNode.getLabel();
        Long nodeId = networkNode.getId();
        if (label == null) {
            label = Util.shortenNodeId(nodeId, false);
        }
        boolean isAnchor = networkNode.isAnchor();
        holder.nodeTypeView.setState(isAnchor ? NodeStateView.State.ANCHOR : NodeStateView.State.TAG, false);
        holder.nodeName.setText(daApp.getString(isAnchor ? R.string.anchor_name : R.string.tag_name, label));
        // show it
        holder.tvNodeId.setText(daApp.getString(R.string.node_id_pattern, Util.formatAsHexa(nodeId, true)));
        holder.tvNodeId.setVisibility(View.VISIBLE);
        //
        holder.bleAddress.setText(daApp.getString(R.string.node_ble_pattern, networkNode.getBleAddress()));
        // set up initial flipview state
        holder.flipView.flipSilently(adapter.isSelected(position));
        // progress
        NetworkAssignmentRunner.NodeAssignStatus status = networkAssignmentRunnerSupplier.get().getNodeAssignStatus(item.node.getId());
        if (status != null && !status.terminal) {
            // it's in progress
            holder.progress.makeIndeterminate();
            enforceSeparatorVisible[0] = true;
        } else {
            holder.progress.makeInactive();
        }
        // fail indicator
        holder.failIndicator.setVisibility(status != null && status == NetworkAssignmentRunner.NodeAssignStatus.FAIL
                ? View.VISIBLE : View.GONE);
    }

}
