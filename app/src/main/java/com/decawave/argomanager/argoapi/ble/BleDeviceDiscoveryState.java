/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

/**
 * Internal discovery state.
 * From outer POV, PAUSED is the same as discovering.
 */
enum BleDeviceDiscoveryState {
    DISCOVERING,
    STOPPING,
    STOPPED
}
