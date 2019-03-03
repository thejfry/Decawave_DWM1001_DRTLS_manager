/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import eu.kryl.android.common.fsm.impl.FiniteStateMachineImpl;
import eu.kryl.android.common.log.LogLevel;

/**
 * FSM representing overall BLE discovery state.
 *
 * @see GattInteractionFsmImpl
 */

class DeviceDiscoveryFsm extends FiniteStateMachineImpl<BleDeviceDiscoveryState> {

    DeviceDiscoveryFsm() {
        super("DeviceDiscoveryFsm", BleDeviceDiscoveryState.class);
        getLog().setLogLevel(LogLevel.INFO);
    }


}
