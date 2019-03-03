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

import com.decawave.argo.api.struct.UwbMode;
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

/** Dialog to pick an item */
public class UwbModePickerDialogFragment extends DialogFragment {

    public static final ComponentLog log = new ComponentLog(UwbModePickerDialogFragment.class);

    private static final String FRAGMENT_TAG = "uwbmodepicker";

    private static final String BK_SELECTED_MODE = "selected";

    // ***************************
    // * INPUT
    // ***************************

    @SuppressWarnings("NullableProblems")
    @NotNull
    private UwbMode[] uwbModes;

    @Nullable
    private UwbMode selectedUwbMode;

    // ***************************
    // * OTHER
    // ***************************

    private AlertDialog dlg;
    private Adapter adapter;

    // ***************************
    // * CONSTRUCTOR
    // ***************************

    public UwbModePickerDialogFragment() {
        uwbModes = UwbMode.values();
    }

    public static Bundle getArgsForMode(UwbMode uwbMode) {
        Bundle b = new Bundle();
        if (uwbMode != null) {
            b.putString(BK_SELECTED_MODE, uwbMode.name());
        }
        return b;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (selectedUwbMode != null) {
            bundle.putString(BK_SELECTED_MODE, selectedUwbMode.name());
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
            if (b.containsKey(BK_SELECTED_MODE)) {
                setSelecteUwbMode(UwbMode.valueOf(b.getString(BK_SELECTED_MODE)));
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

    public static void showDialog(FragmentManager fm, @Nullable UwbMode selectedUwbMode) {
        final UwbModePickerDialogFragment f = new UwbModePickerDialogFragment();
        if (selectedUwbMode != null) {
            f.setArguments(getArgsForMode(selectedUwbMode));
        }
        f.show(fm, UwbModePickerDialogFragment.FRAGMENT_TAG);
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
            holder.bind(uwbModes[position]);
        }

        @Override
        public int getItemCount() {
            return uwbModes.length;
        }

    }

    public boolean setSelecteUwbMode(@Nullable UwbMode uwbMode) {
        boolean b = this.selectedUwbMode != uwbMode;
        this.selectedUwbMode = uwbMode;
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
        UwbMode uwbMode;

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
            UwbMode newUwbMode = (UwbMode) view.getTag();
            if (selectedUwbMode != newUwbMode) {
                // broadcast and dismiss
                InterfaceHub.getHandlerHub(IhCallback.class).onUwbModePicked(newUwbMode);
                dismiss();
            }
        }

        void bind(UwbMode uwbMode) {
            // assign data bean
            this.uwbMode = uwbMode;
            // set up visual elements content
            this.tvNodeType.setText(Util.formatUwbMode(this.uwbMode));
            // toggle radio button
            rb.setChecked(selectedUwbMode == this.uwbMode);
            //
            container.setTag(this.uwbMode);
        }
    }

    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onUwbModePicked(UwbMode uwbMode);

    }


}