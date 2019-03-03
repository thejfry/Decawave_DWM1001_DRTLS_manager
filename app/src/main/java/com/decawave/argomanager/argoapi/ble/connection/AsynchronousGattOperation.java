/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;

import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;

/**
 * Asynchronous gatt operation predecessor.
 */
class AsynchronousGattOperation implements GenericOperation {
    private final Action1<GattInteractionFsm> operation;
    private final Callback callback;
    private final SequentialGattOperationQueue.Token dependsOn;

    interface Callback {

        void onSuccess(SynchronousBleGatt gatt);

        void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage);

    }

    private AsynchronousGattOperation(Action1<GattInteractionFsm> operation,
                                      Callback callback,
                                      SequentialGattOperationQueue.Token dependsOn) {
        this.operation = operation;
        this.callback = callback;
        this.dependsOn = dependsOn;
    }

    private AsynchronousGattOperation(Action1<GattInteractionFsm> operation,
                                      Action1<SynchronousBleGatt> onSuccess,
                                      Action3<SynchronousBleGatt, Integer, String> onFail,
                                      SequentialGattOperationQueue.Token dependsOn) {
        this(operation, new Callback() {
            @Override
            public void onSuccess(SynchronousBleGatt gatt) {
                if (onSuccess != null) onSuccess.call(gatt);
            }

            @Override
            public void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                if (onFail != null) onFail.call(gatt, errorCode, failMessage);
            }
        }, dependsOn);
    }

    AsynchronousGattOperation(Action1<GattInteractionFsm> operation,
                              Action1<SynchronousBleGatt> onSuccess,
                              Action2<SynchronousBleGatt, Fail> onFail,
                              SequentialGattOperationQueue.Token dependsOn) {
        this(operation, onSuccess, (synchronousBleGatt, errorCode, failMessage) -> {
            if (onFail != null) onFail.call(synchronousBleGatt, new Fail(errorCode, failMessage));
        }, dependsOn);
    }

    AsynchronousGattOperation(Action1<GattInteractionFsm> operation,
                                     Action1<SynchronousBleGatt> onSuccess,
                                     Action1<Fail> onFail,
                                     SequentialGattOperationQueue.Token dependsOn) {
        this(operation, onSuccess, (synchronousBleGatt, errorCode, failMessage) -> {
            if (onFail != null) onFail.call(new Fail(errorCode, failMessage));
        }, dependsOn);
    }

    Callback getCallback() {
        return callback;
    }

    @Override
    public SequentialGattOperationQueue.Token dependsOn() {
        return dependsOn;
    }

    @Override
    public Result execute(GattInteractionFsm gattInteraction) {
        operation.call(gattInteraction);
        return Result.ASYNC_NOT_KNOWN;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "hashCode=" + hashCode() +
                ",dependsOn=" + dependsOn +
                '}';
    }
}
