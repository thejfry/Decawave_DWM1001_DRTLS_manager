/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.EnhancedNetworkNodeContainer;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.NetworkNodeEnhancedImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.util.Util.formatAsHexa;

/**
 * Argo project.
 */

public class EnhancedNetworkNodeContainerImpl implements EnhancedNetworkNodeContainer {
    private static final ComponentLog log = new ComponentLog(EnhancedNetworkNodeContainerImpl.class);
    // constants
    private static final Predicate<NetworkNode> TRUE_PREDICATE = (node) -> true;
    // double index
    private Map<Long, NetworkNodeEnhanced> nodesById;
    private Multimap<Short, NetworkNodeEnhanced> nodesByShortId;
    private Map<String, NetworkNodeEnhanced> nodesByBle;


    public EnhancedNetworkNodeContainerImpl() {
        this.nodesByBle = new HashMap<>();
        this.nodesById = new HashMap<>();
        this.nodesByShortId = MultimapBuilder.hashKeys().linkedListValues().build();
    }

    @Override
    public NetworkNodeEnhanced addNode(@NotNull NetworkNode node) {
        NetworkNodeEnhanced nne = new NetworkNodeEnhancedImpl(node);
        doAddNode(nne, false);
        return nne;
    }

    public void doAddNode(NetworkNodeEnhanced nne, boolean duplicateCheck) {
        if (Constants.DEBUG) {
            log.d("addNode: " + "nne = [" + nne + "]");
        }
        NetworkNode node = nne.asPlainNode();
        Long nodeId = node.getId();
        String nodeBle = node.getBleAddress();
        if (Constants.DEBUG) {
            // check argument
            Preconditions.checkNotNull(nodeId);
            Preconditions.checkNotNull(nodeBle);
            if (duplicateCheck) {
                Preconditions.checkState(!nodesById.containsKey(nodeId), "node is already present: " + nodeId);
                Preconditions.checkState(!nodesByBle.containsKey(nodeBle), "node is already present: " + nodeBle);
            }
        }
        nodesByBle.put(node.getBleAddress(), nne);
        nodesById.put(node.getId(), nne);
        nodesByShortId.put(node.getId().shortValue(), nne);
    }

    @Override
    public NetworkNodeEnhanced getNode(long id) {
        return nodesById.get(id);
    }

    @Override
    public NetworkNodeEnhanced getNode(String bleAddress) {
        return nodesByBle.get(bleAddress);
    }

    @Override
    public NetworkNodeEnhanced getNodeByShortId(short shortId) {
        Collection<NetworkNodeEnhanced> nne = nodesByShortId.get(shortId);
        return nne.isEmpty() ? null : nne.iterator().next();
    }

    @Override
    public void removeNode(long nodeId) {
        if (Constants.DEBUG) {
            log.d("removeNode: " + "nodeId = [" + formatAsHexa(nodeId) + "]");
        }
        NetworkNodeEnhanced node = nodesById.remove(nodeId);
        if (node != null) {
            nodesByBle.remove(node.getBleAddress());
            nodesByShortId.remove(node.getId().shortValue(), node);
        }
    }

    @Override
    public Collection<NetworkNodeEnhanced> getNodes(boolean copy) {
        if (copy) {
            return getNodes(TRUE_PREDICATE);
        } else {
            return nodesByBle.values();
        }
    }

    @Override
    public int countNodes(Predicate<NetworkNode> filter) {
        return countNodesEnhanced((nne) -> filter.test(nne.asPlainNode()));
    }

    @Override
    public int countNodesEnhanced(Predicate<NetworkNodeEnhanced> filter) {
        int cntr = 0;
        for (NetworkNodeEnhanced nne : nodesById.values()) {
            if (filter.test(nne)) {
                cntr++;
            }
        }
        return cntr;
    }

    @Override
    public List<NetworkNodeEnhanced> getNodes(Predicate<NetworkNode> filter) {
        return Stream.of(nodesByBle.values()).filter((ne) -> filter.test(ne.asPlainNode())).collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return nodesByBle.isEmpty();
    }
}

