/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import java.util.List;
import java.util.UUID;

/**
 * BLE GATT interaction abstraction.
 *
 * Instance can be obtained via {@link BleDevice#connect(BleGattCallback)}.
 *
 * Results of requests are received via {@link BleGattCallback}.
 */
public interface BleGatt {

    boolean discoverServices();

    BleGattService getService(UUID serviceUuid);

    List<BleGattService> getServices();

    boolean readCharacteristic(BleGattCharacteristic characteristic);

    boolean writeCharacteristic(BleGattCharacteristic characteristic);

    boolean setCharacteristicNotification(BleGattCharacteristic characteristic, boolean enable);

    boolean readDescriptor(BleGattDescriptor descriptor);

    boolean writeDescriptor(BleGattDescriptor descriptor);

    BleDevice getDevice();

    void disconnect();

    /**
     * Caller is notified back via {@link BleGattCallback#onMtuChanged(BleGatt, int, int)}.
     */
    boolean requestMtu(int mtu);

    boolean requestConnectionSpeed(ConnectionSpeed connectionSpeed);

    // do not forget to close after disconnected in order to release resources
    void close();

}
