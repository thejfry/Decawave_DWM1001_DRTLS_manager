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
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.uiutil.DecimalDigitsInputFilter;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;

/** Set up uniform z-axis value - autopositioning */
public class ZaxisValueDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "zavdf";

    private static final String BK_ZAXIS_VALUE = "zAxisValue";

    public static final DecimalDigitsInputFilter INPUT_FILTER_DECIMAL_5_2 = new DecimalDigitsInputFilter(5, 2);

    public static final InputFilter[] POSITION_INPUT_FILTERS = new InputFilter[]{INPUT_FILTER_DECIMAL_5_2};

    private String zAxisValue;

    private EditText etValue;

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (zAxisValue != null) {
            bundle.putString(BK_ZAXIS_VALUE, zAxisValue);
        }
        super.onSaveInstanceState(bundle);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle args = getArguments();
        if (bundle == null) {
            bundle = args;
        }
        // restore dialog state
        if (bundle != null) {
            zAxisValue = bundle.getString(BK_ZAXIS_VALUE);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // custom view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View content = inflater.inflate(R.layout.dlg_zaxis, null);

        etValue = (EditText) content.findViewById(R.id.etValue);
        if (zAxisValue != null) etValue.setText(zAxisValue);
        builder.setView(content);

        // positive button
        builder.setPositiveButton(R.string.btn_ok, (dialog, id) -> {
            InterfaceHub.getHandlerHub(IhCallback.class).onNewZAxisValue(etValue.getText().toString());
            dialog.dismiss();
        });

        final AlertDialog dlg = builder.create();

        etValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                //noinspection ConstantConditions
                dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });

        dlg.getPositiveButton().setEnabled(false);
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
        // allow only 5.2 digits
        etValue.setFilters(POSITION_INPUT_FILTERS);

        return dlg;
    }

    @Override
    public void onResume() {
        super.onResume();
        etValue.requestFocus();
    }

    public static void showDialog(FragmentManager fm,
                                  String prefilledValue) {
        final ZaxisValueDialogFragment f = new ZaxisValueDialogFragment();
        Bundle b[] = { null };
        if (prefilledValue != null) {
            getOrCreateBundle(b).putString(BK_ZAXIS_VALUE, prefilledValue);
        }
        if (b[0] != null) {
            f.setArguments(b[0]);
        }
        f.show(fm, ZaxisValueDialogFragment.FRAGMENT_TAG);
    }

    private static Bundle getOrCreateBundle(Bundle[] b) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(b);
            Preconditions.checkState(b.length == 1);
        }
        return b[0] == null ? b[0] = new Bundle() : b[0];
    }


    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onNewZAxisValue(String zAxisValue);

    }


}