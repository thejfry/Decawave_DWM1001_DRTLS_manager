/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BLE Argo API constants.
 */
public class BleConstants {
    // MAX concurrent connections overall
    public static final int MAX_CONCURRENT_CONNECTION_COUNT = 6;
    public static final int RECONNECT_DELAY_MS = 500;
    public static final int BLE_DISCOVERY_SCAN_MIN_PERIOD_MS = 2000;
    public static final int BLE_DISCOVERY_NOSCAN_MIN_PERIOD_MS = 3000;
    // how many concurrently directly tracked tags - keep one connection for location proxy
    public static final int LOCATION_DATA_OBSERVER_MAX_TRACKED_TAGS_COUNT = MAX_CONCURRENT_CONNECTION_COUNT - 2;
    // timeouts
    static final int CONNECT_TIMEOUT = 6000;
    static final int SECOND_SERVICE_DISCOVERY_TIMEOUT = 10000;
    static final int FIRST_SERVICE_DISCOVERY_TIMEOUT = 3000;
    static final long READ_CHARACTERISTIC_TIMEOUT = 5000;
    static final long CHANGE_MTU_TIMEOUT = 10000;
    // delays
    public static final int RECONNECT_DELAY_ON_SUDDEN_DISCONNECT = 2500;
    public static final int RECONNECT_DELAY_ON_TIMEOUT = 3000;
    public static final int RECONNECT_DELAY_ON_OPERATION_ERROR = 2000;
    public static final int RECONNECT_DELAY_ON_OTHER_ERROR = 5000;

    // see https://punchthrough.com/blog/posts/maximizing-ble-throughput-part-2-use-larger-att-mtu
    public static final int EXTRA_MTU = 4;                              // the MTU needs to always be enlarged with this value!
    //
    public static final int MTU_ON_LOCATION_DATA_OBSERVE = 106;         // notifications must fit into a single MTU
    public static final int MTU_ON_PROXY_POSITION_DATA_OBSERVE = 76;    // notifications must fit into a single MTU
    public static final int FW_UPLOAD_CHUNK_SIZE = 32;                              // pure FW data chunk size
    public static final int MTU_ON_FW_UPLOAD = FW_UPLOAD_CHUNK_SIZE + 5;           // chunk along with offset and message type must fit into a single MTU

    // firmware upload poll command codes
    public static final int FW_POLL_COMMAND_UPLOAD_REFUSED = 0;
    public static final int FW_POLL_COMMAND_BUFFER_REQUEST = 1;
    public static final int FW_POLL_COMMAND_UPLOAD_COMPLETE = 2;
    public static final int FW_POLL_COMMAND_SAVE_FAILED = 3;
    public static final int FW_POLL_COMMAND_SAVE_FAILED_INVALID_CHECKSUM = 14;

    // descriptor
    public static final UUID DESCRIPTOR_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // services
    public static final UUID SERVICE_UUID_NETWORK_NODE = UUID.fromString("680c21d9-c946-4c1f-9c11-baa1c21329e7");
    public static final UUID SERVICE_UUID_STD_GAP = stdBleUuid("1800");

    // characteristics
    public static final UUID CHARACTERISTIC_NETWORK_ID = UUID.fromString("80f9d8bc-3bff-45bb-a181-2d6a37991208");

    public static final UUID CHARACTERISTIC_OPERATION_MODE = UUID.fromString("3f0afd88-7770-46b0-b5e7-9fc099598964");
    public static final UUID CHARACTERISTIC_LOCATION_DATA = UUID.fromString("003bbdf2-c634-4b3d-ab56-7ec889b89a37");
    public static final UUID CHARACTERISTIC_LOCATION_DATA_MODE = UUID.fromString("a02b947e-df97-4516-996a-1882521e0ead");
    public static final UUID CHARACTERISTIC_PERSISTED_POSITION = UUID.fromString("f0f26c9b-2c8c-49ac-ab60-fe03def1b40c");
    public static final UUID CHARACTERISTIC_PASSWORD = UUID.fromString("9d5ab03b-cbf8-4ae5-9f11-63e45f538ada");
    public static final UUID CHARACTERISTIC_DEVICE_INFO = UUID.fromString("1e63b1eb-d4ed-444e-af54-c1e965192501");
    public static final UUID CHARACTERISTIC_STATISTICS = UUID.fromString("0eb2bc59-baf1-4c1c-8535-8a0204c69de5");

