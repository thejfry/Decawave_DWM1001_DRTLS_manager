/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;

/**
 * Capable of processing enqueued tasks.
 *
 * @see GenericOperation
 */
public interface SequentialGattOperationQueue {

    /**
     * Identifies enqueued operation.
     * Capable to express dependencies.
     */
    interface Token {

    }

    boolean isActive();

    void deactivate();

    Token addOperation(GenericOperation operation);

    /**
     * Let the queue know that the currently executing asynchronous operation has successfully (asynchronously) finished.
     */
    void onGattSuccess(SynchronousBleGatt gatt);

    /**
     * Let the queue know that the currently executing asynchronous operation has fail (asynchronously).
     */
    void onGattFail(SynchronousBleGatt gatt, int errorCode, String errorMessage);

    boolean hasPendingAsyncOperation();

    void activate();

}
