/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.argoapi.ble.BtGattCallbackAdapter;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCallback;

import org.jetbrains.annotations.NotNull;

/**
 * Ble device implemented via wrapping android implementation.
 */
class BleDeviceAndroidImpl implements BleDevice {
    @NotNull
    private final BluetoothDevice delegate;

    BleDeviceAndroidImpl(@NotNull BluetoothDevice delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getAddress() {
        return delegate.getAddress();
    }

    @Override
    public BleGatt connect(final BleGattCallback gattCallback) {
        final BleGatt[] bleGatt = { null };
        BluetoothGatt _bleGatt = this.delegate.connectGatt(ArgoApp.daApp, false,
                // adapt the callback methods invocations
                new BtGattCallbackAdapter(gattCallback, () -> bleGatt[0]));
        if (_bleGatt != null) {
            bleGatt[0] = new BleGattAndroidImpl(_bleGatt);
        }
        return bleGatt[0];
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleDeviceAndroidImpl that = (BleDeviceAndroidImpl) o;

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
