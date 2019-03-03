/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.exception;

/**
 * Generic GATT representation problem.
 */
public class GattRepresentationException extends RuntimeException {
    private final String deviceBleAddress;

    protected GattRepresentationException(String deviceBleAddress) {
        this.deviceBleAddress = deviceBleAddress;
    }

    public GattRepresentationException(String deviceBleAddress, String message) {
        super(message);
        this.deviceBleAddress = deviceBleAddress;
    }

    public GattRepresentationException(String deviceBleAddress, String message, Throwable cause) {
        super(message, cause);
        this.deviceBleAddress = deviceBleAddress;
    }

    public GattRepresentationException(String deviceBleAddress, Throwable cause) {
        super(cause);
        this.deviceBleAddress = deviceBleAddress;
    }

    public String getDeviceBleAddress() {
        return deviceBleAddress;
    }
}
