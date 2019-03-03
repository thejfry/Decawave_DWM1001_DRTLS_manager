/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import com.decawave.argo.api.interaction.LocationData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Argo project.
 */
public enum NetworkNodeProperty {
    // common properties
    ID,
    NODE_TYPE,
    UWB_MODE,
    OPERATING_FIRMWARE,
    FIRMWARE_UPDATE_ENABLE,
    LED_INDICATION_ENABLE,
    LABEL,
    LOCATION_DATA_MODE,
    BLE_ADDRESS,
    NETWORK_ID,
    PASSWORD,
    HW_VERSION,
    FW1_VERSION,
    FW2_VERSION,
    FW1_CHECKSUM,
    FW2_CHECKSUM,
    NODE_STATISTICS,
    // anchor-specific
    ANCHOR_INITIATOR,
    ANCHOR_BRIDGE,
    ANCHOR_SEAT(true, false),
    ANCHOR_CLUSTER_MAP(true, false),
    ANCHOR_CLUSTER_NEIGHBOUR_MAP(true, false),
    ANCHOR_MAC_STATS(true, false),
    ANCHOR_AN_LIST(true, false),
    ANCHOR_POSITION,
    ANCHOR_DISTANCES,
    // tag-specific
    TAG_ACCELEROMETER_ENABLE,
    TAG_UPDATE_RATE,
    TAG_STATIONARY_UPDATE_RATE,
    TAG_LOW_POWER_MODE_ENABLE,
    TAG_LOCATION_ENGINE_ENABLE,
    TAG_LOCATION_DATA(true, false),
    // application specific (not stored in a node)
    LAST_SEEN(false, true);
    
    public final boolean dynamic;
    // application specific - not stored in a node (only in extended node/wrapper)
    public final boolean extended;

    NetworkNodeProperty() {
        this.dynamic = false;
        this.extended = false;
    }

    NetworkNodeProperty(boolean dynamic, boolean extended) {
        this.dynamic = dynamic;
        this.extended = extended;
    }

    @SuppressWarnings("unchecked")
    public <T> T deepCopy(@NotNull T value) {
        if (this == NetworkNodeProperty.NODE_STATISTICS) {
            return (T) new NodeStatistics((NodeStatistics) value);
        } else if (this == NetworkNodeProperty.ANCHOR_AN_LIST) {
            return (T) new ArrayList<>((List) value);
        } else if (this == NetworkNodeProperty.ANCHOR_POSITION) {
            return (T) new Position((Position) value);
        } else if (this == NetworkNodeProperty.ANCHOR_DISTANCES) {
            return (T) RangingAnchor.deepCopy((List<RangingAnchor>) value);
        } else if (this == NetworkNodeProperty.TAG_LOCATION_DATA) {
            // we need to do a deep copy
            LocationData ld = (LocationData) value;
            Position newPos = null;
            if (ld.position != null) {
                newPos = new Position(ld.position);
            }
            List<RangingAnchor> newRa = null;
            if (ld.distances != null) {
                newRa = RangingAnchor.deepCopy(ld.distances);
            }
            return (T) new LocationData(newPos, newRa);
        } else {
            return value;
        }
    }
}
