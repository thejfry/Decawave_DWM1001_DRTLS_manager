/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.decawave.argo.api.struct.NodeType;
import com.decawave.argomanager.R;
import com.decawave.argomanager.util.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;

/** Dialog to pick a item */
public class NodeTypePickerDialogFragment extends DialogFragment {

    public static final ComponentLog log = new ComponentLog(NodeTypePickerDialogFragment.class);

    private static final String FRAGMENT_TAG = "nodetypepicker";

    private static final String BK_SELECTED_TYPE = "selected";

    // ***************************
    // * INPUT
    // ***************************

    @SuppressWarnings("NullableProblems")
    @NotNull
    private NodeType[] nodeTypes;

    @Nullable
    private NodeType selectedNodeType;

    // ***************************
    // * OTHER
    // ***************************

    private AlertDialog dlg;
    private Adapter adapter;

    // ***************************
    // * CONSTRUCTOR
    // ***************************

    public NodeTypePickerDialogFragment() {
        nodeTypes = NodeType.values();
    }

    public static Bundle getArgsForNodeType(NodeType nodeType) {
        Bundle b = new Bundle();
        if (nodeType != null) {
            b.putString(BK_SELECTED_TYPE, nodeType.name());
        }
        return b;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (selectedNodeType != null) {
            bundle.putString(BK_SELECTED_TYPE, selectedNodeType.name());
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle b = bundle;
        if (b == null) {
            b = getArguments();
        }
        if (b != null) {
            if (b.containsKey(BK_SELECTED_TYPE)) {
                setSelectedNodeType(NodeType.valueOf(b.getString(BK_SELECTED_TYPE)));
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        adapter = new Adapter();
        @SuppressLint("InflateParams")
        final View content = LayoutInflater.from(getActivity()).inflate(R.layout.dlg_item_picker, null);
        final RecyclerView recyclerView = (RecyclerView) content.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        builder.setView(content);
        builder.setRemoveTopPadding(true);

        dlg = builder.create();

        return dlg;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static void showDialog(FragmentManager fm, @Nullable NodeType selectedNodeType) {
        final NodeTypePickerDialogFragment f = new NodeTypePickerDialogFragment();
        if (selectedNodeType != null) {
            f.setArguments(getArgsForNodeType(selectedNodeType));
        }
        f.show(fm, NodeTypePickerDialogFragment.FRAGMENT_TAG);
    }

    // ***************************
    // * INNER CLASSES
    // ***************************

    /**
     * Adapter
     */
    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(R.layout.li_dlg_update_rate_picker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.bind(nodeTypes[position]);
        }

        @Override
        public int getItemCount() {
            return nodeTypes.length;
        }

    }

    public boolean setSelectedNodeType(@Nullable NodeType nodeType) {
        boolean b = this.selectedNodeType != nodeType;
        this.selectedNodeType = nodeType;
        return b;
    }

    /**
     * View holder
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.radio)
        RadioButton rb;
        @BindView(R.id.tvUpdateRate)
        TextView tvNodeType;
        //
        private View container;
        // data bean
        NodeType nodeType;

        ViewHolder(View v) {
            super(v);
            // extract references to visual elements
            ButterKnife.bind(this, v);
            // set up the listener
            v.setOnClickListener(this);
            container = v;
        }

        @Override
        public void onClick(View view) {
            NodeType newNodeType = (NodeType) view.getTag();
            if (selectedNodeType != newNodeType) {
                // broadcast and dismiss
                InterfaceHub.getHandlerHub(IhCallback.class).onNodeTypePicked(newNodeType);
                dismiss();
            }
        }

        void bind(NodeType nodeType) {
            // assign data bean
            this.nodeType = nodeType;
            // set up visual elements content
            this.tvNodeType.setText(Util.nodeTypeString(this.nodeType));
            // toggle radio button
            rb.setChecked(selectedNodeType == this.nodeType);
            //
            container.setTag(this.nodeType);
        }
    }

    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onNodeTypePicked(NodeType nodeType);

    }


}