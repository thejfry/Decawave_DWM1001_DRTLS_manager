/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

/**
 * Internal system listener.
 */
interface TransientNodeChangeHandler {

    /**
     * Enter event. Used also for new nodes.
     */
    void onNodeUpdatedAndBecameTransient(NetworkNodeEnhanced node);

    boolean nodeAboutToBePersisted(String bleAddress);

    void onNodeUpdatedAndBecamePersistent(String bleAddress);

    void onNodeUpdated(NetworkNodeEnhanced node);

    void onNetworkRemovedNodeBecameTransient(NetworkNodeEnhanced node);

}
