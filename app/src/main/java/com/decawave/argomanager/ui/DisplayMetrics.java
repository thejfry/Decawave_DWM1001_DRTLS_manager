/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui;

import android.content.res.Resources;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 *
 */

public class DisplayMetrics {

    public static float LCD_DIP_SCALING_FACTOR;
    public static int LCD_DENSITY_DPI;
    public static float LCD_DENSITY_SCALING_FACTOR = 1.0f; // onCreate() updates it.
    public static android.util.DisplayMetrics DISPLAY_METRICS;

    public static float XDPI;

    private static boolean initialized = false;

    public static final void initDisplayMetrics() {
        if (initialized) {
            return;
        }
        Resources res = daApp.getResources();
        if (res == null) {
            throw new RuntimeException("getResourcesIsNull");
        }

        DISPLAY_METRICS = daApp.getResources().getDisplayMetrics();
        LCD_DIP_SCALING_FACTOR = DISPLAY_METRICS.density;
        LCD_DENSITY_DPI = DISPLAY_METRICS.densityDpi;
        LCD_DENSITY_SCALING_FACTOR = DISPLAY_METRICS.density;
        XDPI = DISPLAY_METRICS.xdpi;
        initialized = true;
    }

}
