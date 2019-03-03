/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * List of errors associated with the given BLE device.
 */
public class DeviceErrors {
    public final String deviceBleAddress;
    private List<ErrorDetail> errors;

    public DeviceErrors(String deviceBleAddress) {
        this.deviceBleAddress = deviceBleAddress;
        this.errors = new LinkedList<>();
    }

    public DeviceErrors(String deviceBleAddress, ErrorDetail error) {
        this(deviceBleAddress);
        this.errors.add(error);
    }

    public void addError(ErrorDetail errorDetail) {
        errors.add(errorDetail);
    }

    public List<ErrorDetail> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    private static class ErrorDetailHash {
        int errorCode;
        String message;

        ErrorDetailHash(int errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ErrorDetailHash that = (ErrorDetailHash) o;

            //noinspection SimplifiableIfStatement
            if (errorCode != that.errorCode) return false;
            return message != null ? message.equals(that.message) : that.message == null;

        }

        @Override
        public int hashCode() {
            int result = errorCode;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }
    }

    public boolean anyUnreadMajorError() {
        return anyUnreadErrorGeneric(false);

    }

    public boolean anyUnreadHardError() {
        return anyUnreadErrorGeneric(true);
    }

    private boolean anyUnreadErrorGeneric(boolean hardErrorOnly) {
        // aggregate the same, unread errors
        Map<ErrorDetailHash, Integer> errorCounter = new HashMap<>();
        for (ErrorDetail error : errors) {
            if (error.isRead()) {
                // skip this one
                continue;
            }
            if (!error.getProperties().warningOnly) {
                // this is serious error
                return true;
            } else if (!hardErrorOnly) {
                // evaluate threshold
                ErrorDetailHash edh = new ErrorDetailHash(error.errorCode, error.message);
                int counter = 0;
                if (errorCounter.containsKey(edh)) {
                    counter = errorCounter.get(edh);
                }
                errorCounter.put(edh, ++counter);
                //noinspection RedundantIfStatement
                if (counter == error.getProperties().thresholdConsideredMajor) {
                    return true;
                }
            }
        }
        return false;
    }

}
