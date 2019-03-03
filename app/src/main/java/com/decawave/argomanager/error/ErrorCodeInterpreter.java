/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.error;

import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argomanager.Constants;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */
@SuppressWarnings("WeakerAccess")
public class ErrorCodeInterpreter {
    private static final ComponentLog log = new ComponentLog(ErrorCodeInterpreter.class);

    public static class Properties {
        public final boolean warningOnly;
        public final int thresholdConsideredMajor;

        public Properties(boolean warningOnly, int thresholdConsideredMajor) {
            this.warningOnly = warningOnly;
            this.thresholdConsideredMajor = thresholdConsideredMajor;
        }

        public boolean isSoft() {
            return thresholdConsideredMajor == Integer.MAX_VALUE;
        }
    }

    private static final Properties PROPS_WARNING_THRESHOLD_10 = new Properties(true, 10);
    private static final Properties PROPS_SOFT_WARNING = new Properties(true, Integer.MAX_VALUE);
    private static final Properties PROPS_ERROR = new Properties(false, 1);
    private static final Properties PROPS_SOFT_ERROR = new Properties(false, Integer.MAX_VALUE);

    public static @NotNull Properties interpret(int errorCode) {
        switch (errorCode) {
            case ErrorCode.DISCOVERY_FAILED:
            case ErrorCode.GATT_REPRESENTATION:
            case ErrorCode.GATT_MISSING_DESCRIPTOR:
            case ErrorCode.GATT_MISSING_CHARACTERISTIC:
            case ErrorCode.GATT_INSUFFICIENT_MTU:
            case ErrorCode.GATT_INCONSISTENT_LOCATION_DATA:
                return PROPS_ERROR;
            case ErrorCode.FAILED_UPDATE:
            case ErrorCode.FAILED_UPDATE_CHECK:
            case ErrorCode.FAILED_FIRMWARE_UPLOAD_PRECONDITION:
                return PROPS_SOFT_ERROR;
            case ErrorCode.GATT_ASYNC:
            case ErrorCode.GATT_OPERATION_INIT:
            case ErrorCode.GATT_OPERATION_TIMEOUT:
            case ErrorCode.GATT_DISCONNECT_TIMEOUT:
            case ErrorCode.GATT_FAILED_SERVICE_DISCOVERY:
            case ErrorCode.BLE_CONNECTION_DROPPED:
            case ErrorCode.BLE_CONNECT_TIMEOUT:
            case ErrorCode.BLE_READ_TIMEOUT:
            case ErrorCode.GATT_REPRESENTATION_WARN:
                return PROPS_WARNING_THRESHOLD_10;
            case ErrorCode.DISCOVERY_OVERLAP:
            case ErrorCode.GATT_MISSING_DESCRIPTOR_WARN:
            case ErrorCode.GATT_MISSING_CHARACTERISTIC_WARN:
            case ErrorCode.GATT_OBSOLETE_CALLBACK:
            case ErrorCode.AP_DISTANCE_MEASURE_TIMEOUT:
            case ErrorCode.AP_MEASURED_DISTANCE_MISSING:
            case ErrorCode.STREAM_CLOSE_ERROR:
            case ErrorCode.GATT_BROKEN:     // this cannot be avoided, therefore soft warning
                return PROPS_SOFT_WARNING;
            default:
                // unknown error code
                if (Constants.DEBUG && errorCode != ErrorCode.UNSPECIFIC) {
                    log.w("unknown error code passed: " + errorCode + ", returning default PROPS_SOFT_WARNING");
                }
                return PROPS_SOFT_WARNING;
        }
    }

    public static String getName(int errorCode) {
        String name = errorCodeNames.get(errorCode);
        return name == null ? "<UNKNOWN:" + errorCode + ">" : name;
    }
    
    @SuppressWarnings("unchecked")
    private static final Map<Integer, String> errorCodeNames = new HashMap() {{
        put(ErrorCode.DISCOVERY_FAILED,"DISCOVERY_FAILED");
        put(ErrorCode.DISCOVERY_OVERLAP,"DISCOVERY_OVERLAP");
        put(ErrorCode.GATT_REPRESENTATION,"GATT_REPRESENTATION");
        put(ErrorCode.GATT_FAILED_SERVICE_DISCOVERY,"GATT_FAILED_SERVICE_DISCOVERY");
        put(ErrorCode.GATT_MISSING_DESCRIPTOR,"GATT_MISSING_DESCRIPTOR");
        put(ErrorCode.GATT_MISSING_CHARACTERISTIC,"GATT_MISSING_CHARACTERISTIC");
        put(ErrorCode.GATT_ASYNC,"GATT_ASYNC");
        put(ErrorCode.GATT_OPERATION_INIT,"GATT_OPERATION_INIT");
        put(ErrorCode.GATT_OPERATION_TIMEOUT,"GATT_OPERATION_TIMEOUT");
        put(ErrorCode.BLE_CONNECTION_DROPPED,"BLE_CONNECTION_DROPPED");
        put(ErrorCode.BLE_CONNECT_TIMEOUT,"BLE_CONNECT_TIMEOUT");
        put(ErrorCode.BLE_READ_TIMEOUT,"BLE_READ_TIMEOUT");
        put(ErrorCode.GATT_MISSING_DESCRIPTOR_WARN,"GATT_MISSING_DESCRIPTOR_WARN");
        put(ErrorCode.GATT_MISSING_CHARACTERISTIC_WARN,"GATT_MISSING_CHARACTERISTIC_WARN");
        put(ErrorCode.GATT_OBSOLETE_CALLBACK,"GATT_OBSOLETE_CALLBACK");
        put(ErrorCode.UNSPECIFIC, "UNSPECIFIC");
    }};
    
}
