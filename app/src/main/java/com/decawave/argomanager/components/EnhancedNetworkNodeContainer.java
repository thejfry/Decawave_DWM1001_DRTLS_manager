/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import java.util.Collection;
import java.util.List;

/**
 * Bunch of network nodes.
 * Organized just by id and BLE address.
 */
public interface EnhancedNetworkNodeContainer {

    NetworkNodeEnhanced addNode(NetworkNode node);

    NetworkNodeEnhanced getNode(long id);

    NetworkNodeEnhanced getNode(String bleAddress);

    NetworkNodeEnhanced getNodeByShortId(short shortId);

    void removeNode(long nodeId);

    Collection<NetworkNodeEnhanced> getNodes(boolean copy);

    int countNodes(Predicate<NetworkNode> filter);

    int countNodesEnhanced(Predicate<NetworkNodeEnhanced> filter);

    List<NetworkNodeEnhanced> getNodes(Predicate<NetworkNode> filter);

    boolean isEmpty();

}
