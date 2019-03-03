/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.ble.BleDevice;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens for discovery state changes.
 */
public interface IhPositionObserveListener extends InterfaceHubHandler {

    void onPositionObserved(BleDevice bleDevice, Position position);

}
