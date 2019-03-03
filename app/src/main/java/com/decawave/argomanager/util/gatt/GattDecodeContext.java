/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util.gatt;

/**
 * Argo project.
 */

@SuppressWarnings("WeakerAccess")
public class GattDecodeContext {
    private GattDecoder.GattOperationMode operationMode;
    private byte[] operationModeEncoded;

    public void setOperationMode(GattDecoder.GattOperationMode operationMode) {
        this.operationMode = operationMode;
        // invalidate the encoded operation mode
        this.operationModeEncoded = null;
    }

    public void setOperationMode(byte[] operationModeEncoded) {
        this.operationModeEncoded = operationModeEncoded;
        // invalidate the cache
        this.operationMode = null;
    }

    public GattDecoder.GattOperationMode getOperationMode() {
        if (operationMode == null && operationModeEncoded != null) {
            // decode the operation mode
            operationMode = GattDecoder.decodeOperationMode(operationModeEncoded);
        }
        return operationMode;
    }
}