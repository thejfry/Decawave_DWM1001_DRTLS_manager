/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothGattDescriptor;

import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.google.common.base.Preconditions;

import java.util.UUID;

import eu.kryl.android.common.Constants;

/**
 * Argo project.
 */

class BleGattDescriptorAndroidImpl implements BleGattDescriptor {
    final BluetoothGattDescriptor delegate;
    private final BleGatt scope;

    BleGattDescriptorAndroidImpl(BluetoothGattDescriptor delegate, BleGatt scope) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(delegate);
            Preconditions.checkNotNull(scope);
        }
        this.delegate = delegate;
        this.scope = scope;

    }

    @Override
    public UUID getUuid() {
        return delegate.getUuid();
    }

    @Override
    public BleGattCharacteristic getCharacteristic() {
        return BleObjectCachingFactory.newCharacteristic(delegate.getCharacteristic(), scope);
    }

    @Override
    public boolean setValue(byte[] value) {
        return delegate.setValue(value);
    }

    @Override
    public byte[] getValue() {
        return delegate.getValue();
    }

}
