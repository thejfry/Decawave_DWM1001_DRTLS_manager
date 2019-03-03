/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.TurnOnLocationServiceDialogFragment;
import com.google.common.base.Preconditions;

import java.util.Arrays;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * AndroidPermissionHelper implementation.
 */
public class AndroidPermissionHelperImpl implements AndroidPermissionHelper {
    private static final int REQUEST_CODE_ENABLE_BT = 100;
    private static final int REQUEST_CODE_ENABLE_LOCATION = 101;
    private static final int REQUEST_CODE_FINE_GRAINED_PERMISSION = 102;
    // log
    private static final ComponentLog log = new ComponentLog(AndroidPermissionHelper.class);
    // members
    private final BleAdapter bleAdapter;
    private final LocationManager locationManager;
    //
    private boolean inProgress;
    private Action0 currentGrantSuccessListener;
    private Action0 currentGrantFailListener;

    private IhOnActivityResultListener onActivityResultListener = new IhOnActivityResultListener() {
        @Override
        public void onActivityResult(MainActivity mainActivity, int requestCode, int resultCode, Intent data) {
            if (Constants.DEBUG) {
                log.d("onActivityResult: " + "mainActivity = [" + mainActivity + "], requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
                Preconditions.checkState(requestCode == REQUEST_CODE_ENABLE_BT || requestCode == REQUEST_CODE_ENABLE_LOCATION);
                Preconditions.checkState(inProgress);
            }
            // evaluate the result of request
            if (requestCode == REQUEST_CODE_ENABLE_BT) {
                if (resultCode == Activity.RESULT_OK) {
                    if (allSetUp()) {
                        // all set up
                        finalCallGrantListener(true);
                    } else {
                        nextStep(mainActivity);
                        // keep registered in IH
                    }
                } else {
                    // result code is failed
                    finalCallGrantListener(false);
                }
            } else if (requestCode == REQUEST_CODE_ENABLE_LOCATION) {
                // the resultCode is meaningless in this case, we have to check for the real status of location service
                handleLocationServiceEnablementResult();
            } else {
                // maybe we should silently ignore the onActivityResult instead
                throw new IllegalStateException("unsupported requestCode = " + requestCode);
            }
        }

        @Override
        public void onRequestPermissionsResult(MainActivity mainActivity, int requestCode, String[] permissions, int[] grantResults) {
            if (Constants.DEBUG) {
                log.d("onRequestPermissionsResult: " + "mainActivity = [" + mainActivity + "], requestCode = [" + requestCode + "], permissions = ["
                        + Arrays.toString(permissions) + "], grantResults = [" + Arrays.toString(grantResults) + "]");
                Preconditions.checkState(requestCode == REQUEST_CODE_FINE_GRAINED_PERMISSION);
                Preconditions.checkState(inProgress);
            }
            //
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                if (allSetUp()) {
                    finalCallGrantListener(true);
                } else {
                    nextStep(mainActivity);
                }
            } else {
                finalCallGrantListener(false);
            }
        }

    };

    @Inject
    AndroidPermissionHelperImpl(BleAdapter bleAdapter) {
        this.bleAdapter = bleAdapter;
        this.locationManager = (LocationManager) daApp.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean mkSureServicesEnabledAndPermissionsGranted(MainActivity mainActivity, Action0 grantSuccessListener) {
        return mkSureServicesEnabledAndPermissionsGranted(mainActivity, grantSuccessListener, () -> {
            // on reject
            ToastUtil.showToast("ArgoManager cannot work without granted permissions and location/BT services!");
        });
    }

    @Override
    public boolean mkSureServicesEnabledAndPermissionsGranted(MainActivity mainActivity, Action0 grantSuccessListener, Action0 grantFailListener) {
        if (Constants.DEBUG) {
            Preconditions.checkState(!inProgress);
            Preconditions.checkState(currentGrantSuccessListener == null);
            Preconditions.checkState(currentGrantFailListener == null);
        }
        if (!allSetUp()) {
            // we really need to request everything
            inProgress = true;
            currentGrantSuccessListener = grantSuccessListener;
            currentGrantFailListener = grantFailListener;
            InterfaceHub.registerHandler(onActivityResultListener);
            nextStep(mainActivity);
            return false;
        } else {
            // all is set up, call the grant success listener immediately
            grantSuccessListener.call();
            return true;
        }
    }

    private void finalCallGrantListener(boolean success) {
        if (Constants.DEBUG) {
            log.d("finalCallGrantListener: " + "success = [" + success + "]");
        }
        final Action0 l = success ? currentGrantSuccessListener : currentGrantFailListener;
        currentGrantFailListener = currentGrantSuccessListener = null;
        inProgress = false;
        InterfaceHub.unregisterHandler(onActivityResultListener);
        // finally notify the listener
        l.call();
    }

    private void handleLocationServiceEnablementResult() {
        if (!locationServiceEnabled()) {
            finalCallGrantListener(false);
        } else {
            // because the control flow in this case is a bit complicated (dialog -> nested activity -> back)
            // we will rather stop here and force the user to perform the action again instead of calling the listener/nextStep
            currentGrantFailListener = currentGrantSuccessListener = null;
            inProgress = false;
            InterfaceHub.unregisterHandler(onActivityResultListener);
        }
    }

    private void nextStep(MainActivity mainActivity) {
        if (Constants.DEBUG) {
            log.d("nextStep: " + "mainActivity = [" + mainActivity + "]");
        }
        // do the machinery
        if (!locationServiceEnabled()) {
            TurnOnLocationServiceDialogFragment.showDialog(mainActivity.getSupportFragmentManager());
        } else if (!bleAdapter.isEnabled()) {
            genericStartActivityForResult(mainActivity, REQUEST_CODE_ENABLE_BT, null);
        } else if (!locationPermissionGranted()) {
            mainActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_FINE_GRAINED_PERMISSION);
        }
    }

    @Override
    public void startActivityToEnableLocationService(MainActivity mainActivity) {
        genericStartActivityForResult(mainActivity, REQUEST_CODE_ENABLE_LOCATION, null);
    }

    @Override
    public boolean allSetUp() {
        return
                // BLE adapter must be enabled
                bleAdapter.isEnabled()
                // LOCATION service must be enabled
                && locationServiceEnabled()
                // permissions are granted
                && locationPermissionGranted();
    }

    private boolean locationPermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkMarshmallowPermissions();
    }

    private boolean locationServiceEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkMarshmallowPermissions() {
        // we are on M+
        // check that fine location permission is granted
        return daApp.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void genericStartActivityForResult(MainActivity mainActivity, int requestCode, InterfaceHubHandler handlerToRegister) {
        Intent i;
        switch (requestCode) {
            case REQUEST_CODE_ENABLE_BT:
                i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                break;
            case REQUEST_CODE_ENABLE_LOCATION:
                i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                break;
            default:
                throw new IllegalStateException("unsupported request code = " + requestCode);
        }
        // register our universal listener
        if (handlerToRegister != null) InterfaceHub.registerHandler(handlerToRegister);
        mainActivity.startActivityForResult(i, requestCode);
    }


    // private callback method
    public void onTurnOnLocationServiceDialogCancelled() {
        handleLocationServiceEnablementResult();
    }

}
