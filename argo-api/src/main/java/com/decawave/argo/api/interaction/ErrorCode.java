/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.interaction;

/**
 * Error codes reported by GATT interaction FSM.
 */
public class ErrorCode {
    // no error (optional, same as null, to explicitly state, that everything went smoothly)
    public static final int NO_ERROR = 0;
    // related to BLE advertisements
    public static final int DISCOVERY_FAILED = 1;
    public static final int DISCOVERY_OVERLAP = 2;

    public static final int GATT_REPRESENTATION = 10;
    public static final int GATT_REPRESENTATION_WARN = 11;
    public static final int GATT_FAILED_SERVICE_DISCOVERY = 12;
    public static final int GATT_ASYNC = 13; // explicitly reported async operation error
    public static final int GATT_OPERATION_INIT = 14;
    public static final int GATT_OPERATION_TIMEOUT = 15;
    public static final int GATT_DISCONNECT_TIMEOUT = 16;
    public static final int GATT_MISSING_DESCRIPTOR = 17;
    public static final int GATT_MISSING_DESCRIPTOR_WARN = 18;
    public static final int GATT_MISSING_CHARACTERISTIC = 19;
    public static final int GATT_MISSING_CHARACTERISTIC_WARN = 20;
    public static final int GATT_OBSOLETE_CALLBACK = 21;
    public static final int GATT_INSUFFICIENT_MTU = 22;
    public static final int GATT_INCONSISTENT_LOCATION_DATA = 23;
    public static final int GATT_BROKEN = 30;
    // BLE connection related
    public static final int BLE_CONNECTION_DROPPED = 100;
    public static final int BLE_CONNECT_TIMEOUT = 101;
    public static final int BLE_READ_TIMEOUT = 102;

    //// application level
    // user initiated update
    public static final int FAILED_UPDATE = 200;
    public static final int FAILED_UPDATE_CHECK = 201;
    public static final int STREAM_CLOSE_ERROR = 202;
    public static final int AP_DISTANCE_MEASURE_TIMEOUT = 203;
    public static final int AP_MEASURED_DISTANCE_MISSING = 204;
    // firmware update
    public static final int FAILED_FIRMWARE_UPLOAD_BASE = 300;
    public static final int FAILED_FIRMWARE_UPLOAD_PRECONDITION = 600;
    //
    public static final int UNSPECIFIC = 1000;

}
