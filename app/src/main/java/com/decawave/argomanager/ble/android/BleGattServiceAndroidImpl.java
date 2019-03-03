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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Ble GATT service implemented via wrapping android implementation.
 */
class BleGattServiceAndroidImpl implements BleGattService {
    private final BluetoothGattService delegate;
    private final BleGattServiceSubEntityFactory factory;

    BleGattServiceAndroidImpl(BluetoothGattService delegate, BleGattServiceSubEntityFactory factory) {
        this.delegate = delegate;
        this.factory = factory;
    }

    @Override
    public UUID getUuid() {
        return delegate.getUuid();
    }

    @Override
    public BleGattCharacteristic getCharacteristic(UUID characteristicUuid) {
        BluetoothGattCharacteristic _ch = delegate.getCharacteristic(characteristicUuid);
        if (_ch == null) {
            return null;
        } // else:
        return factory.newCharacteristic(_ch);
    }

    @Override
    public List<BleGattService> getIncludedServices() {
        List<BleGattService> services = new LinkedList<>();
        for (BluetoothGattService service : delegate.getIncludedServices()) {
            services.add(factory.newIncludedService(service));
        }
        return services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleGattServiceAndroidImpl that = (BleGattServiceAndroidImpl) o;

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
