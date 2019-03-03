/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argomanager.argoapi.ble.ReadCharacteristicRequest;
import com.decawave.argomanager.argoapi.ble.ReadCharacteristicRequestsBuilder;

import java.util.Set;

/**
 * Argo project.
 */

public class PropertySet {

    private static final NetworkNodeProperty[] NETWORK_NODE_PROPS = {
            NetworkNodeProperty.NODE_TYPE,
            NetworkNodeProperty.UWB_MODE,
            NetworkNodeProperty.LED_INDICATION_ENABLE,
            NetworkNodeProperty.OPERATING_FIRMWARE,
            NetworkNodeProperty.ID,
            NetworkNodeProperty.NETWORK_ID,
            NetworkNodeProperty.LOCATION_DATA_MODE,
            NetworkNodeProperty.HW_VERSION,
            NetworkNodeProperty.FW1_VERSION,
            NetworkNodeProperty.FW2_VERSION,
            NetworkNodeProperty.FW1_CHECKSUM,
            NetworkNodeProperty.FW2_CHECKSUM,
            NetworkNodeProperty.NODE_STATISTICS,
    };


    private static ReadCharacteristicRequestsBuilder NETWORK_NODE_RD_REQ_BUILDER = new ReadCharacteristicRequestsBuilder()
        .addProperty(NetworkNodeProperty.LABEL)
        .addProperty(NETWORK_NODE_PROPS);



    public static final Set<ReadCharacteristicRequest> TAG_REQUESTS = NETWORK_NODE_RD_REQ_BUILDER.clone().addProperty(
                        NetworkNodeProperty.TAG_UPDATE_RATE,
                        NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE,
                        NetworkNodeProperty.TAG_LOW_POWER_MODE_ENABLE,
                        NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE,
                        NetworkNodeProperty.TAG_LOCATION_DATA
                ).build();


    public static final Set<ReadCharacteristicRequest> ANCHOR_REQUESTS =
                NETWORK_NODE_RD_REQ_BUILDER.clone().addProperty(
                        NetworkNodeProperty.ANCHOR_BRIDGE,
                        NetworkNodeProperty.ANCHOR_INITIATOR,
                        NetworkNodeProperty.ANCHOR_SEAT,
                        NetworkNodeProperty.ANCHOR_CLUSTER_MAP,
                        NetworkNodeProperty.ANCHOR_CLUSTER_NEIGHBOUR_MAP,
                        NetworkNodeProperty.ANCHOR_MAC_STATS,
                        NetworkNodeProperty.ANCHOR_AN_LIST,
                        NetworkNodeProperty.ANCHOR_POSITION,
                        NetworkNodeProperty.ANCHOR_DISTANCES
                        ).build();
}
