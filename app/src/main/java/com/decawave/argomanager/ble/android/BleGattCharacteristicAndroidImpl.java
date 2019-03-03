/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothGattCharacteristic;

import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.decawave.argomanager.ble.BleGattService;
import com.decawave.argomanager.ble.WriteType;
import com.decawave.argomanager.util.gatt.GattEncoder;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import eu.kryl.android.common.Constants;

/**
 * Ble characteristic implemented via wrapping android implementation.
 */
class BleGattCharacteristicAndroidImpl implements BleGattCharacteristic {
    final BluetoothGattCharacteristic delegate;
    private final BleGatt scope;

    BleGattCharacteristicAndroidImpl(@NotNull BluetoothGattCharacteristic delegate, @NotNull BleGatt scope) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(delegate);
        }
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public UUID getUuid() {
        return delegate.getUuid();
    }

    @Override
    public byte[] getValue() {
        return delegate.getValue();
    }

    public boolean setStringValue(String value) {
        return delegate.setValue(value);
    }

    @Override
    public boolean setByteValue(byte[] value) {
        return delegate.setValue(value);
    }

    @Override
    public boolean setIntValue(Integer value) {
        Preconditions.checkState(value >= 0, "negative values not supported!");
        return delegate.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
    }

    @Override
    public boolean setShortValue(Short value) {
        return delegate.setValue(value, BluetoothGattCharacteristic.FORMAT_SINT16, 0);
    }

    @Override
    public boolean setBooleanValue(Boolean value) {
        return delegate.setValue(GattEncoder.encodeBoolean(value));
    }

    @Override
    public boolean setPositionValue(Position value) {
        return delegate.setValue(GattEncoder.encodePosition(value));
    }

    @Override
    public boolean valueLoaded() {
        return delegate.getValue() != null;
    }

    @Override
    public BleGattDescriptor getDescriptor(UUID descriptor) {
        return BleObjectCachingFactory.newDescriptor(delegate.getDescriptor(descriptor), scope);
    }

    @Override
    public boolean emptyValue() {
        byte[] v = delegate.getValue();
        return v == null || v.length == 0;
    }

    @Override
    public boolean setUuidValue(UUID uuid) {
        return delegate.setValue(GattEncoder.encodeUuid(uuid));
    }

    @Override
    public String getStringValue() {
        return delegate.getStringValue(0);
    }

    @Override
    public Integer getIntValue() {
        if (emptyValue()) {
            return null;
        }
        return delegate.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
    }

    @Override
    public BleGattService getService() {
        return BleObjectCachingFactory.newService(delegate.getService(), scope);
    }

    @Override
    public void setWriteType(WriteType writeType) {
        delegate.setWriteType(writeType == WriteType.NO_RESPONSE ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleGattCharacteristicAndroidImpl that = (BleGattCharacteristicAndroidImpl) o;

        return delegate.equals(that.delegate);

    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