    // anchor-specific characteristics
    public static final UUID CHARACTERISTIC_ANCHOR_CLUSTER_INFO = UUID.fromString("17b1613e-98f2-4436-bcde-23af17a10c72");
    public static final UUID CHARACTERISTIC_ANCHOR_MAC_STATS = UUID.fromString("28d01d60-89de-4bfa-b6e9-651ba596232c");
    public static final UUID CHARACTERISTIC_ANCHOR_LIST = UUID.fromString("5b10c428-af2f-486f-aee1-9dbd79b6bccb");
    public static final UUID CHARACTERISTIC_PROXY_POSITIONS = UUID.fromString("f4a67d7d-379d-4183-9c03-4b6ea5103291");

    // tag-specific characteristics
    public static final UUID CHARACTERISTIC_TAG_UPDATE_RATE = UUID.fromString("7bd47f30-5602-4389-b069-8305731308b6");

    // firmware update
    public static final UUID CHARACTERISTIC_FW_UPDATE_PUSH = UUID.fromString("5955aa10-e085-4030-8aa6-bdfac89ac32b");
    public static final UUID CHARACTERISTIC_FW_UPDATE_POLL = UUID.fromString("9eed0e27-09c0-4d1c-bd92-7c441daba850");

    // explicit disconnect
    public static final UUID CHARACTERISTIC_DISCONNECT = UUID.fromString("ed83b848-da03-4a0a-a2dc-8b401080e473");

    // the shortened BLE UUID mask is:
    // 0000XXXX-0000-1000-8000-00805f9b34fb
    public static final UUID CHARACTERISTIC_STD_LABEL = stdBleUuid("2A00");

    private static UUID stdBleUuid(String doubleOctet) {
        Preconditions.checkState(doubleOctet.length() == 4);
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", doubleOctet));
    }


    // keep here only gateway characteristics (keySet() is used to identify the set of GW chars)
    public static final Map<UUID, String> MAP_CHARACTERISTIC_TITLE = new HashMap<UUID, String>() {{
        put(CHARACTERISTIC_NETWORK_ID, "Argo Network ID");
        put(CHARACTERISTIC_LOCATION_DATA, "Location data");
        put(CHARACTERISTIC_LOCATION_DATA_MODE, "Location data mode");
        put(CHARACTERISTIC_PERSISTED_POSITION, "Persisted position");
        put(CHARACTERISTIC_TAG_UPDATE_RATE, "Update rate");
        put(CHARACTERISTIC_STD_LABEL, "Label");
        put(CHARACTERISTIC_OPERATION_MODE, "Operation mode");
        put(CHARACTERISTIC_PASSWORD, "Password");
        put(CHARACTERISTIC_ANCHOR_CLUSTER_INFO, "Cluster info");
        put(CHARACTERISTIC_DEVICE_INFO, "Device info");
        put(CHARACTERISTIC_ANCHOR_MAC_STATS, "MAC status");
        put(CHARACTERISTIC_ANCHOR_LIST, "Anchor list");
        put(CHARACTERISTIC_STATISTICS, "Node statistics");
        put(CHARACTERISTIC_FW_UPDATE_PUSH, "FW update push");
        put(CHARACTERISTIC_FW_UPDATE_POLL, "FW update poll");
        put(CHARACTERISTIC_DISCONNECT, "Disconnect request");
        put(CHARACTERISTIC_PROXY_POSITIONS, "Proxy positions");
    }};

    static final Map<UUID, String> MAP_DESCRIPTOR_TITLE = new HashMap<UUID, String>() {{
        put(DESCRIPTOR_CCC, "CCC descriptor");
    }};

}