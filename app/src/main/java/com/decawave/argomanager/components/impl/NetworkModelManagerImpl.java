/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkModelManager;
import com.decawave.argomanager.components.NetworkPropertyChangeListener;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;

/**
 * In-memory network repository.
 */
public class NetworkModelManagerImpl implements NetworkModelManager {
    public static final ComponentLog log = new ComponentLog(NetworkModelManagerImpl.class).disable();
    private static final Comparator<NetworkModel> COMPARATOR_BY_NAME = (o1, o2) -> o1.getNetworkName().compareTo(o2.getNetworkName());

    private Map<Short, NetworkModel> networkMap;

    private Map<Short, NetworkModel> removedNetworks = new HashMap<>();

    private NetworkPropertyChangeListener systemListener;

    @Inject
    NetworkModelManagerImpl() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // API

    @Override
    public void setNetworkChangeListener(NetworkPropertyChangeListener networkPropertyChangeListener) {
        systemListener = networkPropertyChangeListener;
    }

    @Override
    public void init(Collection<NetworkModel> networks) {
        // sort the networks by name
        networkMap = sortNetworksByName(networks);
        // set up change listener with each network
        for (NetworkModel networkModel : networkMap.values()) {
            networkModel.setChangeListener(systemListener);
        }
    }

    @Override
    public @NotNull Map<Short, NetworkModel> getNetworks() {
        if (networkMap == null) {
            return Collections.emptyMap();
        }
        // we need to have control over the collection
        return Collections.unmodifiableMap(networkMap);
    }
    
    @Override
    public void removeNetwork(short networkId, boolean explicitUserAction) {
        if (Constants.DEBUG) {
            log.d("removeNetwork() called with: " + "networkId = [" + networkId + "]");
        }
        NetworkModel n = networkMap.remove(networkId);
        if (n != null) {
            removedNetworks.put(n.getNetworkId(), n);
            // reset change listener
            n.setChangeListener(null);
            systemListener.onNetworkRemoved(networkId, n.getNetworkName(), explicitUserAction);
        }
    }

    @Override
    public void addNetwork(NetworkModel newNetwork) {
        if (Constants.DEBUG) {
            Preconditions.checkState(newNetwork.getNetworkId() != 0, "network ID cannot be 0!");
        }
        NetworkModel oldVal = networkMap.put(newNetwork.getNetworkId(), newNetwork);
        newNetwork.setChangeListener(systemListener);
        networkMap = sortNetworksByName(networkMap.values());
        if (oldVal == null) {
            systemListener.onNetworkAdded(newNetwork.getNetworkId());
        } else {
            systemListener.onNetworkUpdated(newNetwork.getNetworkId());
        }
    }

    @Override
    public void undoNetworkRemove(short networkId) {
        if (Constants.DEBUG) {
            log.d("undoNetworkRemove() called with: " + "networkId = [" + networkId + "]");
        }
        NetworkModel n = removedNetworks.get(networkId);
        if (n != null) {
            networkMap.put(n.getNetworkId(), n);
            // set change listener again
            n.setChangeListener(systemListener);
            systemListener.onNetworkAdded(networkId);
        }
    }

    @Override
    public boolean hasNetworkByName(String networkName) {
        for (NetworkModel networkModel : networkMap.values()) {
            if (networkModel.getNetworkName().equals(networkName)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean hasNetwork(Short networkId) {
        return networkMap.containsKey(networkId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // internal methods
    private Map<Short, NetworkModel> sortNetworksByName(Collection<NetworkModel> _networks) {
        List<NetworkModel> networks = new ArrayList<>(_networks);
        // order the networks alphabetically
        Collections.sort(networks, COMPARATOR_BY_NAME);
        // now create a new linked map where the networks are ordered by name
        Map<Short, NetworkModel> r = new LinkedHashMap<>();
        for (NetworkModel networkModel : networks) {
            r.put(networkModel.getNetworkId(), networkModel);
        }
        return r;
    }

}
