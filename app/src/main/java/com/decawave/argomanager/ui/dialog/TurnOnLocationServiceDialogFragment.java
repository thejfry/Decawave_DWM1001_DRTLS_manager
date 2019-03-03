/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ioc.IocContext;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.AndroidPermissionHelperImpl;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import eu.kryl.android.appcompat.dialogs.AlertDialog;

/** Give name to the new/unknown network */
public class TurnOnLocationServiceDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "tolsdf";

    @Inject
    AndroidPermissionHelper permissionHelper;

    public TurnOnLocationServiceDialogFragment() {
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        // inject dependencies first
        IocContext.daCtx.inject(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // custom view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View content = inflater.inflate(R.layout.dlg_enable_location_service, null);

        builder.setView(content);

        // positive button
        builder.setPositiveButton(R.string.btn_settings, (dialog, id) -> {
            // One should have registered IhOnActivityResultListener first in order to be able to get the
            // result from activity launch.
            permissionHelper.startActivityToEnableLocationService((MainActivity) getActivity());
            dialog.dismiss();
        });

        final AlertDialog dlg = builder.create();

        dlg.getPositiveButton().setEnabled(true);
        return dlg;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((AndroidPermissionHelperImpl) permissionHelper).onTurnOnLocationServiceDialogCancelled();
    }

    /**
     * One should have registered IhOnActivityResultListener first in order to be able to get the
     * result from activity launch.
     */
    public static void showDialog(FragmentManager fm) {
        final TurnOnLocationServiceDialogFragment f = new TurnOnLocationServiceDialogFragment();
        f.show(fm, TurnOnLocationServiceDialogFragment.FRAGMENT_TAG);
    }

}