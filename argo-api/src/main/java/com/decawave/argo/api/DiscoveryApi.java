/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.ServiceData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

/**
 * API for Leaps Network discovery.
 * Does not keep the discovered nodes set for future retrieval.
 * The API have the following states: STOPPED, DISCOVERING, PAUSED, STOPPING
 */
public interface DiscoveryApi {

    /**
     * Starts node discovery.
     * Puts the discovery API to the DISCOVERING state.
     */
    void startDiscovery(@NotNull Action2<ServiceData, NetworkNode> onNodeDiscoveredCallback,
                        @NotNull Action1<Fail> onFailCallback,
                        @NotNull Func1<String, ConnectPriority> priorityResolver,
                        @Nullable Map<String, ServiceData> initialServiceData);

    /**
     * @return true if the state is different than STOPPED (i.e. STOPPING or DISCOVERING)
     */
    boolean isDiscovering();

    /**
     * Issues a request to stop node discovery.
     */
    void stopDiscovery();

    /**
     * Whether the stop request is currently in progress. The state is STOPPING.
     */
    boolean isStopping();

    /**
     * Valid only in STOPPING state.
     */
    void continueDiscovery();


}
