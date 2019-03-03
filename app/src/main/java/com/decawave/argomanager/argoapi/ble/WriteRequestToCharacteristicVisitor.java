/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.google.common.base.Preconditions;

import java.util.UUID;

import eu.kryl.android.common.Constants;
import rx.functions.Func0;

/**
 * Converts the given request to setting appropriate content of the enclosed characteristic.
 *
 * Make sure that you do not use one instance of visitor concurrently!
 *
 * Whenever you wish to convert the value to characteristic setValue() do {@link #setCharacteristic(BleGattCharacteristic)}
 * first.
 *
 */
class WriteRequestToCharacteristicVisitor implements WriteCharacteristicRequestVisitor {
    private BleGattCharacteristic characteristic;

    WriteRequestToCharacteristicVisitor() {
    }

    public void setCharacteristic(BleGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public void visitText(WriteCharacteristicRequest<String> request) {
        doVisit(() -> characteristic.setStringValue(request.getValue()), "setStringValue()");
    }

    @Override
    public void visitUuid(WriteCharacteristicRequest<UUID> request) {
        doVisit(() -> characteristic.setUuidValue(request.getValue()), "setUuidValue()");
    }

    @Override
    public void visitPosition(WriteCharacteristicRequest<Position> request) {
        doVisit(() -> characteristic.setPositionValue(request.getValue()), "setPositionValue()");
    }

    @Override
    public void visitInteger(WriteCharacteristicRequest<Integer> request) {
        doVisit(() -> characteristic.setIntValue(request.getValue()), "setIntValue()");
    }

    @Override
    public void visitByte(WriteCharacteristicRequest<byte[]> request) {
        doVisit(() -> characteristic.setByteValue(request.getValue()), "setByteValue()");
    }

    @Override
    public void visitShort(WriteCharacteristicRequest<Short> request) {
        doVisit(() -> characteristic.setShortValue(request.getValue()), "setShortValue()");
    }

    @Override
    public void visitBoolean(WriteCharacteristicRequest<Boolean> request) {
        doVisit(() -> characteristic.setBooleanValue(request.getValue()), "setBooleanValue()");
    }

    private void doVisit(Func0<Boolean> setCharValueFunction, String setterName) {
        // try for the first time
        boolean b = setCharValueFunction.call();
        if (!b) {
            // reset the old value...
            this.characteristic.setByteValue(null);
            // ... and then set the value again
            b = setCharValueFunction.call();
        }
        if (Constants.DEBUG) {
            Preconditions.checkState(b, setterName + " failed!");
        }
    }
}
