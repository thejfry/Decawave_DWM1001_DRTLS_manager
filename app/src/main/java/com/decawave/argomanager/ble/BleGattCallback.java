/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import com.decawave.argo.api.ConnectionState;

/**
 * BLE GATT callback - requests are submitted via {@link BleGatt},
 * responses are received via this callback.
 */
public interface BleGattCallback {

    void onConnectionStateChange(BleGatt gatt, int status, ConnectionState newConnectionState);

    void onServicesDiscovered(BleGatt gatt, int status);

    void onCharacteristicRead(BleGatt gatt, BleGattCharacteristic characteristic, byte[] value, int status);

    void onCharacteristicWritten(BleGatt gatt, BleGattCharacteristic characteristic, int status);

    void onDescriptorRead(BleGatt gatt, BleGattDescriptor descriptor, byte[] value, int status);

    void onDescriptorWritten(BleGatt gatt, BleGattDescriptor descriptor, int status);

    void onCharacteristicChanged(BleGatt bleGatt, BleGattCharacteristic characteristic, byte[] value);

    /**
     * Called after {@link com.decawave.argomanager.ble.BleGatt#requestMtu(int)} has been processed.
     *
     * @param gatt identification of the gatt
     * @param mtu the new MTU size
     * @param status GATT_SUCCESS if the
     */
    void onMtuChanged(BleGatt gatt, int mtu, int status);

}
