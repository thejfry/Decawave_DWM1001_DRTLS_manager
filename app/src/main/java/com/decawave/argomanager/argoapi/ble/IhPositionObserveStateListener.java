/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens for discovery state changes.
 */
public interface IhPositionObserveStateListener extends InterfaceHubHandler {

    void beforeObserveStarted();

    void afterObserveStarted();

    void afterObserveStopped();

}
