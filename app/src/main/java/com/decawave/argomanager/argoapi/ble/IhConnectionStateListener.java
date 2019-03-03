/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens to connect/disconnect events.
 */
public interface IhConnectionStateListener extends InterfaceHubHandler {

    void onConnecting(String bleAddress);

    void onConnected(String bleAddress);

    void onDisconnecting(String bleAddress);

    /**
     *
     * @param bleAddress device which was disconnected
     * @param sessionSuccess if true, session was without errors, if false there were session errors, if null
     *                       the connection request was cancelled before it even started
     */
    void onDisconnected(String bleAddress, Boolean sessionSuccess);

}
