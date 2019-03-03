/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.content.Context;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworksNodesStorage;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.NetworkNodeEnhancedImpl;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action2;

/**
 * Network model storage/persistence.
 *
 * Simple straightforward - GSON.
 */
public class NetworksNodesStorageImpl implements NetworksNodesStorage {
    //
    private static final String FILE_NAME = "networks-nodes.json";
    //
    public static final ComponentLog log = new ComponentLog(NetworksNodesStorageImpl.class);
    /////////////////////////////////////////////////////////////////////////////////////////////////////
    // persistence magic
    private final Gson GSON = new Gson();

    @Inject
    NetworksNodesStorageImpl() {
    }


    @Override
    public void save(@NotNull Collection<NetworkNodeEnhanced> nodes,
                     @NotNull Collection<NetworkModel> networks) {
        List<PersistedNetwork> pNetworks = new ArrayList<>(networks.size());
        for (NetworkModel networkModel : networks) {
            pNetworks.add(PersistedNetwork.fromNetworkModel(networkModel));
        }
        List<PersistedNetworkNode> pNodes = new ArrayList<>(nodes.size());
        for (NetworkNodeEnhanced node : nodes) {
            pNodes.add(PersistedNetworkNode.fromNetworkNode(node));
        }
        PersistedConfiguration persistedConfiguration = new PersistedConfiguration();
        persistedConfiguration.networks = pNetworks;
        persistedConfiguration.nodes = pNodes;
        String jsonStr = GSON.toJson(persistedConfiguration);
        if (Constants.DEBUG) {
            log.d("persisting application configuration");
        }
        try {
            FileOutputStream fileOutputStream = ArgoApp.daApp.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            bw.write(jsonStr);
            bw.close();
        } catch (IOException e) {
            throw new IllegalStateException("should not occur");
        }
    }

