/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

/**
 * Keeps device log block status.
 *
 * We need to keep this separate from LogEntryCollector because of circular dependency.
 */
public interface LogBlockStatus {

    void blockDeviceLog(String deviceBleAddress, Severity upTo);

    void unblockDeviceLog(String deviceBleAddress);

    boolean isDeviceLogBlocked(String bleDeviceAddress);

    Severity getBlockSeverity(String bleDeviceAddress);

}
