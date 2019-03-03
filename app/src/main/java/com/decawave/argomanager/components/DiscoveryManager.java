/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argo.api.struct.NetworkNode;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manages discovery, keeps cross-discovery-sessions state.
 *
 * The discovered nodes are reported from the low-level discovery API ({@link com.decawave.argo.api.DiscoveryApi}
 * via transient node events internal callback (of network node manager).
 * This internal callback passes the invocations to discovery manager which translates them to discovery
 * events. The discovery manager combines these callback with presence API and interprets those as if the nodes
 * are joining and leaving/present and missing.
 *
 * @see com.decawave.argomanager.components.ih.IhNodeDiscoveryListener
 */
public interface DiscoveryManager {

    void startTimeLimitedDiscovery(boolean prolongIfRunning);

    void startDiscovery();

    /**
     * Issues a stop discovery request.
     */
    void stopDiscovery();

    /**
     * Whether this is in process of stopping the discovery.
     */
    boolean isStopping();

    /**
     * Valid only if stopping.
     */
    void continueDiscovery();

    void scheduleDiscoveryStop(long duration);

    void cancelScheduledDiscoveryStop();

    void ignoreDiscoveryStopRequests(boolean ignore);

    boolean isDiscovering();

    void stopDiscoveryIfRunning();

    ///////////////////////////////////////////////////////////////////////////
    // node-info routines
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This call does not return nodes which were discovered but since the discovery
     * became persistent.
     */
    @NotNull
    List<NetworkNode> getDiscoveredTransientOnlyNodes();

    /**
     * @return true if there is at least one discovered node which is still transient
     *         (has not been assigned to network yet), same as getDiscoveredTransientOnlyNodes().size > 0
     */
    boolean anyTransientNodeDiscovered();

    /**
     * @return number of initially transient nodes discovered in this/last discovery session regardless
     *         their current transient/persistent status
     */
    int getNumberOfDiscoverySessionNodes();


}