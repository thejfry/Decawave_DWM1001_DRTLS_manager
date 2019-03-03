/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.PositionObservationManager;

import javax.inject.Inject;

import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 */

public class PositionObservationManagerImpl implements PositionObservationManager {
    private static final ComponentLog log = new ComponentLog(PositionObservationManager.class);
    // dependencies
    private final Runnable stopPositionObservationRunnable = this::stopPositionObservationIfRunning;
    private final LocationDataObserver locationDataObserver;

    @Inject
    PositionObservationManagerImpl(LocationDataObserver locationDataObserver) {
        this.locationDataObserver = locationDataObserver;
    }

    public void schedulePositionObservationStop(long duration) {
        uiHandler.postDelayed(stopPositionObservationRunnable, duration);
    }

    @Override
    public void startPositionObservation() {
        // simly delegate
        locationDataObserver.startObserve();
    }

    @Override
    public boolean isObservingPosition() {
        return locationDataObserver.isObserving();
    }

    public void cancelScheduledPositionObservationStop() {
        uiHandler.removeCallbacks(stopPositionObservationRunnable);
    }

    private void stopPositionObservationIfRunning() {
        if (Constants.DEBUG) log.d("stopPositionObservationIfRunning()");
        if (locationDataObserver != null && locationDataObserver.isObserving()) {
            locationDataObserver.stopObserve();
        }
    }

}
