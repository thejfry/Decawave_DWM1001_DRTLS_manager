/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.signal;

import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import javax.inject.Inject;

import static com.decawave.argomanager.ArgoApp.log;

/**
 * Argo project.
 */

public class SignalStrengthInterpreterImpl implements SignalStrengthInterpreter {
    public static final int VERY_LOW_RSSI = -90;
    private static final int DEFAULT_SMALLEST_RSSI = -85;
    public static final int DEFAULT_LARGEST_RSSI = -70;
    private static final int PARTS = SignalStrength.values().length - 1;
    // members
    private int smallestRssi = DEFAULT_SMALLEST_RSSI;
    private int largestRssi = DEFAULT_LARGEST_RSSI;

    @Inject
    SignalStrengthInterpreterImpl() {
    }

    @Override
    public SignalStrength asSignalStrength(Integer rssi) {
        if (rssi == null) {
            return null;
        }
        if (rssi < VERY_LOW_RSSI) {
            return SignalStrength.VERY_LOW;
        } // else:
        int lo = getSmallestRssiSeen(rssi);
        int hi = getLargestRssiSeen(rssi);
        // apply the maths
        int interval = -lo + hi;
        float delta = 1f * interval / PARTS;
        int fromStart = -lo + rssi;
        if (fromStart > 0) {
            // do not let overflow the index above the array length
            fromStart--;
        }
        //
        int i = (int) (fromStart / delta);
        if (i < 0 || i >= PARTS) {
            log.w("wrong idx! rssi = " + rssi + ", lo = " + lo + ", hi = " + hi + ", fromStart = " + fromStart + ", delta = " + delta + ", interval = " + interval);
        }
        return SignalStrength.values()[i + 1];
    }

    private int getLargestRssiSeen(int rssi) {
        if (rssi > largestRssi) {
            largestRssi = rssi;
        }
        return largestRssi;
    }

    private int getSmallestRssiSeen(int rssi) {
        if (Constants.DEBUG) {
            Preconditions.checkState(rssi >= VERY_LOW_RSSI, "rssi = " + rssi);
        }
        // process the value
        if (rssi < smallestRssi) {
            smallestRssi = rssi;
        }
        return smallestRssi;
    }


}
