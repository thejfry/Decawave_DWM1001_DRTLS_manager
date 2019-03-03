/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Anchor implementation.
 */
class AnchorNodeImpl extends NetworkNodeImpl implements AnchorNode {
    AnchorNodeImpl(Long nodeId) {
        super(nodeId, NodeType.ANCHOR);
    }

    AnchorNodeImpl(AnchorNode other) {
        super(other);
    }

    public Integer getMacStats() {
        return getProperty(NetworkNodeProperty.ANCHOR_MAC_STATS);
    }

    void setMacStats(Integer macStats) {
        setProperty(NetworkNodeProperty.ANCHOR_MAC_STATS, macStats);
    }

    @Override
    public Byte getSeatNumber() {
        return getProperty(NetworkNodeProperty.ANCHOR_SEAT);
    }

    @Override
    public Short getClusterMap() {
        return getProperty(NetworkNodeProperty.ANCHOR_CLUSTER_MAP);
    }

    @Override
    public Short getClusterNeighbourMap() {
        return getProperty(NetworkNodeProperty.ANCHOR_CLUSTER_NEIGHBOUR_MAP);
    }

    @Override
    public Position getPosition() {
        Position aPosition = getProperty(NetworkNodeProperty.ANCHOR_POSITION);
        return aPosition == null ? null : new Position(aPosition);
    }

    void setSeatNumber(Byte seatNumber) {
        setProperty(NetworkNodeProperty.ANCHOR_SEAT, seatNumber);
    }

    void setClusterMap(Short clusterMap) {
        setProperty(NetworkNodeProperty.ANCHOR_CLUSTER_MAP, clusterMap);
    }

    void setClusterNeighbourMap(Short clusterNeighbourMap) {
        setProperty(NetworkNodeProperty.ANCHOR_CLUSTER_NEIGHBOUR_MAP, clusterNeighbourMap);
    }

    @Override
    public void setPosition(Position position) {
        setProperty(NetworkNodeProperty.ANCHOR_POSITION, position);
    }

    public List<RangingAnchor> getDistances() {
        return getProperty(NetworkNodeProperty.ANCHOR_DISTANCES, true);
    }

    @Override
    public boolean anyDistance() {
        List<RangingAnchor> distances = getProperty(NetworkNodeProperty.ANCHOR_DISTANCES);
        return distances != null && distances.size() > 0;
    }

    public void setDistances(List<RangingAnchor> distances) {
        setProperty(NetworkNodeProperty.ANCHOR_DISTANCES, distances);
    }

    @Override
    public List<Short> getAnchorList() {
        List<Short> anchorList = getProperty(NetworkNodeProperty.ANCHOR_AN_LIST);
        // create a deep copy
        return anchorList == null ? null : new LinkedList<>(anchorList);
    }

    void setAnchorList(List<Short> anchorList) {
        setProperty(NetworkNodeProperty.ANCHOR_AN_LIST, anchorList);
    }

    @Override
    public Boolean isInitiator() {
        return getProperty(NetworkNodeProperty.ANCHOR_INITIATOR);
    }

    public void setInitiator(Boolean initiator) {
        setProperty(NetworkNodeProperty.ANCHOR_INITIATOR, initiator);
    }

    @Override
    public Boolean isBridge() {
        return getProperty(NetworkNodeProperty.ANCHOR_BRIDGE);
    }

    @SuppressWarnings("WeakerAccess")
    public void setBridge(Boolean bridge) {
        setProperty(NetworkNodeProperty.ANCHOR_BRIDGE, bridge);
    }

    @Override
    public boolean isPropertyRecognized(NetworkNodeProperty property) {
        switch (property) {
            case ANCHOR_INITIATOR:
            case ANCHOR_BRIDGE:
            case ANCHOR_SEAT:
            case ANCHOR_CLUSTER_MAP:
            case ANCHOR_CLUSTER_NEIGHBOUR_MAP:
            case ANCHOR_MAC_STATS:
            case ANCHOR_AN_LIST:
            case ANCHOR_POSITION:
            case ANCHOR_DISTANCES:
                return true;
            default:
                return super.isPropertyRecognized(property);
        }
    }

    @Override
    public Position extractPositionDirect() {
        return getProperty(NetworkNodeProperty.ANCHOR_POSITION);
    }

    @Nullable
    @Override
    public List<RangingAnchor> extractDistancesDirect() {
        return getProperty(NetworkNodeProperty.ANCHOR_DISTANCES);
    }


}
