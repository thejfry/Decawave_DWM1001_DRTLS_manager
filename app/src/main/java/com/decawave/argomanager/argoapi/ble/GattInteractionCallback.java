/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.BleGattCharacteristic;

import java.util.List;

/**
 * Notification callback about the GATT interaction FSM events:
 * 1. driving lifecycle of {@link GattInteractionFsm}.
 * 2. being called back from {@link GattInteractionFsm}.
 *
 */
public interface GattInteractionCallback {

    /**
     * Information for this FSM whether it still makes sense to continue in it's lifecycle.
     *
     * @return true if so, false otherwise
     */
    boolean stillInterested();

    /**
     * Called after the read characteristics request is successfully executed.
     *
     * @param gatt gatt to be queried for the loaded information
     */
    void onCharacteristicReadComplete(SynchronousBleGatt gatt);

    /**
     * Called after the read descriptors request is successfully executed.
     *
     * @param gatt gatt to be queried for the loaded information
     */
    void onDescriptorReadComplete(SynchronousBleGatt gatt);

    /**
     * Called after the descriptor write have been successfully done.
     *
     * @see GattInteractionFsm#writeCharacteristics(List)
     */
    void onDescriptorWriteComplete(SynchronousBleGatt gatt);

    /**
     * Called after the write have been successfully done.
     *
     * @see GattInteractionFsm#writeCharacteristics(List)
     */
    void onCharacteristicWriteComplete(SynchronousBleGatt gatt);

    /**
     * Called after MTU has been successfully changed.
     *
     * @see GattInteractionFsm#writeCharacteristics(List)
     */
    void onMtuChangeComplete(SynchronousBleGatt syncGatt);

    /**
     * Called in case of connection error: failed connection set up, service discovery, sudden disconnect.
     *
     * @param gatt gatt to be queried / used
     */
    void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * Called if the read characteristic request failed.
     *
     * @param gatt identification of the gatt
     */
    void onCharacteristicReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * Called after the write sequence failed.
     *
     * @param gatt identification of the gatt
     *
     * @see GattInteractionFsm#writeCharacteristics(List)
     */
    void onCharacteristicWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * Called if the read descriptor request failed.
     *
     * @param gatt identification of the gatt
     */
    void onDescriptorReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * Called after the write sequence failed.
     *
     * @param gatt identification of the gatt
     *
     * @see GattInteractionFsm#writeDescriptors(List)
     */
    void onDescriptorWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * Called after MTU change request failed.
     *
     * @param gatt identification of the gatt
     */
    void onMtuChangeFailed(SynchronousBleGatt gatt, int errorCode, String failMessage);

    /**
     * When we register for characteristic change event, we receive the changes via this callback.
     *
     * @param gatt identification of the gatt
     */
    void onCharacteristicChanged(SynchronousBleGatt gatt, BleGattCharacteristic characteristic, byte[] value);

}
