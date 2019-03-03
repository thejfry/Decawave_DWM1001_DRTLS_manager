/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.support.annotation.NonNull;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.error.DeviceErrors;
import com.decawave.argomanager.error.ErrorCodeInterpreter;
import com.decawave.argomanager.error.ErrorDetail;
import com.decawave.argomanager.error.IhErrorManagerListener;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.ApplicationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Error manager default and only implementation.
 */
public class ErrorManagerImpl implements ErrorManager {
    // logging
    private static final ComponentLog log = new ComponentLog(ErrorManager.class);
    // dependencies
    private final BleConnectionApi bleConnectionApi;
    private final AppPreferenceAccessor appPreferenceAccessor;
    // members
    private Map<String, DeviceErrors> errorsByDevice;
    // cache
    private boolean anyMajorUnreadErrorCache;
    private boolean anyHardUnreadErrorCache;

    @Inject
    ErrorManagerImpl(BleConnectionApi bleConnectionApi,
                     AppPreferenceAccessor appPreferenceAccessor) {
        this.bleConnectionApi = bleConnectionApi;
        this.appPreferenceAccessor = appPreferenceAccessor;
        this.errorsByDevice = new HashMap<>();
    }

    @Override
    public List<DeviceErrors> getErrors() {
        List<DeviceErrors> errors = new ArrayList<>(errorsByDevice.values());
        if (appPreferenceAccessor.getApplicationMode() == ApplicationMode.SIMPLE) {
            // filter out some errors, keep only RED ones
            List<DeviceErrors> newErrs = new LinkedList<>();
            for (DeviceErrors oldDevErr : errors) {
                DeviceErrors newDevErr = null;
                List<ErrorDetail> oldErrs = oldDevErr.getErrors();
                for (ErrorDetail oldErr : oldErrs) {
                    ErrorCodeInterpreter.Properties props = oldErr.getProperties();
                    if (!props.warningOnly && !props.isSoft()) {
                        if (newDevErr == null) {
                            // create a new dev error instance
                            newDevErr = new DeviceErrors(oldDevErr.deviceBleAddress, oldErr);
                        } else {
                            // there already is created a newDevErr instance
                            newDevErr.addError(oldErr);
                        }
                    }
                }
                if (newDevErr != null) {
                    newErrs.add(newDevErr);
                }
            }
            errors = newErrs;
        }
        return errors;
    }

    @Override
    public void reportError(String deviceBleAddress, Throwable exception, String message, int errorCode) {
        reportError(deviceBleAddress, new ErrorDetail(exception, message, errorCode));
    }

    private void reportError(String deviceBleAddress, ErrorDetail errorDetail) {
        if (Constants.DEBUG) {
            log.w("reported ERROR " + errorDetail.errorCode + " for " + deviceBleAddress + ": " + errorDetail.message, errorDetail.exception);
        }
        if (ErrorCodeInterpreter.interpret(errorDetail.errorCode).isSoft()) {
            log.d("skipping " + errorDetail + ", it's soft");
            return;
        }
        DeviceErrors deviceErrors = getDeviceErrors(deviceBleAddress);
        deviceErrors.addError(errorDetail);
        if (!anyMajorUnreadErrorCache && deviceErrors.anyUnreadMajorError()) {
            anyMajorUnreadErrorCache = true;
        }
        if (!anyHardUnreadErrorCache && deviceErrors.anyUnreadHardError()) {
            anyHardUnreadErrorCache = true;
        }
        // let the connection api know
        bleConnectionApi.onSessionError(deviceBleAddress, errorDetail.errorCode);
        // notify
        InterfaceHub.getHandlerHub(IhErrorManagerListener.class).onErrorDetailAdded(deviceBleAddress, errorDetail);
    }

    @NonNull
    private DeviceErrors getDeviceErrors(String deviceBleAddress) {
        DeviceErrors deviceErrors = errorsByDevice.get(deviceBleAddress);
        if (deviceErrors == null) {
            deviceErrors = new DeviceErrors(deviceBleAddress);
            errorsByDevice.put(deviceBleAddress, deviceErrors);
        }
        return deviceErrors;
    }

    @Override
    public void removeDeviceErrors(String deviceBleAddress) {
        DeviceErrors r = errorsByDevice.remove(deviceBleAddress);
        if (r != null) {
            // notify
            InterfaceHub.getHandlerHub(IhErrorManagerListener.class).onErrorRemoved(deviceBleAddress);
        }
    }

    @Override
    public boolean anyUnreadError() {
        boolean onlyHardErrors = appPreferenceAccessor.getApplicationMode() == ApplicationMode.SIMPLE;
        if (onlyHardErrors) {
            return anyHardUnreadErrorCache;
        } else {
            return anyMajorUnreadErrorCache;
        }
    }

    @Override
    public void markErrorsAsRead() {
        anyMajorUnreadErrorCache = false;
        anyHardUnreadErrorCache = false;
        // mark all errors as read
        for (DeviceErrors deviceErrors : errorsByDevice.values()) {
            //noinspection Convert2streamapi
            for (ErrorDetail ed : deviceErrors.getErrors()) {
                ed.markAsRead();
            }
        }
    }

    @Override
    public void clearErrors() {
        errorsByDevice.clear();
        anyMajorUnreadErrorCache = false;
        anyHardUnreadErrorCache = false;
        // notify
        InterfaceHub.getHandlerHub(IhErrorManagerListener.class).onErrorsClear();
    }

}
