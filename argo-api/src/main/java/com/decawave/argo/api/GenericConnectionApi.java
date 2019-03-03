/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.ConnectPriority;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Manages connections to LEAPS network elements (gateways, anchors, tags).
 */
@SuppressWarnings("WeakerAccess")
public interface GenericConnectionApi<AddressType> {

    /**
     * Connects to given address/network node.
     * @param address identifies the node
     * @param connectPriority priority of this connect request dispatch (influences latency)
     * @param onConnectedCallback called after connection has been established
     * @param onFailCallback called when something goes wrong either during connection establish procedure or later
     * @param onDisconnectedCallback called whenever the connection is closed, also in case of a failure, provides
     *                               int error code: 0 means success, non-zero means failure, null means that the connection
     *                               did not happen (the request was terminated before it could start)
     * @return network node connection which is in CONNECTING phase (may progress to CONNECTED)
     *
     */
    NetworkNodeConnection connect(@NotNull AddressType address,
                                  @NotNull ConnectPriority connectPriority,
                                  @NotNull Action1<NetworkNodeConnection> onConnectedCallback,
                                  @Nullable Action2<NetworkNodeConnection,Fail> onFailCallback,
                                  @Nullable Action2<NetworkNodeConnection, Integer> onDisconnectedCallback);
}
