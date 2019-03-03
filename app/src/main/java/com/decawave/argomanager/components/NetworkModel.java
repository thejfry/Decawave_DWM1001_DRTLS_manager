/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.ui.view.FloorPlan;
import com.google.common.base.Objects;

/**
 * Represents an ARGO network properties.
 *
 * Nodes are kept in network node manager.
 */
public class NetworkModel {
    // id of the network - as it is advertised
    public final short networkId;
    // modifiable members
    private String networkName;
    // nodes
    private FloorPlan floorPlan;
    //
    private NetworkPropertyChangeListener changeListener;

    public NetworkModel(short networkId) {
        this(networkId, null);
    }


    public NetworkModel(short networkId, String networkName) {
        this.networkId = networkId;
        this.networkName = networkName;
        this.changeListener = VOID_LISTENER;
    }

    public void setChangeListener(NetworkPropertyChangeListener changeListener) {
        this.changeListener = changeListener == null ? VOID_LISTENER : changeListener;
    }

    public short getNetworkId() {
        return networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
        this.changeListener.onNetworkRenamed(networkId, networkName);
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        FloorPlan oldFp = this.floorPlan;
        this.floorPlan = floorPlan;
        if (!Objects.equal(floorPlan, oldFp)) {
            // notify
            changeListener.onFloorPlanChanged(networkId, floorPlan);
        }
    }

    public FloorPlan getFloorPlan() {
        return floorPlan;
    }

    public String getNetworkName() {
        return networkName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkModel that = (NetworkModel) o;

        //noinspection SimplifiableIfStatement
        if (networkId != that.networkId) return false;
        return networkName != null ? networkName.equals(that.networkName) : that.networkName == null;

    }

    @Override
    public int hashCode() {
        int result = (int) networkId;
        result = 31 * result + (networkName != null ? networkName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkModel{" +
                "networkId='" + networkId + '\'' +
                ", networkName='" + networkName + '\'' +
                '}';
    }

    private static NetworkPropertyChangeListener VOID_LISTENER = new NetworkPropertyChangeListener() {

        @Override
        public void onNetworkAdded(short networkId) {

        }

        @Override
        public void onNetworkUpdated(short networkId) {

        }

        @Override
        public void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction) {

        }

        @Override
        public void onNetworkRenamed(short networkId, String newName) {

        }

        @Override
        public void onFloorPlanChanged(short networkId, FloorPlan floorPlan) {

        }

    };

}
