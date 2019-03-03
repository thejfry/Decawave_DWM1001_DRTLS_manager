/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ble.BleDevice;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.Lazy;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;
import rx.functions.Action1;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Default implementation of PeriodicBleScanner.
 */
public class PeriodicBleScannerImpl implements PeriodicBleScanner {
    private static final ComponentLog log = new ComponentLog(PeriodicBleScannerImpl.class);
    // dependencies
    private final Lazy<BleAdapter> bleAdapter;
    private final ScannerTimeAdaptor scannerTimeAdaptor;
    // state
    private Action1<Action0> semaphore;
    private Callback callback;
    // because we do not have asynchronous notification about successfully started scan
    private boolean bleScanning;
    private Object tag;

    @Inject
    PeriodicBleScannerImpl(Lazy<BleAdapter> bleAdapterSupplier) {
        this.bleAdapter = bleAdapterSupplier;
        this.scannerTimeAdaptor = new ScannerTimeAdaptor(BleConstants.BLE_DISCOVERY_SCAN_MIN_PERIOD_MS,
                BleConstants.BLE_DISCOVERY_NOSCAN_MIN_PERIOD_MS);
    }


    /**
     * @param semaphore semaphore which signals that it is OK now to start BLE scan
     * @param callback notification about asynchronous events
     */
    @Override
    public void startPeriodicScan(@NotNull Action1<Action0> semaphore,
                                  @NotNull Callback callback) {
        if (Constants.DEBUG) {
            log.d("startPeriodicScan()");
            Preconditions.checkState(this.semaphore == null);
            Preconditions.checkState(this.callback == null);
            Preconditions.checkState(!this.bleScanning);
        }
        // set up
        this.semaphore = semaphore;
        this.callback = callback;
        this.tag = new Object();
        final Object tag = this.tag;
        startBleAdapterScanAndScheduleStop(tag, true);
    }

    private int[] scanAndSleepTime = new int[2];

    private void startBleAdapterScanAndScheduleStop(Object tag, boolean callOnStarted) {
        if (Constants.DEBUG) log.d("startBleAdapterScanAndScheduleStop() called with: " + "callback = [" + callback + "], tag = [" + tag + "]");
        //
        if (callOnStarted) {
            this.callback.onStarted();
        }
        this.semaphore.call(() -> {
            if (Constants.DEBUG) {
                log.d("semaphore up, starting BLE scan");
            }
            // semaphore is notifying us that it is safe to start scan now
            if (this.tag != tag) {
                // we are not interested anymore
                return;
            }
            BleAdapter bleAdapter = this.bleAdapter.get();
            if (!bleAdapter.isEnabled()) {
                // BLE is off
                // post failure
                uiHandler.post(this::genericOnFailed);
                return;
            }
            bleAdapter.startServiceDataScan(BleConstants.SERVICE_UUID_NETWORK_NODE, new BleAdapter.ScanCallback() {
                @Override
                public void onServiceDataScan(BleDevice bluetoothDevice, int rssi, byte[] serviceData) {
                    // fixing issue #223
                    if (tag == PeriodicBleScannerImpl.this.tag) {
                        callback.onServiceDataScan(bluetoothDevice, rssi, serviceData);
                    }
                }

                @Override
                public void onScanFailed() {
                    if (Constants.DEBUG) {
                        log.d("onScanFailed");
                    }
                    genericOnFailed();
                }

            });
            bleScanning = true;
            // let the adaptor know
            scannerTimeAdaptor.onScanStarted();
            // notify the callback
            this.callback.onScanStarted();
            scannerTimeAdaptor.getScanAndSleepTime(callOnStarted, scanAndSleepTime);
            // schedule stop
            uiHandler.postDelayed(() -> {
                // stop the scan
                if (this.tag == tag) {
                    stopBleAdapterScanAndScheduleStart(tag, scanAndSleepTime[1]);
                }
            }, scanAndSleepTime[0]);
        });
    }

    private void genericOnFailed() {
        Callback oldCallback = callback;
        // call generic routine
        onStopped();
        // notify callback
        oldCallback.onScanFailed();
        if (Constants.DEBUG) {
            Preconditions.checkState(tag == null, "cannot start new scan in onScanFailed!");
        }
        oldCallback.onStopped();
    }

    private void stopBleAdapterScanAndScheduleStart(Object tag, int sleeptime) {
        if (Constants.DEBUG)
            log.d("stopBleAdapterScanAndScheduleStart() called with: " + "tag = [" + tag + "]");
        // we are still interested
        if (Constants.DEBUG) {
            Preconditions.checkState(bleScanning);
        }
        BleAdapter bleAdapter = this.bleAdapter.get();
        if (!bleAdapter.isEnabled()) {
            genericOnFailed();
            return;
        }
        bleAdapter.stopServiceDataScan();
        bleScanning = false;
        // notify callback if necessary
        scannerTimeAdaptor.onScanStopped();
        this.callback.onScanStopped();
        // schedule start again after SLEEP
        uiHandler.postDelayed(() -> {
            if (this.tag == tag) {
                startBleAdapterScanAndScheduleStop(tag, false);
            }
        }, sleeptime);
    }

    @Override
    public boolean isBleScanning() {
        return bleScanning;
    }

    @Override
    public void stopPeriodicScan() {
        if (Constants.DEBUG) {
            log.d("stopPeriodicScan");
        }
        // make sure that scanning is stopped
        if (bleAdapter.get().isScanning()) {
            Preconditions.checkNotNull(callback);
            bleAdapter.get().stopServiceDataScan();
        }
        Callback oldCallback = callback;
        onStopped();
        //
        oldCallback.onStopped();
    }

    private void onStopped() {
        // stop the scan if necessary
        boolean wasScanning = bleScanning;
        Callback oldCallback = this.callback;
        // reset state
        callback = null;
        semaphore = null;
        tag = null;
        bleScanning = false;
        // notify callback if necessary
        if (wasScanning) {
            scannerTimeAdaptor.onScanStopped();
            oldCallback.onScanStopped();
        }
    }

    @Override
    public boolean isStarted() {
        return tag != null;
    }

}
