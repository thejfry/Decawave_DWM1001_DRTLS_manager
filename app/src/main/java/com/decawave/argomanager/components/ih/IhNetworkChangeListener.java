/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import com.decawave.argomanager.ui.view.FloorPlan;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens to events associated with networks.
 */
public interface IhNetworkChangeListener extends InterfaceHubHandler {

    void onNetworkAdded(short networkId);

    void onNetworkUpdated(short networkId);

    void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction);

    void onNetworkRenamed(short networkId, String newName);

    void onFloorPlanChanged(short networkId, FloorPlan floorPlan);

}
