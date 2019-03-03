/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import java.util.Collection;

import rx.functions.Action2;

/**
 * Network model storage/persistence.
 */
public interface NetworksNodesStorage {

    void save(Collection<NetworkNodeEnhanced> nodes,
              Collection<NetworkModel> networks);

    void load(Action2<Collection<NetworkNodeEnhanced>, Collection<NetworkModel>> callback);
}
