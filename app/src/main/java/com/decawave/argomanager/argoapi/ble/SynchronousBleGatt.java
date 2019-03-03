/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.BleGattCharacteristic;

import java.util.List;
import java.util.UUID;

/**
 * Ble GATT with only synchronous methods exposed.
 *
 * If you would like to invoke an asynchronous method, use {@link GattInteractionFsm} instead.
 *
 * @see com.decawave.argomanager.ble.BleGatt
 */
public interface SynchronousBleGatt {

    BleGattServiceRdonly getService(UUID serviceUuid);

    String getDeviceAddress();

    List<BleGattServiceRdonly> getServices();

    boolean setCharacteristicNotification(BleGattCharacteristic characteristic, boolean enable);

}
