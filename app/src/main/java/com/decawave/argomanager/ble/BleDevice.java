/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

/**
 * BLE device.
 */
public interface BleDevice {

    String getAddress();

    /**
     * Initiates a connection to this device.
     * @param gattCallback used to delegate results of asynchronous actions
     * @return ble GATT abstraction, in rare situations might also return null (broken Android API)
     */
    BleGatt connect(BleGattCallback gattCallback);

}
