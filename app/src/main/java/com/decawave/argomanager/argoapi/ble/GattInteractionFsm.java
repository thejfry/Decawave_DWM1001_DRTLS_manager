/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.ConnectionSpeed;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * FSM driving interaction with BLE Gatt server.
 *
 * Requests dispatching is done asynchronously. Result notification is
 * performed via {@link GattInteractionCallback}.
 */
public interface GattInteractionFsm {

    /**
     * Initiates a connection.
     *
     * @param gattInteractionCallback application level callback for asynchronous notifications
     */
    void initiateConnect(GattInteractionCallback gattInteractionCallback);

    void initiateDisconnect();

    void setDisconnectOnProblem(boolean doDisconnect);

    void setDebugLoggingEnabled(boolean debugLoggingEnabled);

    /**
     * @return true if the state is DISCONNECTING or DISCONNECTED
     */
    boolean isTerminate();

    boolean isDisconnected();

    /**
     * Initiates characteristics write.
     * Caller is notified back about the result via {@link GattInteractionCallback} passed to {@link #initiateConnect(GattInteractionCallback)}.
     *
     * @param writeRequests write requests to perform
     */
    void writeCharacteristics(List<WriteCharacteristicRequest> writeRequests);

    /**
     * Initiates characteristics read.
     * Caller is notified back about the result via {@link GattInteractionCallback#onCharacteristicReadComplete(SynchronousBleGatt)}.
     *
     * @param readRequests read requests to perform
     */
    void readCharacteristics(Set<ReadCharacteristicRequest> readRequests);

    /**
     * Initiates characteristics read.
     * Caller is notified back about the result via {@link GattInteractionCallback#onCharacteristicReadComplete(SynchronousBleGatt)}.
     *
     * @param readRequests read requests to perform
     */
    void readCharacteristics(ReadCharacteristicRequest... readRequests);

    /**
     * Initiates descriptors read.
     * Caller is notified back about the result via {@link GattInteractionCallback#onDescriptorReadComplete(SynchronousBleGatt)}.
     *
     * @param readRequests read requests to perform
     */
    void readDescriptors(List<ReadDescriptorRequest> readRequests);

    /**
     * Initiates descriptors write.
     * Caller is notified back about the result via {@link GattInteractionCallback} passed to {@link #initiateConnect(GattInteractionCallback)}.
     *
     * @param writeDescriptorRequests write requests to perform
     */
    void writeDescriptors(List<WriteDescriptorRequest> writeDescriptorRequests);


    /**
     * Initiates MTU change.
     * Caller is notified back about the result via {@link GattInteractionCallback} passed to {@link #initiateConnect(GattInteractionCallback)}.
     */
    void changeMtu(int newMtu);

    /**
     * Changes connection speed.
     */
    boolean changeConnectionSpeed(ConnectionSpeed connectionSpeed);

    /**
     * @return last negotiated MTU
     */
    @Nullable Integer getLastNegotiatedSystemMtu();

    /**
     * @return whether the device disconnects on problem.
     */
    boolean doDisconnectOnProblem();

    /**
     * Sets idle state - for example after an error report is processed, in order to be able to further interact with a FSM.
     */
    void makeSureIdle();

}
