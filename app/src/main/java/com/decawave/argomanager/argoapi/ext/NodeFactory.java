/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import android.support.annotation.NonNull;

import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeStatistics;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Factory for creating node instances.
 */

public class NodeFactory {

    @SuppressWarnings("unchecked")
    public static @NotNull <T extends NetworkNode> T newNodeCopy(@NotNull T node) {
        NodeType nodeType = node.getType();
        switch (nodeType) {
            case ANCHOR:
                return (T) new AnchorNodeImpl((AnchorNode) node);
            case TAG:
                return (T) new TagNodeImpl((TagNode) node);
            default:
                throw new IllegalArgumentException("unexpected node type: " + node);
        }
    }

    private static NetworkNode newNode(NodeType type, long nodeId) {
        switch (type) {
            case ANCHOR:
                return new AnchorNodeImpl(nodeId);
            case TAG:
                return new TagNodeImpl(nodeId);
            default:
                throw new IllegalArgumentException("unexpected node type: " + type);
        }
    }

    /**
     * Creates a node copy: node type + id + ble address and any other additional properties.
     * @param node network node
     * @param extraProperties additional properties to copy
     * @return copy of the given network node
     */
    public static NodeBuilder getBasicCopyBuilder(NetworkNode node, NetworkNodeProperty... extraProperties) {
        NodeBuilder builder = newBuilder(node.getType(), node.getId()).setBleAddress(node.getBleAddress());
        for (NetworkNodeProperty property : extraProperties) {
            if (node.isPropertyInitialized(property)) {
                builder.setProperty(property, node.getProperty(property));
            }
        }
        return builder;
    }

    public static NodeBuilder newBuilder(@NotNull NodeType nodeType, @Nullable Long nodeId) {
        switch (nodeType) {
            case ANCHOR:
                return newAnchorBuilder(nodeId);
            case TAG:
                return newTagBuilder(nodeId);
            default:
                throw new IllegalArgumentException("unexpected nodeType = " + nodeType);
        }
    }

    @NonNull
    private static TagNodeBuilder newTagBuilder(Long nodeId) {
        return new TagNodeBuilder(new TagNodeImpl(nodeId));
    }

    @NonNull
    public static AnchorNodeBuilder newAnchorBuilder(Long nodeId) {
        return new AnchorNodeBuilder(new AnchorNodeImpl(nodeId));
    }

    public static NetworkNode newDiffingWrapper(NetworkNode node) {
        if (node == null) {
            return null;
        }
        NodeType type = node.getType();
        switch (type) {
            case ANCHOR:
                return new AnchorNodeDiffingWrapper((AnchorNode) node);
            case TAG:
                return new TagNodeDiffingWrapper((TagNode) node);
            default:
                throw new IllegalArgumentException("unsupported node type: " + type);
        }
    }

    public static NetworkNode newDiffingWrapper(long nodeId, NodeType type) {
        return newDiffingWrapper(newNode(type, nodeId));
    }

    @SuppressWarnings("unchecked")
    public abstract static class NodeBuilder<T extends NetworkNodeImpl,U extends NodeBuilder> {
        T networkNode;

        private NodeBuilder(@NotNull T networkNode) {
            this.networkNode = networkNode;
        }

        /**
         * Generic call to set a property.
         * @param property identifies property to be set
         * @param value value of a property
         * @return builder
         */
        public U setProperty(NetworkNodeProperty property, Object value) {
            networkNode.setProperty(property, value);
            return (U) this;
        }

        public U setNetworkId(Short networkId) {
            networkNode.setNetworkId(networkId);
            return (U) this;
        }

        public U setUwbMode(UwbMode uwbMode) {
            networkNode.setUwbMode(uwbMode);
            return (U) this;
        }

        public U setBleAddress(String bleAddress) {
            networkNode.setBleAddress(bleAddress);
            return (U) this;
        }

        public U setLabel(String label) {
            networkNode.setLabel(label);
            return (U) this;
        }

        public U setPassword(String password) {
            networkNode.setPassword(password);
            return (U) this;
        }