    @Override
    public void load(Action2<Collection<NetworkNodeEnhanced>, Collection<NetworkModel>> callback) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = ArgoApp.daApp.openFileInput(FILE_NAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            log.i(FILE_NAME + " not found, returning empty repository");
            callback.call(new LinkedList<>(), new LinkedList<>());
            return;
        } catch (IOException e) {
            throw new IllegalStateException("should not occur");
        }
        String jsonStr = sb.toString();
        log.d("loading networks");
        PersistedConfiguration configuration = GSON.fromJson(jsonStr, PersistedConfiguration.class);
        // transform to application level objects
        List<NetworkModel> networks = new LinkedList<>();
        List<NetworkNodeEnhanced> nodes = new LinkedList<>();
        if (configuration != null) {
            for (PersistedNetwork network : configuration.networks) {
                networks.add(network.toNetworkModel());
            }
            for (PersistedNetworkNode node : configuration.nodes) {
                nodes.add(node.toNetworkNode());
            }
        }
        callback.call(nodes, networks);
    }

    /**
     * Complete application configuration is made
     */
    private static class PersistedConfiguration {
        List<PersistedNetwork> networks;
        List<PersistedNetworkNode> nodes;
    }

    private static class PersistedFloorPlan {
        int pxCenterX;
        int pxCenterY;
        int rotation;
        int tenMetersInPixels;
        //
        String floorPlanFileName;

        static PersistedFloorPlan fromFloorPlan(FloorPlan floorPlan) {
            PersistedFloorPlan fp = new PersistedFloorPlan();
            fp.pxCenterX = floorPlan.pxCenterX;
            fp.pxCenterY = floorPlan.pxCenterY;
            fp.rotation = floorPlan.rotation;
            fp.tenMetersInPixels = floorPlan.tenMetersInPixels;
            fp.floorPlanFileName = floorPlan.getFloorPlanFileName();
            return fp;
        }

        FloorPlan toFloorPlan() {
            return new FloorPlan(floorPlanFileName, pxCenterX, pxCenterY, rotation, tenMetersInPixels);
        }

    }

    /**
     * To have controlled way of serialization (regardless changes in network node API).
     */
    private static class PersistedNetwork {
        short networkId;
        String networkName;
        PersistedFloorPlan floorPlan;

        static PersistedNetwork fromNetworkModel(NetworkModel network) {
            PersistedNetwork n = new PersistedNetwork();
            n.networkId = network.getNetworkId();
            n.networkName = network.getNetworkName();
            if (network.getFloorPlan() != null) {
                n.floorPlan = PersistedFloorPlan.fromFloorPlan(network.getFloorPlan());
            }
            return n;
        }

        NetworkModel toNetworkModel() {
            NetworkModel r = new NetworkModel(networkId, networkName);
            if (floorPlan != null) {
                r.setFloorPlan(floorPlan.toFloorPlan());
            }
            return r;
        }
    }

    /**
     * To have controlled way of serialization (regardless changes in network node API).
     */
    private static class PersistedNetworkNode {
        // flat model
        long nodeId;
        NodeType nodeType;

        String label;
        String bleAddress;
        Short networkId;
        Integer hwVersion;
        Integer fw1Version;
        Integer fw1Checksum;
        Integer fw2Version;
        Integer fw2Checksum;
        Boolean firmwareUpdateEnable;
        Boolean ledIndicationEnable;
        OperatingFirmware operatingFirmware;

        // anchor-specific
        Position position;
        UwbMode uwbMode;
        Boolean initiator;
        Boolean bridge;
        Byte seatNumber;
        Short clusterMap;
        Short clusterNeighbourMap;
        Integer macStats;

        // seat number, cluster map, cluster neighbour map is transient - we do not persist it
        // anchor list and ranging anchors are also transient
        // MAC stats is also transient

        // tag specific
        Integer updateRate;
        Integer stationaryUpdateRate;
        Boolean accelerometerEnable;
        Boolean locationEngineEnable;
        Boolean lowPowerModeEnable;


        // UI state
        Long lastSeen;
        TrackMode trackMode;

        static PersistedNetworkNode fromNetworkNode(@NotNull NetworkNodeEnhanced nne) {
            PersistedNetworkNode r = new PersistedNetworkNode();
            NetworkNode node = nne.asPlainNode();
            r.nodeId = node.getId();
            r.nodeType = node.getType();
            r.networkId = node.getNetworkId();
            r.bleAddress = node.getBleAddress();
            r.label = node.getLabel();
            r.uwbMode = node.getUwbMode();
            r.hwVersion = node.getHwVersion();
            r.fw1Version = node.getFw1Version();
            r.fw1Checksum = node.getFw1Checksum();
            r.fw2Version = node.getFw2Version();
            r.fw2Checksum = node.getFw2Checksum();
            r.firmwareUpdateEnable = node.isFirmwareUpdateEnable();
            r.ledIndicationEnable = node.isLedIndicationEnable();
            r.operatingFirmware = node.getOperatingFirmware();
            if (node.isAnchor()) {
                // anchor specific stuff
                AnchorNode anchor = (AnchorNode) node;
                r.position = anchor.getPosition();        // position of a tag changes too dynamically
                r.initiator = anchor.isInitiator();
                r.bridge = anchor.isBridge();
                r.seatNumber = anchor.getSeatNumber();
                r.clusterMap = anchor.getClusterMap();
                r.clusterNeighbourMap = anchor.getClusterNeighbourMap();
                r.macStats = anchor.getMacStats();
                r.trackMode = null;
            } else if (node.isTag()) {
                r.updateRate = ((TagNode) node).getUpdateRate();
                r.stationaryUpdateRate = ((TagNode) node).getStationaryUpdateRate();
                r.lowPowerModeEnable = ((TagNode) node).isLowPowerModeEnable();
                r.locationEngineEnable = ((TagNode) node).isLocationEngineEnable();
                r.accelerometerEnable = ((TagNode) node).isAccelerometerEnable();
                r.trackMode = nne.getTrackMode();
            }
            // enhanced node properties
            r.lastSeen = nne.getLastSeen();
            return r;
        }

        NetworkNodeEnhanced toNetworkNode() {
            NodeFactory.NodeBuilder nodeBuilder = NodeFactory.newBuilder(nodeType, nodeId)
                    .setNetworkId(networkId)
                    .setBleAddress(bleAddress)
                    .setUwbMode(uwbMode)
                    .setLabel(label)
                    .setHwVersion(hwVersion)
                    .setFw1Version(fw1Version)
                    .setFw2Version(fw2Version)
                    .setFw1Checksum(fw1Checksum)
                    .setFw2Checksum(fw2Checksum)
                    .setFirmwareUpdateEnable(firmwareUpdateEnable)
                    .setLedIndicationEnable(ledIndicationEnable)
                    .setOperatingFirmware(operatingFirmware);
            if (nodeType == NodeType.ANCHOR) {
                NodeFactory.AnchorNodeBuilder anchorBuilder = (NodeFactory.AnchorNodeBuilder) nodeBuilder;
                anchorBuilder.setInitiator(initiator)
                        .setBridge(bridge)
                        .setSeatNumber(seatNumber)
                        .setPosition(position)
                        .setClusterMap(clusterMap)
                        .setClusterNeighbourMap(clusterNeighbourMap)
                        .setMacStats(macStats);
            } else if (nodeType == NodeType.TAG) {
                NodeFactory.TagNodeBuilder tagBuilder = (NodeFactory.TagNodeBuilder) nodeBuilder;
                tagBuilder.setUpdateRate(updateRate)
                        .setStationaryUpdateRate(stationaryUpdateRate)
                        .setLocationEngineEnable(locationEngineEnable)
                        .setLowPowerModeEnable(lowPowerModeEnable)
                        .setAccelerometerEnable(accelerometerEnable);
            }
            NetworkNode node = nodeBuilder.build();
            NetworkNodeEnhancedImpl nne = new NetworkNodeEnhancedImpl(node, lastSeen);
            if (nodeType == NodeType.TAG) {
                // default is to track position
                nne.setTrackMode(trackMode != null ? trackMode : TrackMode.TRACKED_POSITION);
            }
            return nne;
        }

    }
}
