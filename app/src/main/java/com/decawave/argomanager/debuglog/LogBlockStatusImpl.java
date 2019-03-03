/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Argo project.
 */
public class LogBlockStatusImpl implements LogBlockStatus {
    //
    private Map<String, Severity> blockedDevices;

    @Inject
    LogBlockStatusImpl() {
        this.blockedDevices = new HashMap<>();
    }

    @Override
    public void blockDeviceLog(String deviceBleAddress, Severity upTo) {
        blockedDevices.put(deviceBleAddress, upTo);
    }

    @Override
    public void unblockDeviceLog(String deviceBleAddress) {
        blockedDevices.remove(deviceBleAddress);
    }

    @Override
    public boolean isDeviceLogBlocked(String bleDeviceAddress) {
        return blockedDevices.containsKey(bleDeviceAddress);
    }

    @Override
    public Severity getBlockSeverity(String bleDeviceAddress) {
        return blockedDevices.get(bleDeviceAddress);
    }
}