        public @NotNull T build() {
            T n = this.networkNode;
            this.networkNode = null;
            return n;
        }

        public U setHwVersion(Integer hwVersion) {
            networkNode.setHwVersion(hwVersion);
            return (U) this;
        }

        public U setFw1Version(Integer fwVersion) {
            networkNode.setFw1Version(fwVersion);
            return (U) this;
        }

        public U setFw2Version(Integer fwVersion) {
            networkNode.setFw2Version(fwVersion);
            return (U) this;
        }

        public U setFw1Checksum(Integer fwChecksum) {
            networkNode.setFw1Checksum(fwChecksum);
            return (U) this;
        }

        public U setFw2Checksum(Integer fwChecksum) {
            networkNode.setFw2Checksum(fwChecksum);
            return (U) this;
        }

        public U setFirmwareUpdateEnable(Boolean enable) {
            networkNode.setFirmwareUpdateEnable(enable);
            return (U) this;
        }

        public U setLedIndicationEnable(Boolean enable) {
            networkNode.setLedIndicationEnable(enable);
            return (U) this;
        }

        public U setNodeStatistics(NodeStatistics statistics) {
            networkNode.setNodeStatistics(statistics);
            return (U) this;
        }

        public U setLocationDataMode(LocationDataMode locationDataMode) {
            networkNode.setLocationDataMode(locationDataMode);
            return (U) this;
        }

        public U setOperatingFirmware(OperatingFirmware operatingFirmware) {
            networkNode.setOperatingFirmware(operatingFirmware);
            return (U) this;
        }

        public boolean isAnchor() {
            return networkNode.isAnchor();
        }

    }

    public static class AnchorNodeBuilder extends NodeBuilder<AnchorNodeImpl,AnchorNodeBuilder> {

        private AnchorNodeBuilder(@NotNull AnchorNodeImpl networkNode) {
            super(networkNode);
        }

        public AnchorNodeBuilder setInitiator(Boolean initiator) {
            networkNode.setInitiator(initiator);
            return this;
        }

        public AnchorNodeBuilder setPosition(Position position) {
            networkNode.setPosition(position);
            return this;
        }

        public AnchorNodeBuilder setDistances(List<RangingAnchor> distances) {
            networkNode.setDistances(distances);
            return this;
        }

        public AnchorNodeBuilder setBridge(Boolean bridge) {
            networkNode.setBridge(bridge);
            return this;
        }

        public AnchorNodeBuilder setSeatNumber(Byte seatNumber) {
            networkNode.setSeatNumber(seatNumber);
            return this;
        }

        public AnchorNodeBuilder setClusterMap(Short clusterMap) {
            networkNode.setClusterMap(clusterMap);
            return this;
        }

        public AnchorNodeBuilder setClusterNeighbourMap(Short clusterNeighbourMap) {
            networkNode.setClusterNeighbourMap(clusterNeighbourMap);
            return this;
        }

        public AnchorNodeBuilder setMacStats(Integer integer) {
            networkNode.setMacStats(integer);
            return this;
        }

    }

    public static class TagNodeBuilder extends NodeBuilder<TagNodeImpl,TagNodeBuilder> {

        private TagNodeBuilder(@NotNull TagNodeImpl networkNode) {
            super(networkNode);
        }

        public TagNodeBuilder setUpdateRate(Integer updateRate) {
            networkNode.setUpdateRate(updateRate);
            return this;
        }

        public TagNodeBuilder setStationaryUpdateRate(Integer updateRate) {
            networkNode.setStationaryUpdateRate(updateRate);
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public TagNodeBuilder setAccelerometerEnable(Boolean enable) {
            networkNode.setAccelerometerEnable(enable);
            return this;
        }

        public TagNodeBuilder setLocationEngineEnable(Boolean enable) {
            networkNode.setLocationEngineEnable(enable);
            return this;
        }

        public TagNodeBuilder setLowPowerModeEnable(Boolean enable) {
            networkNode.setLowPowerModeEnable(enable);
            return this;
        }

        public TagNodeBuilder setLocationData(LocationData locationData) {
            networkNode.setLocationData(locationData);
            return this;
        }
    }


}
