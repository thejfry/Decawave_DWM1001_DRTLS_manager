/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.error.DeviceErrors;

import java.util.List;

/**
 * Manages errors/warnings which occurred during application lifetime.
 */
public interface ErrorManager {

    /**
     * @return either full error list (application mode ADVANCED) or reduced error list (application mode SIMPLE)
     */
    List<DeviceErrors> getErrors();

    void reportError(String deviceBleAddress, Throwable exception, String message, int errorCode);

    void removeDeviceErrors(String deviceBleAddress);

    boolean anyUnreadError();

    void markErrorsAsRead();

    void clearErrors();

}
