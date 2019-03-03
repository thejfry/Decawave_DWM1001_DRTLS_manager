/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.BleGattCharacteristic;

/**
 * Argo project.
 */

class GattInteractionCallbackWrapper implements GattInteractionCallback {
    private final GattInteractionCallback delegate;

    GattInteractionCallbackWrapper(GattInteractionCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean stillInterested() {
        return delegate.stillInterested();
    }

    @Override
    public void onCharacteristicReadComplete(SynchronousBleGatt gatt) {
        delegate.onCharacteristicReadComplete(gatt);
    }

    @Override
    public void onDescriptorReadComplete(SynchronousBleGatt gatt) {
        delegate.onDescriptorReadComplete(gatt);
    }

    @Override
    public void onDescriptorWriteComplete(SynchronousBleGatt gatt) {
        delegate.onDescriptorWriteComplete(gatt);
    }

    @Override
    public void onCharacteristicWriteComplete(SynchronousBleGatt gatt) {
        delegate.onCharacteristicWriteComplete(gatt);
    }

    @Override
    public void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onFail(gatt, errorCode, failMessage);
    }

    @Override
    public void onCharacteristicReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onCharacteristicReadFailed(gatt, errorCode, failMessage);
    }

    @Override
    public void onCharacteristicWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onCharacteristicWriteFailed(gatt, errorCode, failMessage);
    }

    @Override
    public void onDescriptorReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onDescriptorReadFailed(gatt, errorCode, failMessage);
    }

    @Override
    public void onDescriptorWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onDescriptorWriteFailed(gatt, errorCode, failMessage);
    }

    @Override
    public void onCharacteristicChanged(SynchronousBleGatt gatt, BleGattCharacteristic characteristic, byte[] value) {
        delegate.onCharacteristicChanged(gatt, characteristic, value);
    }

    @Override
    public void onMtuChangeComplete(SynchronousBleGatt syncGatt) {
        delegate.onMtuChangeComplete(syncGatt);
    }

    @Override
    public void onMtuChangeFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
        delegate.onMtuChangeFailed(gatt, errorCode, failMessage);
    }
}
