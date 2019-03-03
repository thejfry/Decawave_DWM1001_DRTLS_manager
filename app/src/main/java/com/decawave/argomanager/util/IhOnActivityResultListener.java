/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.content.Intent;

import com.decawave.argomanager.ui.MainActivity;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Argo project.
 */

public interface IhOnActivityResultListener extends InterfaceHubHandler {

    void onActivityResult(MainActivity mainActivity, int requestCode, int resultCode, Intent data);

    void onRequestPermissionsResult(MainActivity mainActivity, int requestCode, String[] permissions, int[] grantResults);

}
