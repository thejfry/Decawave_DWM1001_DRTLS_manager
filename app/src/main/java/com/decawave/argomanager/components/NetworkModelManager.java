/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * In memory representation of available networks.
 *
 * This is rather internal interface, use {@link NetworkNodeManager} on application
 * level instead. Manager also handles persistence, etc.
 */
public interface NetworkModelManager {

    void init(Collection<NetworkModel> networkMap);

    @NotNull
    Map<Short, NetworkModel> getNetworks();

    void removeNetwork(short networkId, boolean explicitUserAction);

    void addNetwork(NetworkModel newNetwork);

    void undoNetworkRemove(short networkId);

    boolean hasNetworkByName(String networkName);

    void setNetworkChangeListener(NetworkPropertyChangeListener networkPropertyChangeListener);

    boolean hasNetwork(Short oldNetworkId);
}
