/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.util.gatt.GattEncoder;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Ble adapter implemented via wrapping android implementation.
 */
public class BleAdapterLollipopAndroidImpl implements BleAdapter {
    private static final ComponentLog log = new ComponentLog(BleAdapterLollipopAndroidImpl.class).disable();
    private static final ScanSettings SCAN_SETTINGS = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

    private final BluetoothAdapter delegate;
    //
    private android.bluetooth.le.ScanCallback lollipopScanCallback;

    public BleAdapterLollipopAndroidImpl(BluetoothAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public BleDevice getRemoteDevice(String bleAddress) {
        BluetoothDevice device = delegate.getRemoteDevice(bleAddress);
        Preconditions.checkNotNull(device);
        return wrapDevice(device);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void startServiceDataScan(UUID serviceUuid, final ScanCallback callback) {
        if (Constants.DEBUG) {
            Preconditions.checkState(lollipopScanCallback == null, "scan has been already started");
        }
        // start scan, filter only those matching the given anyOfService
        log.d("starting service scan: " + serviceUuid);
        BluetoothLeScanner scanner = delegate.getBluetoothLeScanner();
        if (scanner == null) {
            log.w("scanner is null! someone disabled BT in the meantime? simulating fail");
            uiHandler.post(callback::onScanFailed);
            return;
        }
        List<ScanFilter> filters = null;
        ParcelUuid serviceDataUuid;
        if (serviceUuid != null) {
            filters = new ArrayList<>(1);
            ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
            serviceDataUuid = new ParcelUuid(serviceUuid);
            scanFilterBuilder.setServiceData(serviceDataUuid, null);
            filters.add(scanFilterBuilder.build());
        }
        // avoid failed callback invocation before this invocation finishes
        boolean[] startScanFinished = {false};
        android.bluetooth.le.ScanCallback[] sc = { null };
        sc[0] = new android.bluetooth.le.ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                if (Constants.DEBUG) {
                    log.d("received advertisement from " + result.getDevice().getAddress());
                }
                final BluetoothDevice device = result.getDevice();
                final int rssi = result.getRssi();
                //noinspection ConstantConditions
                parseAdRecordsNotifyCallback(device, rssi,
                        result.getScanRecord().getBytes(), serviceUuid, callback, sc[0]);
            }

            @Override
            public void onScanFailed(int errorCode) {
                lollipopScanCallback = null;
                // avoid too early callback notification
                if (!startScanFinished[0]) {
                    ArgoApp.uiHandler.post(callback::onScanFailed);
                } else {
                    callback.onScanFailed();
                }
            }
        };
        lollipopScanCallback = sc[0];
        // start the scan now
        scanner.startScan(filters, SCAN_SETTINGS, lollipopScanCallback);
        //
        startScanFinished[0] = true;
    }


    private void parseAdRecordsNotifyCallback(BluetoothDevice bluetoothDevice,
                                              int rssi,
                                              byte[] bytes,
                                              UUID serviceUuid,
                                              BleAdapter.ScanCallback callback,
                                              android.bluetooth.le.ScanCallback lollipopScanCallback) {
        List<BleAdvertUtil.AdRecord> adRecords = BleAdvertUtil.parseAdRecord(bytes);
        // find the record with advertised service
        for (BleAdvertUtil.AdRecord adRecord : adRecords) {
            if (adRecord.length > 17 && adRecord.type == 0x21) {
                // parse the first 16 bytes as UUID
                if (serviceUuid == null || serviceUuid.equals(GattEncoder.decodeUuid(adRecord.data))) {
                    final byte[] serviceData = Arrays.copyOfRange(adRecord.data, 16, adRecord.length - 1);
                    // pass the scan result onto UI thread
                    ArgoApp.uiHandler.post(() -> {
                        // fixing bug #200: check that we are still scanning
                        if (this.lollipopScanCallback == lollipopScanCallback)
                            callback.onServiceDataScan(BleObjectCachingFactory.newDevice(bluetoothDevice), rssi, serviceData);
                    });
                    break;
                }
            }
        }
    }

    @Override
    public void stopServiceDataScan() {
        log.d("stopping scan");
        Preconditions.checkNotNull(lollipopScanCallback, "cannot stop scan, when scan has not been started yet!");
        android.bluetooth.le.ScanCallback callback = lollipopScanCallback;
        lollipopScanCallback = null;
        BluetoothLeScanner scanner = delegate.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(callback);
        } else {
            log.w(new Exception("returned scanner is null!"));
        }
    }

    @Override
    public boolean isScanning() {
        return lollipopScanCallback != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleAdapterLollipopAndroidImpl that = (BleAdapterLollipopAndroidImpl) o;

        return delegate.equals(that.delegate);

    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    private BleDevice wrapDevice(BluetoothDevice device) {
        return BleObjectCachingFactory.newDevice(device);
    }

}
