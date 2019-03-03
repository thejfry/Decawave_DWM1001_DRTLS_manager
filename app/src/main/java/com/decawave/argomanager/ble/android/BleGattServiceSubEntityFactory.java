/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattService;

/**
 * Helper interface for creating instances of a BLE characteristic.
 */

interface BleGattServiceSubEntityFactory {

    BleGattCharacteristic newCharacteristic(BluetoothGattCharacteristic characteristic);

    BleGattService newIncludedService(BluetoothGattService includedService);

}
