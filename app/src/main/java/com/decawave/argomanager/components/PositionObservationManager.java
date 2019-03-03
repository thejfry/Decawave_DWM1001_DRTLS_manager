/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

/**
 * Argo project.
 */

public interface PositionObservationManager {

    void startPositionObservation();

    boolean isObservingPosition();

    void schedulePositionObservationStop(long duration);

    void cancelScheduledPositionObservationStop();
}
