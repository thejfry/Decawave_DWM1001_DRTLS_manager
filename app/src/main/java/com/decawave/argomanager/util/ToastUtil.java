/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.widget.Toast;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Utility class to show toast by
 */
public class ToastUtil {

    private static Toast toast;

    public static void showToast(String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    public static void showToast(String text, int duration) {
        AndroidValidate.runningOnUiThread();

        if (toast != null) {
            toast.cancel();
        }

        toast = Toast.makeText(daApp, text, duration);
        toast.show();
    }

    public static void showToast(int stringResId, int duration) {
        AndroidValidate.runningOnUiThread();

        if (toast != null) {
            toast.cancel();
        }

        toast = Toast.makeText(daApp, stringResId, duration);
        toast.show();
    }

    public static void showToast(int stringResId) {
        showToast(stringResId, Toast.LENGTH_SHORT);
    }

}
