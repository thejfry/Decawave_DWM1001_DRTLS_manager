/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argo.api.struct.NetworkNodeProperty;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Read characteristic request builder.
 */

public class ReadCharacteristicRequestsBuilder {
    private Set<ReadCharacteristicRequest> requests;

    public ReadCharacteristicRequestsBuilder() {
        this.requests = new LinkedHashSet<>();
    }

    public ReadCharacteristicRequestsBuilder addProperty(NetworkNodeProperty... properties) {
        for (NetworkNodeProperty property : properties) {
            addCharacteristicForProperty(property, requests);
        }
        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ReadCharacteristicRequestsBuilder clone() {
        ReadCharacteristicRequestsBuilder b = new ReadCharacteristicRequestsBuilder();
        b.requests.addAll(this.requests);
        return b;
    }

    public Set<ReadCharacteristicRequest> build() {
        Set<ReadCharacteristicRequest> r = this.requests;
        requests = new LinkedHashSet<>();
        return r;
    }

    private static void addCharacteristicForProperty(NetworkNodeProperty property, Set<ReadCharacteristicRequest> readRequests) {
        ReadCharacteristicRequest request = null;

        switch (property) {
            case ID:
            case ANCHOR_BRIDGE:
            case HW_VERSION:
            case FW1_VERSION:
            case FW2_VERSION:
            case FW1_CHECKSUM:
            case FW2_CHECKSUM:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_DEVICE_INFO);
                break;
            case LABEL:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_STD_GAP, BleConstants.CHARACTERISTIC_STD_LABEL);
                break;
            case NETWORK_ID:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_NETWORK_ID);
                break;
            case ANCHOR_POSITION:
            case ANCHOR_DISTANCES:
            case TAG_LOCATION_DATA:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_LOCATION_DATA);
                break;
            case LOCATION_DATA_MODE:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_LOCATION_DATA_MODE);
                break;
            case TAG_UPDATE_RATE:
            case TAG_STATIONARY_UPDATE_RATE:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_TAG_UPDATE_RATE);
                break;
            case NODE_STATISTICS:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_STATISTICS);
                break;
            case BLE_ADDRESS:
                // this is filled on transport layer (at least now, when we have no bridges yet), no corresponding characteristic
                break;
            case PASSWORD:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_PASSWORD);
                break;
            case NODE_TYPE:
            case ANCHOR_INITIATOR:
            case UWB_MODE:
            case LED_INDICATION_ENABLE:
            case TAG_ACCELEROMETER_ENABLE:
            case TAG_LOCATION_ENGINE_ENABLE:
            case TAG_LOW_POWER_MODE_ENABLE:
            case OPERATING_FIRMWARE:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_OPERATION_MODE);
                break;
            case ANCHOR_MAC_STATS:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_ANCHOR_MAC_STATS);
                break;
            case ANCHOR_SEAT:
            case ANCHOR_CLUSTER_MAP:
            case ANCHOR_CLUSTER_NEIGHBOUR_MAP:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_ANCHOR_CLUSTER_INFO);
                break;
            case ANCHOR_AN_LIST:
                request = new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_ANCHOR_LIST);
                break;
            default:
                throw new IllegalArgumentException("illegal property specified: " + property);
        }
        if (request != null) {
            readRequests.add(request);
        }
    }


}
