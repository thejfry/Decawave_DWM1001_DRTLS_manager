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

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;

/** Give name to the new/unknown network */
public class NewNetworkNameDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "rndf";

    private static final String BK_NETWORK_NAME = "networkName";
    private static final String BK_NETWORK_ID = "networkId";
    private static final String BK_NEW_NETWORK = "newNetwork";

    private String networkName;

    private Short networkId;

    private EditText etValue;

    public NewNetworkNameDialogFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (networkName != null) {
            bundle.putString(BK_NETWORK_NAME, networkName);
        }
        if (networkId != null) {
            bundle.putShort(BK_NETWORK_ID, networkId);
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
            networkName = bundle.getString(BK_NETWORK_NAME);
            networkId = bundle.containsKey(BK_NETWORK_ID) ? bundle.getShort(BK_NETWORK_ID) : null;
        }
        if (networkId == null && bundle != args) {
            networkId = args.getShort(BK_NETWORK_ID);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // custom view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View content = inflater.inflate(R.layout.dlg_rename_network, null);

        etValue = (EditText) content.findViewById(R.id.etValue);
        etValue.setText(networkName);
        builder.setView(content);
        if (args.containsKey(BK_NEW_NETWORK)) {
            builder.setTitle(R.string.discovery_new_network);
        }

        // positive button
        builder.setPositiveButton(R.string.save, (dialog, id) -> {
            InterfaceHub.getHandlerHub(IhCallback.class).onNewNetworkName(networkId, etValue.getText().toString());
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

        return dlg;
    }

    @Override
    public void onResume() {
        super.onResume();
        etValue.requestFocus();
    }

    public static void showDialog(FragmentManager fm, String prefilledValue, Short networkId, boolean newNetwork) {
        final NewNetworkNameDialogFragment f = new NewNetworkNameDialogFragment();
        Bundle b[] = { null };
        if (prefilledValue != null) {
            getOrCreateBundle(b).putString(BK_NETWORK_NAME, prefilledValue);
        }
        if (networkId != null) {
            getOrCreateBundle(b).putShort(BK_NETWORK_ID, networkId);
        }
        if (newNetwork) {
            getOrCreateBundle(b).putBoolean(BK_NEW_NETWORK, true);
        }
        if (b[0] != null) {
            f.setArguments(b[0]);
        }
        f.show(fm, NewNetworkNameDialogFragment.FRAGMENT_TAG);
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

        void onNewNetworkName(Short networkId, String networkName);

    }


}