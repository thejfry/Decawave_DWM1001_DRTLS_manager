/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.impl.NetworksNodesStorageImpl;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.TrackMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Central place for managing existing ARGO networks.
 *
 * Binds these independent components together:
 * - {@link NetworkModelManager}
 * - {@link com.decawave.argomanager.prefs.AppPreferenceAccessor}
 * - {@link NetworksNodesStorageImpl}
 * - {@link LocationDataLogger}
 *
 * Get notified about network changes via {@link IhPersistedNodeChangeListener}
 */
public interface NetworkNodeManager {

    /**
     * Must be called before used.
     */
    void init();

    boolean isActiveNetworkId(Short isActiveNetworkId);

    boolean isInActiveNetwork(NetworkNodeEnhanced nne);

    void declareNetwork(NetworkModel networkModel);

    void setNodeTrackMode(long nodeId, TrackMode trackMode);

    /**
     * @return given node track mode, or null if the node is not found
     */
    @Nullable TrackMode getNodeTrackMode(long nodeId);

    /**
     * @return given node track mode, or null if the node is not found
     */
    @Nullable TrackMode getNodeTrackMode(String bleAddress);

    @Nullable NetworkModel getActiveNetwork();

    @NotNull NetworkModel getActiveNetworkNullSafe();

    boolean activeNetworkContainsNode(String bleAddress);

    @NotNull Map<Short,NetworkModel> getNetworks();

    /**
     * Update existing/add a new node.
     * This also leads to an update of lastNodeUpdate timestamp.
     *  @param node target node state
     */
    void onNodeIntercepted(NetworkNode node);

    @NotNull List<NetworkNodeEnhanced> getActiveNetworkNodes();

    @NotNull List<NetworkNodeEnhanced> getNetworkNodes(short networkId);

    @NotNull List<NetworkNodeEnhanced> getNetworkNodes(Predicate<NetworkNode> filter);

    Long bleToId(String nodeBleAddress);

    String idToBle(Long nodeId);

    NetworkNodeEnhanced getNode(long nodeId);

    NetworkNodeEnhanced getNode(String bleAddress);

    void updateAnchorPosition(long nodeId, Position position);

    void updateAnchorDistances(long nodeId, List<RangingAnchor> distances);

    void updateTagLocationData(long nodeId, LocationData locationData);

    void updateLastSeen(String bleAddress);

    void removeNetwork(short networkId);

    void removeNetwork(short networkId, boolean explicitUserAction);

    void removeActiveNetwork();

    void undoNetworkRemove(short networkId);

    boolean hasNetworkByName(String networkName);

    boolean isNodePersisted(long nodeId);

    boolean isNodePersisted(String bleAddress);

    int getNumberOfAnchors(short networkId);

    int getNumberOfTags(short networkId);

    int getNumberOfDirectlyTrackedTags(short networkId);

    NetworkNodeEnhanced getNodeByShortId(short nodeId);

    /**
     * Does not interaction with the node. It just removes it's representation from internal
     * container.
     * @param nodeId identifies the node
     * @param userInitiated whether the action was initiated by user
     */
    void forgetNode(Long nodeId, boolean userInitiated);

}
