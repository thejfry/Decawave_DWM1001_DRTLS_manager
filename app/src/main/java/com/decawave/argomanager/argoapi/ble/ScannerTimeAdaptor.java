/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import android.os.SystemClock;

import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import java.util.LinkedList;
import java.util.List;

/**
 * Argo project.
 */
class ScannerTimeAdaptor {
    private static final int THIRTY_SECONDS = 30 * 1000;
    private static final float NUMBER_OF_ATTEMPTS_PER_30S_FOR_COMPUTATION = 4.3f;
    private static final float SCAN_NO_SCAN_RATIO = 0.5f;
    // times
    private final int minScanDuration;
    private final int minSleepDuration;

    // state
    private List<Long> scanMemory;
    private Long currentScanTimestamp;


    ScannerTimeAdaptor(int minScanDuration, int minSleepDuration) {
        this.minScanDuration = minScanDuration;
        this.minSleepDuration = minSleepDuration;
        //
        this.scanMemory = new LinkedList<>();
    }

    private List<Long> getScanMemory(long now) {
        if (scanMemory.size() > 0) {
            // remove entries older than 30 seconds
            now -= THIRTY_SECONDS;
            while (!scanMemory.isEmpty() && scanMemory.get(0) <= now) {
                scanMemory.remove(0);
            }
        }
        return scanMemory;
    }

    void onScanStarted() {
        if (Constants.DEBUG) {
            Preconditions.checkState(currentScanTimestamp == null);
        }
        currentScanTimestamp = SystemClock.uptimeMillis();
    }

    void onScanStopped() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(currentScanTimestamp);
        }
        // add
        scanMemory.add(currentScanTimestamp);
        currentScanTimestamp = null;
    }

    // we must return scan time such long, that after this scan is finished and a new one
    // is started immediately, it does not violate the 5 scans in 30 seconds rule
    void getScanAndSleepTime(boolean firstLowLatencyScan, int[] scanAndSleepTime) {
        long now = SystemClock.uptimeMillis();
        List<Long> scanMemory = getScanMemory(now);
        int scanDuration = 0, sleepDuration = 0;
        if (scanMemory.size() != 0) {
            // we need to scale the remaining time
            // take the first entry
            long oldestScanAt = scanMemory.get(0);
            long remainingTime = oldestScanAt + THIRTY_SECONDS - now;
            //
            if (Constants.DEBUG) {
                // older entries were removed in getScanMemory
                Preconditions.checkState(remainingTime > 0);
            }
            // split the remaining time in intervals
            int numberOfExecutedScans = scanMemory.size();
            int oneScanInterval = (int) (remainingTime / (NUMBER_OF_ATTEMPTS_PER_30S_FOR_COMPUTATION - numberOfExecutedScans));
            //
            sleepDuration = (int) (oneScanInterval / (1 + SCAN_NO_SCAN_RATIO));
            scanDuration = oneScanInterval - sleepDuration;
            //
            if (firstLowLatencyScan && scanDuration < minScanDuration * 1.2) {
                // we can afford to respect the low latency request
                scanDuration = minScanDuration;
            }
        }
        // at least min values
        scanAndSleepTime[0] = Math.max(minScanDuration, scanDuration);
        scanAndSleepTime[1] = Math.max(minSleepDuration, sleepDuration);
    }

}
