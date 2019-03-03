/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.struct.PresenceStatus;

import java.util.Set;

/**
 * Presence API provides approximate information about presence of network nodes.
 * The presence is just guessed based on:
 * 1. received discovery broadcasts
 * 2. existing connections
 *
 * see {@link IhPresenceApiListener}
 */
public interface BlePresenceApi {

    void init();

    boolean isNodePresent(String nodeBleAddress);

    Set<String> getPresentNodes();

    Integer getNodeRssi(String nodeBleAddress);

    Integer getAgingNodeRssi(String nodeBleAddress);

    PresenceStatus getNodeStatus(String nodeBleAddress);

    boolean isTagTrackedViaProxy(String tagBleAddress);

    boolean isTagTrackedDirectly(String tagBleAddress);

}
