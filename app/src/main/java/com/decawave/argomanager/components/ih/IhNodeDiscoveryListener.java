/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import com.decawave.argo.api.struct.NetworkNode;

import org.jetbrains.annotations.NotNull;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * These events are about transient nodes only.
 *
 * For events about persistent nodes use {@link IhPersistedNodeChangeListener}.
 */
public interface IhNodeDiscoveryListener extends InterfaceHubHandler {

    void onNodeDiscovered(@NotNull NetworkNode node);

    void onDiscoveredNodeUpdate(@NotNull NetworkNode node);

    void onDiscoveredNodeRemoved(long nodeId);

}
