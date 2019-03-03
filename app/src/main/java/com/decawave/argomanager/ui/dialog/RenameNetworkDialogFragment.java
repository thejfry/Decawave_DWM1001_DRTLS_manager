/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.NetworkNodeManager;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import eu.kryl.android.appcompat.dialogs.AlertDialog;

import static com.decawave.argomanager.ioc.IocContext.daCtx;

/** List preference dialog fragment */
public class RenameNetworkDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "rndf";

    private static final String BK_VALUE = "value";

    private String value;

    private EditText etValue;
    
    @Inject
    NetworkNodeManager networkNodeManager;

    public RenameNetworkDialogFragment() {
        daCtx.inject(this);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(BK_VALUE, value);
        super.onSaveInstanceState(bundle);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        if (bundle == null) {
            bundle = getArguments();
        }
        // restore dialog state
        if (bundle != null) {
            if (bundle.containsKey(BK_VALUE)) {
                value = bundle.getString(BK_VALUE);
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // custom view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View content = inflater.inflate(R.layout.dlg_rename_network, null);

        etValue = (EditText) content.findViewById(R.id.etValue);
        etValue.setText(value);
        builder.setView(content);

        // positive button
        builder.setPositiveButton(R.string.btn_rename, (dialog, id) -> {
            //noinspection ConstantConditions
            networkNodeManager.getActiveNetwork().setNetworkName(etValue.getText().toString());
            dialog.dismiss();
        });

        final AlertDialog dlg = builder.create();

        etValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                //noinspection ConstantConditions
                dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });

        dlg.getPositiveButton().setEnabled(!value.isEmpty());
        etValue.addTextChangedListener(new TextWatcher() {
                                           @Override
                                           public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                                           }

                                           @Override
                                           public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                                           }

                                           @Override
                                           public void afterTextChanged(Editable editable) {
                                               dlg.getPositiveButton().setEnabled(!editable.toString().isEmpty());
                                           }
                                       });

        return dlg;
    }

    @Override
    public void onResume() {
        super.onResume();
        etValue.requestFocus();
    }

    public static void showDialog(FragmentManager fm, String prefilledValue) {
        final RenameNetworkDialogFragment f = new RenameNetworkDialogFragment();
        if (prefilledValue != null) {
            Bundle b = new Bundle();
            b.putString(BK_VALUE, prefilledValue);
            f.setArguments(b);
        }
        f.show(fm, RenameNetworkDialogFragment.FRAGMENT_TAG);
    }


}