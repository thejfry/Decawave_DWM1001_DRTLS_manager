/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens to events associated with enhanced network node:
 * - last seen
 * - last updated
 * - warnings
 */
public interface IhEnhancedNodePropertiesChangeListener extends InterfaceHubHandler {

    /**
     * Used also for new nodes, and also for new declared network (no change on network node).
     */
    void onPropertiesChanged(NetworkNodeEnhanced node);

}
