/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.BleDevice;

import org.jetbrains.annotations.NotNull;

import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Scanning API is built around BLE adapter and it's scanning capabilities.
 * It periodically turns on and off
 */
public interface PeriodicBleScanner {

    /**
     * Starts periodic scanning.
     *  @param scanSemaphore semaphore determining when it is safe to start scanning
     * @param callback notification about asynchronous events
     */
    void startPeriodicScan(@NotNull Action1<Action0> scanSemaphore,
                           Callback callback);

    /**
     * stops periodic scanning
     */
    void stopPeriodicScan();

    /**
     * @return state of this API instance
     */
    boolean isStarted();

    /**
     * @return true if the underlying BLE adapter is currently switched on/actively scanning
     */
    boolean isBleScanning();

    /**
     * Callback used to notify about asynchronous scan events.
     */
    interface Callback {

        /**
         * When periodic scan is started.
         */
        void onStarted();

        /**
         * When a new device is found.
         */
        void onServiceDataScan(final BleDevice bluetoothDevice, int rssi, byte[] serviceData);

        /**
         * When scan failed.
         * After the scan has failed, we also call {@link #onStopped()}.
         */
        void onScanFailed();

        /**
         * When scan starts.
         */
        void onScanStarted();

        /**
         * When scan stops.
         */
        void onScanStopped();

        /**
         * When periodic scan is stopped.
         * Called also in case of failure - after {@link #onScanFailed()}
         */
        void onStopped();

    }

}
