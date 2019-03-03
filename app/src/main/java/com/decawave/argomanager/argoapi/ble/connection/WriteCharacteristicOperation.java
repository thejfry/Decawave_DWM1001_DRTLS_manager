/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.argoapi.ble.WriteCharacteristicRequest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Argo project.
 */
class WriteCharacteristicOperation extends AsynchronousGattOperation {

    private WriteCharacteristicOperation(@NotNull List<WriteCharacteristicRequest> writeRequests,
                                         @Nullable Action1<SynchronousBleGatt> onSuccess,
                                         @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                         SequentialGattOperationQueue.Token dependsOn) {
        super((gi) -> gi.writeCharacteristics(writeRequests),
                onSuccess, onFail, dependsOn);
    }

    static void enqueue(SequentialGattOperationQueue queue, List<WriteCharacteristicRequest> writeRequests,
                        @Nullable Action1<SynchronousBleGatt> onSuccess,
                        @Nullable Action2<SynchronousBleGatt, Fail> onFail, SequentialGattOperationQueue.Token dependsOn) {
        queue.addOperation(new WriteCharacteristicOperation(writeRequests, onSuccess, onFail, dependsOn));
    }

}
