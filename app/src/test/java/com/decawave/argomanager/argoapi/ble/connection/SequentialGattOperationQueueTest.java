/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Argo project.
 *

09-07 13:23:42.141  7940  7940 E AndroidRuntime: Process: com.decawave.argomanager, PID: 7940
09-07 13:23:42.141  7940  7940 E AndroidRuntime: java.lang.NullPointerException: dependent operation state is null?!
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.google.common.base.Preconditions.checkNotNull(Preconditions.java:228)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.SequentialGattOperationQueueImpl.executeNext(SequentialGattOperationQueueImpl.java:166)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.SequentialGattOperationQueueImpl.addOperation(SequentialGattOperationQueueImpl.java:122)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.WriteCharacteristicOperation.enqueue(WriteCharacteristicOperation.java:37)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.writeCharacteristics(NetworkNodeBleConnectionImpl.java:545)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.disconnect(NetworkNodeBleConnectionImpl.java:301)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.callFailConditionalDisconnect(NetworkNodeBleConnectionImpl.java:443)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.lambda$getOtherSideEntity$2(NetworkNodeBleConnectionImpl.java:363)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl$$Lambda$3.call(Unknown Source)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.AsynchronousGattOperation.lambda$new$0(AsynchronousGattOperation.java:63)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.AsynchronousGattOperation$$Lambda$1.call(Unknown Source)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.AsynchronousGattOperation$1.onFail(AsynchronousGattOperation.java:53)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.lambda$onOperationFailed$17(NetworkNodeBleConnectionImpl.java:1077)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl$$Lambda$20.call(Unknown Source)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.AsynchronousGattOperation.executeCallback(AsynchronousGattOperation.java:88)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.SequentialGattOperationQueueImpl.onGattOperationResult(SequentialGattOperationQueueImpl.java:138)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.onOperationFailed(NetworkNodeBleConnectionImpl.java:1077)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl.access$200(NetworkNodeBleConnectionImpl.java:76)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnectionImpl$1.onCharacteristicReadFailed(NetworkNodeBleConnectionImpl.java:211)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.GattInteractionToConnectionWrapperCallback$$Lambda$8.call(Unknown Source)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.GattInteractionToConnectionWrapperCallback.delegateFailToConnection(GattInteractionToConnectionWrapperCallback.java:131)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.connection.GattInteractionToConnectionWrapperCallback.onCharacteristicReadFailed(GattInteractionToConnectionWrapperCallback.java:90)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.GattInteractionCallbackWrapper.onCharacteristicReadFailed(GattInteractionCallbackWrapper.java:54)
09-07 13:23:42.141  7940  7940 E AndroidRuntime: 	at com.decawave.argomanager.argoapi.ble.GattInteractionFsmImpl$4.lambda$onCharacteristicReadFailed$1(Gat

 */


public class SequentialGattOperationQueueTest {

    private class FakeAsyncOperation extends AsynchronousGattOperation {

        FakeAsyncOperation(Action1<SynchronousBleGatt> onSuccess,
                           Action2<SynchronousBleGatt, Fail> onFail,
                           SequentialGattOperationQueue.Token dependsOn) {
            super((gi) -> {
                // noop
            }, onSuccess, onFail, dependsOn);
        }

    }

    @Test
    public void testBugDependentOperationIsNullScenario() {
        GattInteractionFsm mockFsm = Mockito.mock(GattInteractionFsm.class);
        SynchronousBleGatt mockBleGatt = Mockito.mock(SynchronousBleGatt.class);
        SequentialGattOperationQueue operationQueue = new SequentialGattOperationQueueImpl(mockFsm);
        operationQueue.activate();
        boolean[] finalOperationScheduled = {false};
        boolean[] async2Finished = {false};
        // enqueue several operations now
        FakeAsyncOperation async1 = new FakeAsyncOperation(
                // onSuccess
                null,
                // onFail
                (bg,f) -> {
                    // schedule something else (no dependency here)
                    // here was an exception - previously
                    operationQueue.addOperation(new FakeAsyncOperation(null, null, null));
                    //
                    finalOperationScheduled[0] = true;
                },
                null);
        // schedule
        SequentialGattOperationQueue.Token async1Token = operationQueue.addOperation(async1);
        // the async1 is now 'running'...
        // let us enqueue another operation dependent on async1
        FakeAsyncOperation async2 = new FakeAsyncOperation(
                // onSuccess
                synchronousBleGatt -> async2Finished[0] = true,
                // onFail
                (synchronousBleGatt, fail) -> async2Finished[0] = true,
                // dependency
                async1Token);
        // schedule the operation
        operationQueue.addOperation(async2);
        // let the async1 fail now (pretend that the operation is done/has failed asynchronously)
        operationQueue.onGattFail(mockBleGatt, 23, "mock fail");
        //
        Assert.assertTrue("final operation must be scheduled!", finalOperationScheduled[0]);
        Assert.assertFalse("second dependent operation must not be executed!", async2Finished[0]);
    }


}
