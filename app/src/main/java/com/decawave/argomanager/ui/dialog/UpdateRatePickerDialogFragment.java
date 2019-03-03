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

import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ext.UpdateRate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;

/** Dialog to pick a item */
public class UpdateRatePickerDialogFragment extends DialogFragment {

    public static final ComponentLog log = new ComponentLog(UpdateRatePickerDialogFragment.class);

    private static final String FRAGMENT_TAG = "updateratepicker";

    private static final String BK_SELECTED_UPDATE_RATE = "selected";
    private static final String BK_FLAG = "flag";

    // ***************************
    // * INPUT
    // ***************************

    @SuppressWarnings("NullableProblems")
    @NotNull
    private UpdateRate[] updateRates;

    @Nullable
    private UpdateRate selectedUpdateRate;

    private boolean flag;

    // ***************************
    // * OTHER
    // ***************************

    private AlertDialog dlg;
    private Adapter adapter;

    // ***************************
    // * CONSTRUCTOR
    // ***************************

    public UpdateRatePickerDialogFragment() {
        updateRates = UpdateRate.values();
    }

    public static Bundle getArgsForUpdateRate(UpdateRate ur, boolean flag) {
        Bundle b = new Bundle();
        if (ur != null) {
            b.putString(BK_SELECTED_UPDATE_RATE, ur.name());
        }
        b.putBoolean(BK_FLAG, flag);
        return b;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (selectedUpdateRate != null) {
            bundle.putString(BK_SELECTED_UPDATE_RATE, selectedUpdateRate.name());
        }
        bundle.putBoolean(BK_FLAG, flag);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle b = bundle;
        if (b == null) {
            b = getArguments();
        }
        if (b != null) {
            if (b.containsKey(BK_SELECTED_UPDATE_RATE)) {
                setSelectedUpdateRate(UpdateRate.valueOf(b.getString(BK_SELECTED_UPDATE_RATE)));
            }
            flag = b.getBoolean(BK_FLAG);
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

    public static void showDialog(FragmentManager fm, @Nullable UpdateRate selectedUpdateRate, boolean flag) {
        final UpdateRatePickerDialogFragment f = new UpdateRatePickerDialogFragment();
        f.setArguments(getArgsForUpdateRate(selectedUpdateRate, flag));
        f.show(fm, UpdateRatePickerDialogFragment.FRAGMENT_TAG);
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
            holder.bind(updateRates[position]);
        }

        @Override
        public int getItemCount() {
            return updateRates.length;
        }

    }

    public boolean setSelectedUpdateRate(@Nullable UpdateRate updateRate) {
        boolean b = this.selectedUpdateRate != updateRate;
        this.selectedUpdateRate = updateRate;
        return b;
    }

    /**
     * View holder
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.radio)
        RadioButton rb;
        @BindView(R.id.tvUpdateRate)
        TextView tvUpdateRate;

        private View container;
        // data bean
        UpdateRate updateRate;

        public ViewHolder(View v) {
            super(v);
            // extract references to visual elements
            ButterKnife.bind(this, v);
            // set up the listener
            v.setOnClickListener(this);
            container = v;
        }

        @Override
        public void onClick(View view) {
            UpdateRate newUpdateRate = (UpdateRate) view.getTag();
            if (selectedUpdateRate != newUpdateRate) {
                // broadcast and dismiss
                InterfaceHub.getHandlerHub(IhCallback.class).onUpdateRatePicked(newUpdateRate, flag);
                dismiss();
            }
        }

        public void bind(UpdateRate updateRate) {
            // assign data bean
            this.updateRate = updateRate;
            // set up visual elements content
            this.tvUpdateRate.setText(updateRate.text);
            // toggle radio button
            rb.setChecked(selectedUpdateRate == this.updateRate);
            //
            container.setTag(updateRate);
        }
    }

    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onUpdateRatePicked(UpdateRate updateRate, boolean flag);

    }


}