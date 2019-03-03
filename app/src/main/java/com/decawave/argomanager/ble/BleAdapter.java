/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import java.util.UUID;

/**
 * Ble adapter.
 *
 * TODO: decide whether this needs to be Argo-independent.
 */
public interface BleAdapter {

    BleDevice getRemoteDevice(String bleAddress);

    boolean isEnabled();

    void startServiceDataScan(UUID serviceUuid, ScanCallback callback);

    void stopServiceDataScan();

    boolean isScanning();

    interface ScanCallback {

        void onServiceDataScan(final BleDevice bluetoothDevice, int rssi, byte[] serviceData);

        void onScanFailed();

    }

}
