/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;

import rx.functions.Action1;

/**
 * Argo project.
 */
class ChangeMtuOperation extends AsynchronousGattOperation {

    private ChangeMtuOperation(int newMtu, Action1<SynchronousBleGatt> onSuccess, Action1<Fail> onFail) {
        super((gi) -> gi.changeMtu(newMtu), onSuccess, onFail, null);
    }

    static int getSystemMtu(int userMtu) {
        return userMtu + BleConstants.EXTRA_MTU;
    }

    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue sequentialGattOperationQueue, int newUserMtu, Action1<Fail> onFail) {
        // we will always increase the MTU with BleConstants.EXTRA_MTU (see https://punchthrough.com/blog/posts/maximizing-ble-throughput-part-2-use-larger-att-mtu)
        return sequentialGattOperationQueue.addOperation(new ChangeMtuOperation(getSystemMtu(newUserMtu), null, onFail));
    }

    @SuppressWarnings("UnusedReturnValue")
    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue sequentialGattOperationQueue, int newUserMtu, Action1<SynchronousBleGatt> onSuccess, Action1<Fail> onFail) {
        // we will always increase the MTU with BleConstants.EXTRA_MTU (see https://punchthrough.com/blog/posts/maximizing-ble-throughput-part-2-use-larger-att-mtu)
        return sequentialGattOperationQueue.addOperation(new ChangeMtuOperation(getSystemMtu(newUserMtu), onSuccess, onFail));
    }

}
