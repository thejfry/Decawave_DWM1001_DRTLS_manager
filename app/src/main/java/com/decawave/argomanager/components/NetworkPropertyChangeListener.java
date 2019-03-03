/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.ui.view.FloorPlan;

/**
 * System network change listener.
 */
public interface NetworkPropertyChangeListener {

    void onNetworkAdded(short networkId);

    /**
     * Some network properties have changed.
     */
    void onNetworkUpdated(short networkId);

    void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction);

    void onNetworkRenamed(short networkId, String newName);

    void onFloorPlanChanged(short networkId, FloorPlan floorPlan);

}
