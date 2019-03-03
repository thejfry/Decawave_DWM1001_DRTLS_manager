/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argomanager.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of discrete update rates.
 */

public enum UpdateRate {
    MS_100 (100, R.string.update_rate_100ms),
    MS_200 (200, R.string.update_rate_200ms),
    MS_500 (500, R.string.update_rate_500ms),
    S_1 (  1000, R.string.update_rate_1s),
    S_2 (  2000, R.string.update_rate_2s),
    S_5 (  5000, R.string.update_rate_5s),
    S_10 (10000, R.string.update_rate_10s),
    S_30 (30000, R.string.update_rate_30s),
    M_1 ( 60000, R.string.update_rate_1m),
    DEFAULT(     0, R.string.update_rate_default);

    public final int msValue;
    public final int text;

    UpdateRate(int msValue, int text) {
        this.msValue = msValue;
        this.text = text;
    }

    private static Map<Integer, UpdateRate> hlprMap;

    public static UpdateRate getUpdateRateForValue(int msValue) {
        if (hlprMap == null) {
            hlprMap = new HashMap<>();
            for (UpdateRate updateRate : UpdateRate.values()) {
                hlprMap.put(updateRate.msValue, updateRate);
            }
        }
        return hlprMap.get(msValue);
    }
}
