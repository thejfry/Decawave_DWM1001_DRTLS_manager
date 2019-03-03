/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.google.common.base.Preconditions;

import java.util.LinkedList;
import java.util.Queue;

import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */

class SequentialGattOperationQueueImpl implements SequentialGattOperationQueue {
    private static final ComponentLog log = new ComponentLog(SequentialGattOperationQueueImpl.class).disable();
    // constant
    private static final Token SKIP_TOKEN = new StatefulToken(StatefulToken.State.SKIP);
    // dependencies
    private final GattInteractionFsm gattInteractionFsm;
    // state
    private Queue<OperationInfo> queue;
    private OperationInfo currentOp;
    private boolean active;

    // enqueued operation
    private class OperationInfo {
        final StatefulToken token;
        final GenericOperation operation;

        OperationInfo(StatefulToken token, GenericOperation operation) {
            this.token = token;
            this.operation = operation;
        }

        @Override
        public String toString() {
            return "OperationInfo{" +
                    "token=" + token +
                    ", operation=" + operation +
                    '}';
        }
    }

    // stateful token, contains result of task execution
    private static class StatefulToken implements Token {

        enum State {
            FAIL,
            SKIP,
            SUCCESS
        }

        State state;

        StatefulToken(State state) {
            this.state = state;
        }

        StatefulToken() {
            this.state = null;
        }

        @Override
        public String toString() {
            return "StatefulToken{" +
                    "hashCode=" + hashCode() +
                    ",state=" + state +
                    '}';
        }
    }

    SequentialGattOperationQueueImpl(GattInteractionFsm gattInteractionFsm) {
        this.gattInteractionFsm = gattInteractionFsm;
        this.queue = new LinkedList<>();
        this.active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() {
        if (Constants.DEBUG) log.d("deactivate()");
        active = false;
        // even if we are deactivated we will still process the asynchronous result
        // but we will not continue dispatching the queued requests
    }

    @Override
    public void activate() {
        if (Constants.DEBUG) log.d("activate()");
        active = true;
        executeNext();
    }

    @Override
    public Token addOperation(GenericOperation operation) {
        StatefulToken dependsOnToken = (StatefulToken) operation.dependsOn();
        if (dependsOnToken != null &&
                (dependsOnToken.state == StatefulToken.State.SKIP || dependsOnToken.state == StatefulToken.State.FAIL)) {
            // do not enqueue, simply return constant SKIP_TOKEN
            if (Constants.DEBUG) log.d("skipping operation enqueue: " + operation + ", dependsOnToken: " + dependsOnToken);
            return SKIP_TOKEN;
        }
        // else: add the operation to the queue
        if (Constants.DEBUG) log.d("operation enqueue: " + operation);
        StatefulToken token = new StatefulToken();
        this.queue.add(new OperationInfo(token, operation));
        //
        if (active && currentOp == null) {
            executeNext();
        }
        return token;
    }

    @Override
    public void onGattFail(SynchronousBleGatt gatt, int errorCode, String errorMessage) {
        preAsyncResult(false).getCallback().onFail(gatt, errorCode, errorMessage);
        // continue
        postAsyncResult();
    }

    @Override
    public void onGattSuccess(SynchronousBleGatt gatt) {
        preAsyncResult(true).getCallback().onSuccess(gatt);
        // continue
        postAsyncResult();
    }

    private AsynchronousGattOperation preAsyncResult(boolean success) {
        if (Constants.DEBUG) {
            Preconditions.checkState(currentOp != null, "no async operation is executing");
            Preconditions.checkState(currentOp.operation instanceof AsynchronousGattOperation,
                    "can process only AsynchronousGattOperation, in order to implement some other async, introduce a new onXxxCallback() method");
        }
        OperationInfo oldOp = this.currentOp;
        oldOp.token.state = success ? StatefulToken.State.SUCCESS : StatefulToken.State.FAIL;
        // clear current op
        this.currentOp = null;
        // return the old operation
        return (AsynchronousGattOperation) oldOp.operation;
    }

    private void postAsyncResult() {
        // we are finished with the callback processing
        if (active && this.currentOp == null) {
            // process next operation in the row
            executeNext();
        }
    }

    @Override
    public boolean hasPendingAsyncOperation() {
        return currentOp != null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //

    private void executeNext() {
        if (Constants.DEBUG) {
            Preconditions.checkState(currentOp == null, "there is a pending async operation, cannot execute next!");
        }
        while (!queue.isEmpty()) {
            currentOp = queue.poll();
            StatefulToken dependsOn = (StatefulToken) currentOp.operation.dependsOn();
            if (dependsOn != null) {
                if (Constants.DEBUG) {
                    Preconditions.checkNotNull(dependsOn.state, "dependent operation state is null?!");
                }
                if (dependsOn.state != StatefulToken.State.SUCCESS) {
                    // skip this one
                    currentOp.token.state = StatefulToken.State.SKIP;
                    continue;
                }
            }
            if (Constants.DEBUG) log.d("executing operation " + currentOp.operation);
            GenericOperation.Result result = currentOp.operation.execute(gattInteractionFsm);
            if (Constants.DEBUG) log.d("operation returned " + result);
            switch (result) {
                case ASYNC_NOT_KNOWN:
                    // the operation has not finished yet, it waits for asynchronous callback
                    return;
                case SYNCHRONOUS_FAIL:
                    currentOp.token.state = StatefulToken.State.FAIL;
                    // execute next - if any
                    break;
                case SYNCHRONOUS_SUCCESS:
                    currentOp.token.state = StatefulToken.State.SUCCESS;
                    // ok simply execute the next task in the queue
                    break;
            }
        }
        // we have exhausted all operations
        currentOp = null;
    }

}
