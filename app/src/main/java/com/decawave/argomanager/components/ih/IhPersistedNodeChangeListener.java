/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.ih;

import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Listens to events associated with persisted network nodes (associated with a known network).
 */
public interface IhPersistedNodeChangeListener extends InterfaceHubHandler {

    /**
     * Used also for new nodes, and also for new declared network (no change on network node).
     */
    void onNodeUpdatedAndOrAddedToNetwork(short networkId, NetworkNodeEnhanced node);

    /**
     * Called when a node has been removed from a network.
     * @param networkId network from which the node has been removed
     * @param nodeId identifies the node
     * @param userInitiated whether the change was initiated by the user
     */
    void onNodeUpdatedAndRemovedFromNetwork(short networkId, long nodeId, boolean userInitiated);

    /**
     * Used to broadcast that a node in a declared network has changed.
     *
     * @param node updated node
     */
    void onNodeUpdated(NetworkNodeEnhanced node);

    /**
     * Called if the method {@link com.decawave.argomanager.components.NetworkNodeManager#forgetNode(Long, boolean)}
     * is called.
     *
     * @param nodeId identifies the node
     * @param networkId if the node was part of a network, this is the network id
     * @param userInitiated true if user has initiated this action
     */
    void onNodeForgotten(long nodeId, Short networkId, boolean userInitiated);


}
