/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.ConnectionState;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCallback;
import com.decawave.argomanager.ble.android.BleObjectCachingFactory;

import java.util.Arrays;

/**
 * Gatt callback delegating the logic to BleGattCallback, performing the operations on main application/UI thread.
 */
public class BtGattCallbackAdapter extends BluetoothGattCallback {
    private final BleGattCallback delegate;
    private final Supplier<BleGatt> supplier;


    public BtGattCallbackAdapter(BleGattCallback delegate, Supplier<BleGatt> bleGattSupplier) {
        this.delegate = delegate;
        this.supplier = bleGattSupplier;
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        ArgoApp.uiHandler.post(() -> delegate.onConnectionStateChange(supplier.get(), status, toBleState(newState)));

    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        ArgoApp.uiHandler.post(() -> delegate.onServicesDiscovered(supplier.get(), status));
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        // clone the value (theoretically it might change on the background)
        byte[] valueCopy = getValueCopy(characteristic.getValue());
        ArgoApp.uiHandler.post(() ->
                delegate.onCharacteristicRead(supplier.get(),
                        BleObjectCachingFactory.newCharacteristic(characteristic, supplier.get()),
                        valueCopy,
                        status));
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        // workaround for slow write request processing
        ArgoApp.uiHandler.post(() ->
                delegate.onCharacteristicWritten(supplier.get(),
                        BleObjectCachingFactory.newCharacteristic(characteristic, supplier.get()),
                        status));
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        byte[] valueCopy = getValueCopy(characteristic.getValue());
        ArgoApp.uiHandler.post(() ->
                delegate.onCharacteristicChanged(supplier.get(),
                        BleObjectCachingFactory.newCharacteristic(characteristic, supplier.get()),
                        valueCopy));
    }

    @Override
    public void onDescriptorRead(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        byte[] valueCopy = getValueCopy(descriptor.getValue());
        ArgoApp.uiHandler.post(() ->
                delegate.onDescriptorRead(supplier.get(),
                        BleObjectCachingFactory.newDescriptor(descriptor, supplier.get()),
                        valueCopy,
                        status));
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        ArgoApp.uiHandler.post(() ->
                delegate.onDescriptorWritten(supplier.get(), BleObjectCachingFactory.newDescriptor(descriptor, supplier.get()), status));
    }

//    public void onReliableWriteCompleted(final BluetoothGatt gatt, final int status) {

//    public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        ArgoApp.uiHandler.post(() -> delegate.onMtuChanged(supplier.get(), mtu, status));
    }

    private ConnectionState toBleState(int newState) {
        switch (newState) {
            case BluetoothAdapter.STATE_CONNECTED:
                return ConnectionState.CONNECTED;
            case BluetoothAdapter.STATE_CONNECTING:
                return ConnectionState.CONNECTING;
            case BluetoothAdapter.STATE_DISCONNECTED:
                return ConnectionState.CLOSED;
            case BluetoothAdapter.STATE_DISCONNECTING:
                return ConnectionState.DISCONNECTING;
            default:
                throw new IllegalStateException("unsupported connections state: " + newState);
        }
    }

    private byte[] getValueCopy(byte[] value) {
        byte[] valueCopy;
        if (value != null) {
            valueCopy = Arrays.copyOf(value, value.length);
        } else {
            valueCopy = null;
        }
        return valueCopy;
    }

}
