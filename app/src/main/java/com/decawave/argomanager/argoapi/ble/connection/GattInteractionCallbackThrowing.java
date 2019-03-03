/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.argoapi.ble.GattInteractionCallback;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;

/**
 *
 */
class GattInteractionCallbackThrowing implements GattInteractionCallback {


    @Override
    public boolean stillInterested() {
        return true;
    }

    @Override
    public void onCharacteristicReadComplete(SynchronousBleGatt gatt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDescriptorReadComplete(SynchronousBleGatt gatt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDescriptorWriteComplete(SynchronousBleGatt gatt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCharacteristicWriteComplete(SynchronousBleGatt gatt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMtuChangeComplete(SynchronousBleGatt syncGatt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCharacteristicReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCharacteristicWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDescriptorReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDescriptorWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMtuChangeFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCharacteristicChanged(SynchronousBleGatt gatt, BleGattCharacteristic characteristic, byte[] value) {
        throw new UnsupportedOperationException();
    }

}
