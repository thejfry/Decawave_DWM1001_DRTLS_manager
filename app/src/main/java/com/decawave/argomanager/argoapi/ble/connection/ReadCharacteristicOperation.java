/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.BleGattServiceRdonly;
import com.decawave.argomanager.argoapi.ble.ReadCharacteristicRequest;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.util.gatt.GattDecodeContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Argo project.
 *
 * This operation fill decode context when operation mode is among read properties.
 */
class ReadCharacteristicOperation extends AsynchronousGattOperation {

    private ReadCharacteristicOperation(List<ReadCharacteristicRequest> readRequests,
                                        @Nullable Action1<SynchronousBleGatt> onSuccess,
                                        @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                        @NotNull GattDecodeContext decodeContext,
                                        SequentialGattOperationQueue.Token dependsOn) {

        super((gi) -> gi.readCharacteristics(readRequests.toArray(new ReadCharacteristicRequest[readRequests.size()])),
                (syncBleGatt) -> {
                    // onSuccess
                    // let the decode context know
                    byte[] opMode = getOperationModeOpt(readRequests, syncBleGatt);
                    if (opMode != null) {
                        decodeContext.setOperationMode(opMode);
                    }
                    // delegate to success callback
                    if (onSuccess != null) onSuccess.call(syncBleGatt);
                }, onFail, dependsOn);
    }

    @SuppressWarnings("SameParameterValue")
    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue queue,
                                                      ReadCharacteristicRequest readCharacteristicRequest,
                                                      @Nullable Action1<SynchronousBleGatt> onSuccess,
                                                      @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                                      @NotNull GattDecodeContext context,
                                                      SequentialGattOperationQueue.Token dependsOn) {
        return queue.addOperation(new ReadCharacteristicOperation(Collections.singletonList(readCharacteristicRequest), onSuccess, onFail, context, dependsOn));
    }


    static SequentialGattOperationQueue.Token enqueue(SequentialGattOperationQueue queue,
                                                      List<ReadCharacteristicRequest> readCharacteristicRequest,
                                                      @Nullable Action1<SynchronousBleGatt> onSuccess,
                                                      @Nullable Action2<SynchronousBleGatt, Fail> onFail,
                                                      @NotNull GattDecodeContext context,
                                                      SequentialGattOperationQueue.Token dependsOn) {
        return queue.addOperation(new ReadCharacteristicOperation(readCharacteristicRequest, onSuccess, onFail, context, dependsOn));
    }

    private static byte[] getOperationModeOpt(List<ReadCharacteristicRequest> requests, SynchronousBleGatt synchronousBleGatt) {
        for (ReadCharacteristicRequest request : requests) {
            if (BleConstants.CHARACTERISTIC_OPERATION_MODE.equals(request.characteristicUuid)) {
                // hit
                BleGattServiceRdonly svc = synchronousBleGatt.getService(request.serviceUuid);
                if (svc != null) {
                    BleGattCharacteristic ch = svc.getCharacteristic(BleConstants.CHARACTERISTIC_OPERATION_MODE);
                    if (ch != null) {
                        return ch.getValue();
                    }
                }
                return null;
            }
        }
        return null;
    }
}
