/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ioc;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ble.android.BleAdapterLollipopAndroidImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Argo project.
 */
@Module
class ArgoDependencyProvider {

    @Singleton
    @Provides
    BleAdapter provideAdapter() {
        final BluetoothManager bluetoothManager = (BluetoothManager) ArgoApp.daApp.getSystemService(Context.BLUETOOTH_SERVICE);
        return new BleAdapterLollipopAndroidImpl(bluetoothManager.getAdapter());
    }

}
