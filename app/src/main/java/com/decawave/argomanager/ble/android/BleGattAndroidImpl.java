/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.decawave.argomanager.ble.BleGattService;
import com.decawave.argomanager.ble.ConnectionSpeed;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.kryl.android.common.log.ComponentLog;

/**
 * Ble GATT implemented via wrapping android implementation.
 */
class BleGattAndroidImpl implements BleGatt {
    private static final ComponentLog log = new ComponentLog(BleGattAndroidImpl.class);

    private BluetoothGatt delegate;

    BleGattAndroidImpl(@NotNull BluetoothGatt delegate) {
        Preconditions.checkNotNull(delegate);
        this.delegate = delegate;
    }

    @Override
    public boolean discoverServices() {
        return delegate.discoverServices();
    }

    @Override
    public BleGattService getService(UUID serviceUuid) {
        BluetoothGattService _svc = delegate.getService(serviceUuid);
        if (_svc == null) {
            return null;
        }
        return BleObjectCachingFactory.newService(_svc, this);
    }

    @Override
    public List<BleGattService> getServices() {
        List<BluetoothGattService> services = delegate.getServices();
        List<BleGattService> lst = new ArrayList<>(services.size());
        for (BluetoothGattService service : services) {
            lst.add(BleObjectCachingFactory.newService(service, this));
        }
        return lst;
    }

    @Override
    public boolean readCharacteristic(BleGattCharacteristic characteristic) {
        return delegate.readCharacteristic(((BleGattCharacteristicAndroidImpl) characteristic).delegate);
    }

    @Override
    public boolean writeCharacteristic(BleGattCharacteristic characteristic) {
        return delegate.writeCharacteristic(((BleGattCharacteristicAndroidImpl) characteristic).delegate);
    }

    @Override
    public boolean setCharacteristicNotification(BleGattCharacteristic characteristic, boolean enable) {
        return delegate.setCharacteristicNotification(((BleGattCharacteristicAndroidImpl) characteristic).delegate, enable);
    }

    @Override
    public boolean readDescriptor(BleGattDescriptor descriptor) {
        return delegate.readDescriptor(((BleGattDescriptorAndroidImpl) descriptor).delegate);
    }

    @Override
    public boolean writeDescriptor(BleGattDescriptor descriptor) {
        return delegate.writeDescriptor(((BleGattDescriptorAndroidImpl) descriptor).delegate);
    }

    @Override
    public BleDevice getDevice() {
        return BleObjectCachingFactory.newDevice(delegate.getDevice());
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }

    @Override
    public boolean requestMtu(int mtu) {
        return delegate.requestMtu(mtu);
    }

    @Override
    public boolean requestConnectionSpeed(ConnectionSpeed connectionSpeed) {
        int priority;
        switch (connectionSpeed) {
            case HIGH:
                priority = BluetoothGatt.CONNECTION_PRIORITY_HIGH;
                break;
            case BALANCED:
                priority = BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
                break;
            case LOW_POWER:
                priority = BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
                break;
            default:
                throw new IllegalStateException("unexpected connection speed: " + connectionSpeed);
        }
        return delegate.requestConnectionPriority(priority);
    }

    @Override
    public void close() {
        if (Constants.DEBUG) {
            log.d("close()");
        }
        // make sure we got fresh data the next time
//        refreshDeviceCache();
        //
        delegate.close();
        // clear cached representations
        BleObjectCachingFactory.forgetBleGatt(this);
        // throw away the delegate reference (Android bug - in order to release resources we need to do GC)
        delegate = null;
    }

    // we need this method for proper disconnect
    @SuppressWarnings("unused")
    private boolean refreshDeviceCache(){
        try {
            Method refreshMethod = BluetoothGatt.class.getMethod("refresh");
            if (refreshMethod != null) {
                if (Constants.DEBUG) log.d("refresh()");
                return (Boolean) refreshMethod.invoke(delegate);
            }
        }
        catch (Exception localException) {
            // silently ignore
            if (Constants.DEBUG) {
                log.w("cannot invoke refresh() via reflections on " + getDevice().getAddress());
            }
        }
        return false;
    }

}
