/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;

import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Argo project.
 */

class SynchronousOperation implements GenericOperation {
    private final Func1<GattInteractionFsm,Boolean> operation;
    private final SequentialGattOperationQueue.Token dependsOn;

    private SynchronousOperation(Func1<GattInteractionFsm, Boolean> operation, SequentialGattOperationQueue.Token dependsOn) {
        this.operation = operation;
        this.dependsOn = dependsOn;
    }

    @Override
    public SequentialGattOperationQueue.Token dependsOn() {
        return dependsOn;
    }

    @Override
    public Result execute(GattInteractionFsm gattInteractionFsm) {
        return operation.call(gattInteractionFsm) ? Result.SYNCHRONOUS_SUCCESS : Result.SYNCHRONOUS_FAIL;
    }

    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue queue, Func1<GattInteractionFsm,Boolean> operation, SequentialGattOperationQueue.Token dependsOn) {
        return queue.addOperation(new SynchronousOperation(operation, dependsOn));
    }

    static SequentialGattOperationQueue.Token enqueueAction(SequentialGattOperationQueue queue, Action1<GattInteractionFsm> operation, SequentialGattOperationQueue.Token dependsOn) {
        return enqueue(queue, gattInteractionFsm -> {
            operation.call(gattInteractionFsm);
            return true;
        }, dependsOn);
    }

    @Override
    public String toString() {
        return "SynchronousOperation{" +
                "dependsOn=" + dependsOn +
                '}';
    }
}
