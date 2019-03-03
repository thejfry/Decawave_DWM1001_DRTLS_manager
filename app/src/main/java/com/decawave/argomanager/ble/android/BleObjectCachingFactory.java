/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.decawave.argomanager.ble.BleGattService;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import eu.kryl.android.common.Constants;

/**
 *
 */
public class BleObjectCachingFactory {
    // ble device has global tag
    private static Map<BluetoothDevice, BleDevice> bleDeviceMap = new HashMap<>();
    // characteristic and service have BLE GATT tag
    private static Map<BleGatt, BleGattCache> bleGattCacheScope = new HashMap<>();

    private static class BleGattCache {
        private Map<BluetoothGattCharacteristic, BleGattCharacteristic> bleGattCharacteristicMap = new HashMap<>();
        private Map<BluetoothGattDescriptor, BleGattDescriptor> bleGattDescriptorMap = new HashMap<>();
        private Map<BluetoothGattService, BleGattService> bleServiceMap = new HashMap<>();
        private Map<BluetoothGattService, BleGattService> bleIncludedServiceMap = new HashMap<>();
    }

    static BleDevice newDevice(@NotNull BluetoothDevice androidBleDevice) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(androidBleDevice);
        }
        BleDevice bleDevice = bleDeviceMap.get(androidBleDevice);
        if (bleDevice == null) {
            bleDeviceMap.put(androidBleDevice, bleDevice = new BleDeviceAndroidImpl(androidBleDevice));
        }
        return bleDevice;
    }

    static void forgetBleGatt(BleGatt bleGatt) {
        bleGattCacheScope.remove(bleGatt);
    }

    static BleGattService newService(BluetoothGattService service, @NotNull final BleGatt scope) {
        BleGattCache bleGattCache = getOrCreateBleGattCache(scope);
        BleGattService s = bleGattCache.bleServiceMap.get(service);
        if (s == null) {
            final BleGattServiceSubEntityFactory[] factory = {null};
            factory[0] = new BleGattServiceSubEntityFactory() {
                @Override
                public BleGattCharacteristic newCharacteristic(BluetoothGattCharacteristic characteristic) {
                    return BleObjectCachingFactory.newCharacteristic(characteristic, scope);
                }

                @Override
                public BleGattService newIncludedService(BluetoothGattService includedService) {
                    return BleObjectCachingFactory.newIncludedService(includedService, scope, factory[0]);
                }
            };
            bleGattCache.bleServiceMap.put(service, s = new BleGattServiceAndroidImpl(service, factory[0]));
        }
        return s;
    }

    public static BleGattDescriptor newDescriptor(@NotNull BluetoothGattDescriptor descriptor, @NotNull BleGatt scope) {
        Preconditions.checkNotNull(descriptor);
        BleGattCache bleGattCache = getOrCreateBleGattCache(scope);
        BleGattDescriptor desc = bleGattCache.bleGattDescriptorMap.get(descriptor);
        if (desc == null) {
            bleGattCache.bleGattDescriptorMap.put(descriptor, desc = new BleGattDescriptorAndroidImpl(descriptor, scope));
        }
        return desc;
    }

    public static BleGattCharacteristic newCharacteristic(@NotNull BluetoothGattCharacteristic characteristic, @NotNull BleGatt scope) {
        Preconditions.checkNotNull(characteristic);
        BleGattCache bleGattCache = getOrCreateBleGattCache(scope);
        BleGattCharacteristic ch = bleGattCache.bleGattCharacteristicMap.get(characteristic);
        if (ch == null) {
            bleGattCache.bleGattCharacteristicMap.put(characteristic, ch = new BleGattCharacteristicAndroidImpl(characteristic, scope));
        }
        return ch;
    }

    private static BleGattService newIncludedService(@NotNull BluetoothGattService includedService, @NotNull BleGatt scope, BleGattServiceSubEntityFactory subEntityFactory) {
        Preconditions.checkNotNull(includedService);
        BleGattCache bleGattCache = getOrCreateBleGattCache(scope);
        BleGattService s = bleGattCache.bleIncludedServiceMap.get(includedService);
        if (s == null) {
            bleGattCache.bleIncludedServiceMap.put(includedService, s = new BleGattServiceAndroidImpl(includedService, subEntityFactory));
        }
        return s;
    }

    @NonNull
    private static BleGattCache getOrCreateBleGattCache(@NotNull BleGatt bleGatt) {
        BleGattCache bleGattCache = bleGattCacheScope.get(bleGatt);
        if (bleGattCache == null) {
            bleGattCache = new BleGattCache();
            bleGattCacheScope.put(bleGatt, bleGattCache);
        }
        return bleGattCache;
    }
}
