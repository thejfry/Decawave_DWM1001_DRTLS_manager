/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.os.Looper;

public class AndroidValidate {

    /**
     * Android specific check that the current code is not running on UI thread
     */
    public final static void notRunningOnUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("can't run on UI thread");
        }
    }

    /**
     * Android specific check that the current code is running on UI thread
     */
    public final static void runningOnUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // check that we are not in a test environment
            if (!Thread.currentThread().getName().contains("android.test.InstrumentationTestRunner")) {
                throw new IllegalStateException("must run on UI thread");
            }
        }
    }

}
