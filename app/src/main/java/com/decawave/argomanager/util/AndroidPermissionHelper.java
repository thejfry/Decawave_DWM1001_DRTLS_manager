/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import com.decawave.argomanager.ui.MainActivity;

import rx.functions.Action0;

/**
 * Argo project.
 */
public interface AndroidPermissionHelper {

    /**
     * If services are not enabled and permissions are not granted, this returns false
     * and starts the grant process asynchronously.
     *
     * One can listen for the result of the grant process via the passed grantListener.
     *
     * @param mainActivity used to fire grant requests
     * @param grantSuccessListener is invoked after the grant/enable was accepted
     * @param grantFailListener is invoked after the grant/enable was rejected
     *
     * @return true if all is set up, false otherwise - means that asynchronous grant process has just started
     */
    boolean mkSureServicesEnabledAndPermissionsGranted(MainActivity mainActivity, Action0 grantSuccessListener, Action0 grantFailListener);

    boolean mkSureServicesEnabledAndPermissionsGranted(MainActivity mainActivity, Action0 grantSuccessListener);

    void startActivityToEnableLocationService(MainActivity mainActivity);

    boolean allSetUp();

}
