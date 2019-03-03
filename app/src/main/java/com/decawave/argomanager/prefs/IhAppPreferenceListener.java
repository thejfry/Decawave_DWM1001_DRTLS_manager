/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listener for app preference changes.
 *
 * For dataset preference listener see:
 */
public interface IhAppPreferenceListener extends InterfaceHubHandler {

    void onPreferenceChanged(AppPreference.Element element, Object oldValue, Object newValue);

}
