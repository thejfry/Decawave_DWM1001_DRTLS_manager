/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;

/**
 * The only responsibility of this observer is to get more frequent location
 * data updates of TAG positions than the default ones (caused by discovery).
 * The updates are passed onto network repository from where they can be
 * intercepted by {@link IhPersistedNodeChangeListener}.
 *
 * It does by:
 * 1. starting stateful discovery on the background (if necessary)
 * 2. if suitable establish connection and intercept position and distance changes
 * 3. propagates the received location data updates to network repository as node updates
 * 4. the updates are therefore interceptable on network via
 *    {@link IhPersistedNodeChangeListener}
 *
 *
 */
public interface LocationDataObserver {

    void setPreferentiallyObservedNode(String bleAddress);

    void startObserve();

    void stopObserve();

    boolean isObserving();
}
