/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Argo project.
 */
public interface IhPresenceApiListener extends InterfaceHubHandler {

    void onNodePresent(String nodeBleAddress);

    void onNodeMissing(String nodeBleAddress);

    void onNodeRssiChanged(String bleAddress, int rssi);

    void onTagDirectObserve(String bleAddress, boolean observe);
}
