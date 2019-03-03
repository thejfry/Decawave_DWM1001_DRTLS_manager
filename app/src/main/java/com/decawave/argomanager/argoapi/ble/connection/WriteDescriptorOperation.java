/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.argoapi.ble.WriteDescriptorRequest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Operation capable of writing descriptors.
 */
class WriteDescriptorOperation extends AsynchronousGattOperation {

    private WriteDescriptorOperation(@NotNull List<WriteDescriptorRequest> writeRequests,
                                     @Nullable Action1<SynchronousBleGatt> onSuccess,
                                     @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                     @Nullable SequentialGattOperationQueue.Token dependsOn) {
        super((gi) -> gi.writeDescriptors(writeRequests), onSuccess, onFail, dependsOn);
    }

    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue queue, List<WriteDescriptorRequest> writeDescriptorRequests,
                                                      @Nullable Action1<SynchronousBleGatt> onSuccess,
                                                      @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                                      SequentialGattOperationQueue.Token dependsOn) {
        return queue.addOperation(new WriteDescriptorOperation(writeDescriptorRequests, onSuccess, onFail, dependsOn));
    }

}
