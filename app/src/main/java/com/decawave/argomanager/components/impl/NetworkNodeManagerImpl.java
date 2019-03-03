/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.argoapi.ext.NetworkNodePropertySetter;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.EnhancedNetworkNodeContainer;
import com.decawave.argomanager.components.EnhancedNetworkNodeContainerFactory;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkModelManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.NetworkPropertyChangeListener;
import com.decawave.argomanager.components.NetworksNodesStorage;
import com.decawave.argomanager.components.ih.IhEnhancedNodePropertiesChangeListener;
import com.decawave.argomanager.components.ih.IhNetworkChangeListener;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.NetworkNodeEnhancedImpl;
import com.decawave.argomanager.components.struct.NodeWarning;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */
public class NetworkNodeManagerImpl implements NetworkNodeManager {
    private static final ComponentLog log = new ComponentLog(NetworkNodeManager.class);
    private static final int PERSIST_DELAY_MS = 2000;
    // members
    private final AppPreferenceAccessor appPreferenceAccessor;
    private final LocationDataLogger locationDataLogger;
    private final NetworksNodesStorage storage;
    private final NetworkModelManager networkModelManager;
    private final UniqueReorderingStack<Short> activeNetworkIdStack;
    // central place for storing complete node set
    private EnhancedNetworkNodeContainer nodes;
    private TransientNodeChangeHandler transientNodeChangeListener;

    // listener routines simply schedule persist and then pass the change invocation to IH
    private NetworkPropertyChangeListener systemListener = new NetworkPropertyChangeListener() {

        @Override
        public void onNetworkAdded(short networkId) {
            if (Constants.DEBUG) {
                log.d("onNetworkAdded: " + "networkId = [" + networkId + "]");
            }
            schedulePersist();
            // broadcast
            InterfaceHub.getHandlerHub(IhNetworkChangeListener.class).onNetworkAdded(networkId);
            // check which nodes are affected and are now persistent
            for (NetworkNodeEnhanced nne : nodes.getNodes((nn) -> Objects.equals(networkId, nn.getNetworkId()))) {
                // remove those nodes which are not present (if they become present again, they will be added automatically)
                if (transientNodeChangeListener.nodeAboutToBePersisted(nne.getBleAddress())) {
                    InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdatedAndOrAddedToNetwork(networkId, nne);
                } else {
                    // if the node is NOT allowed to become persistent, we have to remove it
                    nodes.removeNode(nne.getId());
                    if (!nne.getWarnings().isEmpty()) {
                        // we also have to recompute warnings
                        recomputeDependentNodesWarnings(nne);
                    }
                }
            }
        }

        @Override
        public void onNetworkUpdated(short networkId) {
            if (Constants.DEBUG) {
                log.d("onNetworkUpdated: " + "networkId = [" + networkId + "]");
            }
            schedulePersist();
            InterfaceHub.getHandlerHub(IhNetworkChangeListener.class).onNetworkUpdated(networkId);
        }

        @Override
        public void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction) {
            if (Constants.DEBUG) {
                log.d("onNetworkRemoved: " + "networkId = [" + networkId + "], networkName = [" + networkName + "]");
            }
            activeNetworkIdStack.remove(networkId);
            schedulePersist();
            InterfaceHub.getHandlerHub(IhNetworkChangeListener.class).onNetworkRemoved(networkId, networkName, explicitUserAction);
            // check which nodes are affected and are now transient
            for (NetworkNodeEnhanced nne : nodes.getNodes(nn -> Objects.equals(nn.getNetworkId(), networkId))) {
                transientNodeChangeListener.onNetworkRemovedNodeBecameTransient(nne);
            }
        }

        @Override
        public void onNetworkRenamed(short networkId, String newName) {
            if (Constants.DEBUG) {
                log.d("onNetworkRenamed: " + "networkId = [" + networkId + "], newName = [" + newName + "]");
            }
            schedulePersist();
            InterfaceHub.getHandlerHub(IhNetworkChangeListener.class).onNetworkRenamed(networkId, newName);
        }

        @Override
        public void onFloorPlanChanged(short networkId, FloorPlan floorPlan) {
            if (Constants.DEBUG)
                log.d("onFloorPlanChanged() called with: " + "floorPlan = [" + floorPlan + "]");
            schedulePersist();
            InterfaceHub.getHandlerHub(IhNetworkChangeListener.class).onFloorPlanChanged(networkId, floorPlan);
        }

    };

    private void recomputeDependentNodesWarnings(NetworkNodeEnhanced node) {
        List<NodeWarning> warnings = node.getWarnings();
        if (!warnings.isEmpty()) {
            Set<Long> changedNodeIds = new HashSet<>();
            HashSet<Long> computedNodeIds = new HashSet<>();
            // go through related nodes only
            for (NodeWarning nodeWarning : node.getWarnings()) {
                if (nodeWarning.relatedNodesParam != null && !nodeWarning.relatedNodesParam.isEmpty()) {
                    for (Long relatedNodeId : nodeWarning.relatedNodesParam) {
                        if (!computedNodeIds.contains(relatedNodeId)) {
                            // we have not computed this node yet
                            NetworkNodeEnhanced relatedNode = getNode(relatedNodeId);
                            if (relatedNode != null) {
                                computeWarningsAndCheckDependentNodes(relatedNode, computedNodeIds, changedNodeIds, true);
                            } else {
                                ArgoApp.reportSilentException(new Exception("unknown related node warning, about to remove " + node + ", resolution of " + relatedNodeId + " failed"));
                            }
                        }
                    }
                }
            }
            // report the changed nodes
            if (!changedNodeIds.isEmpty()) {
                reportChangedNodes(changedNodeIds);
            }
        }
    }


    @Inject
    NetworkNodeManagerImpl(NetworkModelManager networkModelManager,
                           AppPreferenceAccessor appPreferenceAccessor,
                           LocationDataLogger locationDataLogger,
                           NetworksNodesStorage storage,
                           UniqueReorderingStack<Short> activeNetworkIdStack) {
        this.networkModelManager = networkModelManager;
        this.appPreferenceAccessor = appPreferenceAccessor;
        this.locationDataLogger = locationDataLogger;
        this.storage = storage;
        this.activeNetworkIdStack = activeNetworkIdStack;
        // create the container
        this.nodes = EnhancedNetworkNodeContainerFactory.createContainer(Collections.emptyList());
    }

    @Override
    @Inject /* make sure that this method gets called after construction - same as @PostConstruct in JSR 250 */
    public void init() {
        // set up listener - so that we are notified
        this.networkModelManager.setNetworkChangeListener(systemListener);
    }

    public void load() {
        // load from storage
        storage.load(this::onLoadedFromStorage);
    }

    @Override
    public boolean isActiveNetworkId(Short nid) {
        if (nid == null) {
            return false;
        }
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        return activeNetworkId != null && (short) activeNetworkId == nid;
    }

    @Override
    public boolean isInActiveNetwork(NetworkNodeEnhanced nne) {
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        if (Constants.DEBUG) {
            Preconditions.checkState(nodes.getNode(nne.getId()) == nne, "passing a different instance of node could be dangerous!");
        }
        return activeNetworkId != null
                && Objects.equals(nne.asPlainNode().getNetworkId(), activeNetworkId);
    }

    @Override
    public void declareNetwork(NetworkModel networkModel) {
        if (Constants.DEBUG) {
            log.d("declareNetwork: " + "networkModel = [" + networkModel + "]");
        }
        boolean wasEmpty = networkModelManager.getNetworks().isEmpty();
        networkModelManager.addNetwork(networkModel);
        if (wasEmpty) {
            // make it also active
            appPreferenceAccessor.setActiveNetworkId(networkModel.getNetworkId());
        }
    }


    @Override
    public void setNodeTrackMode(long nodeId, TrackMode trackMode) {
        NetworkNodeEnhanced nne = getNode(nodeId);
        if (nne != null && nne.getTrackMode() != trackMode) {
            if (Constants.DEBUG) {
                Preconditions.checkState(nne.isTag(), "node is not a tag, cannot set trackmode");
            }
            ((NetworkNodeEnhancedImpl) nne).setTrackMode(trackMode);
            // and schedule persist
            schedulePersist();
            // we do not broadcast such changes
        }
    }

    @NotNull
    @Override
    public Map<Short, NetworkModel> getNetworks() {
        return networkModelManager.getNetworks();
    }

    @Override
    public void onNodeIntercepted(NetworkNode node) {
        Long nodeId = node.getId();
        NetworkNodeEnhanced myNode = retrieveMyNode(nodeId, node.getBleAddress());
        if (Constants.DEBUG) {
            log.d("onNodeIntercepted: " + "node = [" + node + "], myNode = " + myNode);
        }
        Set<Long> changedNodeIds = null;
        if (myNode != null) {
            // we knew this node
            if (Constants.DEBUG) {
                Preconditions.checkState(node != myNode, "you MUST NOT pass the same instance, we cannot distinguish what happened with the node then!");
            }
            // is there any difference?
            NetworkNode myPlainNode = myNode.asPlainNode();
            if (myPlainNode.getInitializedProperties().containsAll(node.getInitializedProperties())
                    && myPlainNode.compareByProperty(node).isEmpty()) {
                // the other node is not richer (does not have other initialized properties), nor different
                if (Constants.DEBUG) {
                    log.d("nodes do not differ, skipping");
                }
                return;
            }
            nodeId = myPlainNode.getId();
            Short oldNetworkId = myPlainNode.getNetworkId();
            boolean checkWarnings = false;
            Position oldAnchorPosition = myPlainNode.isAnchor() ? myPlainNode.getProperty(NetworkNodeProperty.ANCHOR_POSITION) : null;
            UwbMode oldUwb = myPlainNode.getUwbMode();
            // merge the fresh property values to our node (if possible)
            if (myPlainNode.isCompatible(node)) {
                myPlainNode.copyFrom(node);
            } else {
                // we will recompute warning in each case (there might have been change from anchor to tag/tag to anchor)
                // with re/set position,...
                checkWarnings = true;
                // replace the old myNode with the new one
                NetworkNode myNewPlainNode = NodeFactory.newBuilder(node.getType(), myPlainNode.getId()).build();
                // copy common properties from the old my node
                myNewPlainNode.copyFrom(myPlainNode);
                // copy/overwrite properties from the intercepted node
                myNewPlainNode.copyFrom(node);
                // and add the node
                myNode = nodes.addNode(myNewPlainNode);
            }
            myPlainNode = myNode.asPlainNode();
            Position newAnchorPosition = myPlainNode.isAnchor() ? myPlainNode.getProperty(NetworkNodeProperty.ANCHOR_POSITION) : null;
            UwbMode newUwb = myPlainNode.getUwbMode();
            Short newNetworkId = myPlainNode.getNetworkId();
            // touch timestamps
            myNode.touchLastSeen();
            // check positions
            if (checkWarnings
                    || !Objects.equals(oldAnchorPosition, newAnchorPosition)
                    || !Objects.equals(oldUwb, newUwb)
                    || !Objects.equals(oldNetworkId, newNetworkId)) {
                // we should recompute warnings of this and all related nodes
                changedNodeIds = new HashSet<>();
                computeWarningsAndCheckDependentNodes(myNode, new HashSet<>(), changedNodeIds, false);
            }
            if (node.isPropertyInitialized(NetworkNodeProperty.NETWORK_ID) && !Objects.equals(oldNetworkId, node.getNetworkId())) {
                if (Constants.DEBUG) {
                    log.d("network ids differ: oldNetworkId = " + oldNetworkId + ", newNetworkId = " + node.getNetworkId());
                }
                // there was a network change
                // forget the UI properties on purpose (let the user set it again)
                ((NetworkNodeEnhancedImpl) myNode).setTrackMode(TrackMode.TRACKED_POSITION);
                boolean oldNetworkDeclared = oldNetworkId != null && networkModelManager.hasNetwork(oldNetworkId);
                boolean newNetworkDeclared = newNetworkId != null && networkModelManager.hasNetwork(newNetworkId);
                // 1. broadcast exit events
                if (oldNetworkDeclared) {
                    InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdatedAndRemovedFromNetwork(oldNetworkId, nodeId, false);
                }
                if (!oldNetworkDeclared && !newNetworkDeclared) {
                    transientNodeChangeListener.onNodeUpdated(myNode);
                } else if (!oldNetworkDeclared) {
                    transientNodeChangeListener.onNodeUpdatedAndBecamePersistent(myNode.getBleAddress());
                }
                // 3. broadcast enter events
                if (newNetworkDeclared) {
                    InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdatedAndOrAddedToNetwork(newNetworkId, myNode);
                } else if (oldNetworkDeclared) {
                    transientNodeChangeListener.onNodeUpdatedAndBecameTransient(myNode);
                }
            } else {
                // there is no evidence that the node is in a different network (be it null or anything else)
                Short networkId = myPlainNode.getNetworkId();
                if (networkId != null && networkModelManager.hasNetwork(networkId)) {
                    InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdated(myNode);
                } else {
                    // the network is not declared or null
                    transientNodeChangeListener.onNodeUpdated(myNode);
                }
            }
        } else {
            // we do not know anything about this node
            myNode = nodes.addNode(NodeFactory.newNodeCopy(node));
            // set timestamps
            myNode.touchLastSeen();
            // check positions
            NetworkNode plainNode = myNode.asPlainNode();
            if ((plainNode.isAnchor() && plainNode.extractPositionDirect() != null || plainNode.getUwbMode() != null)) {
                // we should recompute warnings of this and all related nodes
                changedNodeIds = new HashSet<>();
                computeWarningsAndCheckDependentNodes(myNode, new HashSet<>(), changedNodeIds, false);
            }
            // do we know node's network?
            Short networkId = node.getNetworkId();
            if (networkId != null && networkModelManager.hasNetwork(networkId)) {
                // we know the declared network
                InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdatedAndOrAddedToNetwork(networkId, myNode);
            } else {
                transientNodeChangeListener.onNodeUpdatedAndBecameTransient(myNode);
            }
        }
        // broadcast enhanced properties change
        if (changedNodeIds != null) {
            reportChangedNodes(changedNodeIds);
        }
        // in each case do persist
        schedulePersist();
    }

    private void computeWarningsAndCheckDependentNodes(NetworkNodeEnhanced nne,
                                                       Set<Long> computedNodeIds,
                                                       Set<Long> changedNodeIds,
                                                       boolean nodeRemovalAllowed) {
        Long nodeId = nne.getId();
        if (Constants.DEBUG) {
            Preconditions.checkState(!computedNodeIds.contains(nodeId), "node " + nodeId + " warnings already computed!");
        }
        List<NodeWarning> newWarnings = new LinkedList<>();
        Short networkId = nne.asPlainNode().getNetworkId();
        if (networkId != null && networkId != 0) {
            newWarnings.addAll(computeWarnings(nne.asPlainNode()));
        } // else: keep empty warnings
        computedNodeIds.add(nodeId);
        List<NodeWarning> oldWarnings = nne.getWarnings();
        if (!oldWarnings.equals(newWarnings)) {
            ((NetworkNodeEnhancedImpl) nne).setWarnings(newWarnings);
            // collect both old and new dependencies
            Set<Long> dependencies = new HashSet<>();
            dependencies.addAll(Stream.concat(Stream.of(oldWarnings), Stream.of(newWarnings))
                    .flatMap((w) -> {
                        if (w.relatedNodesParam != null) {
                            return Stream.of(w.relatedNodesParam);
                        } else {
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toSet()));
            // go through the dependencies
            computeDependentNodesWarnings(dependencies, computedNodeIds, changedNodeIds, nodeRemovalAllowed);
            // note the change
            changedNodeIds.add(nodeId);
        }
    }

    private void computeDependentNodesWarnings(Set<Long> dependencies, Set<Long> computedNodeIds,
                                               Set<Long> changedNodeIds,
                                               boolean nodeRemovalAllowed) {
        for (Long relatedNodeId : dependencies) {
            if (!computedNodeIds.contains(relatedNodeId)) {
                // recursive invocation
                NetworkNodeEnhanced dependentNode = getNode(relatedNodeId);
                if (dependentNode == null && !nodeRemovalAllowed) {
                    if (com.decawave.argomanager.Constants.DEBUG) {
                        //noinspection ConstantConditions
                        Preconditions.checkState(false, "unknown related node " + Util.formatAsHexa(relatedNodeId) + "!");
                    } else {
                        // this is not that critical, we can silently skip this
                        ArgoApp.reportSilentException(new RuntimeException("unknown related node " + Util.formatAsHexa(relatedNodeId) + "!"));
                    }
                }
                if (dependentNode != null) {
                    computeWarningsAndCheckDependentNodes(dependentNode, computedNodeIds, changedNodeIds, nodeRemovalAllowed);
                }
            }
        }
    }

    private NetworkNodeEnhanced retrieveMyNode(Long nodeId, String bleAddress) {
        NetworkNodeEnhanced myNode;
        if (nodeId != null) {
            myNode = nodes.getNode(nodeId);
        } else if (bleAddress != null) {
            myNode = nodes.getNode(bleAddress);
        } else {
            throw new IllegalArgumentException("cannot update node if neither ID nor BLE address is specified");
        }
        return myNode;
    }

    @Override
    public int getNumberOfAnchors(short networkId) {
        return countNodes(networkId, NodeType.ANCHOR);
    }

    @Override
    public int getNumberOfTags(short networkId) {
        return countNodes(networkId, NodeType.TAG);
    }

    @Override
    public TrackMode getNodeTrackMode(long nodeId) {
        NetworkNodeEnhanced node = getNode(nodeId);
        return node == null ? null : node.getTrackMode();
    }

    @Override
    public TrackMode getNodeTrackMode(String bleAddress) {
        NetworkNodeEnhanced node = getNode(bleAddress);
        return node == null ? null : node.getTrackMode();
    }

    @Override
    public int getNumberOfDirectlyTrackedTags(short networkId) {
        return nodes.countNodesEnhanced((nn) ->
                nn.asPlainNode().isTag() &&
                Objects.equals(nn.asPlainNode().getNetworkId(), networkId) && nn.getTrackMode() == TrackMode.TRACKED_POSITION_AND_RANGING);
    }

    @Override
    public NetworkNodeEnhanced getNodeByShortId(short nodeId) {
        return nodes.getNodeByShortId(nodeId);
    }

    private int countNodes(short networkId, NodeType type) {
        return nodes.countNodes((nn) ->
                Objects.equals(nn.getNetworkId(), networkId) && nn.getType() == type);
    }


    @Override
    public NetworkNodeEnhanced getNode(String bleAddress) {
        return nodes.getNode(bleAddress);
    }

    @NotNull
    @Override
    public List<NetworkNodeEnhanced> getActiveNetworkNodes() {
        NetworkModel activeNetwork = getActiveNetwork();
        if (activeNetwork == null) {
            return Collections.emptyList();
        } // else:
        return getNetworkNodes(activeNetwork.getNetworkId());
    }

    @NotNull
    @Override
    public List<NetworkNodeEnhanced> getNetworkNodes(short networkId) {
        return nodes.getNodes((nn) -> Objects.equals(nn.getNetworkId(),networkId));
    }

    @NotNull
    @Override
    public List<NetworkNodeEnhanced> getNetworkNodes(Predicate<NetworkNode> filter) {
        return nodes.getNodes(filter);
    }

    @Override
    public Long bleToId(String nodeBleAddress) {
        NetworkNodeEnhanced eNode = nodes.getNode(nodeBleAddress);
        return eNode == null ? null : eNode.asPlainNode().getId();
    }

    @Override
    public String idToBle(Long nodeId) {
        NetworkNodeEnhanced eNode = nodes.getNode(nodeId);
        return eNode == null ? null : eNode.getBleAddress();
    }

    @Override
    public NetworkNodeEnhanced getNode(long nodeId) {
        return nodes.getNode(nodeId);
    }

    @Override
    public void removeActiveNetwork() {
        // do the remove carefully
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        if (activeNetworkId == null) {
            // skip this
            return;
        }
        // remove the network from stack
        removeNetwork(activeNetworkId);
    }

    @Override
    public void undoNetworkRemove(short networkId) {
        networkModelManager.undoNetworkRemove(networkId);
    }

    @Override
    public boolean hasNetworkByName(String networkName) {
        return networkModelManager.hasNetworkByName(networkName);
    }

    @Override
    public NetworkModel getActiveNetwork() {
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        if (activeNetworkId == null) {
            return null;
        } // else:
        return networkModelManager.getNetworks().get(activeNetworkId);
    }

    @NotNull
    @Override
    public NetworkModel getActiveNetworkNullSafe() {
        NetworkModel n = getActiveNetwork();
        Preconditions.checkNotNull(n);
        return n;
    }

    @Override
    public boolean activeNetworkContainsNode(String bleAddress) {
        NetworkNodeEnhanced nodeEnhanced = nodes.getNode(bleAddress);
        return nodeEnhanced != null
                && Objects.equals(appPreferenceAccessor.getActiveNetworkId(), nodeEnhanced.asPlainNode().getNetworkId());
    }

    @Override
    public boolean isNodePersisted(long nodeId) {
        NetworkNodeEnhanced nodeEnhanced = nodes.getNode(nodeId);
        return nodeEnhanced != null
                && networkModelManager.hasNetwork(nodeEnhanced.asPlainNode().getNetworkId());
    }

    @Override
    public boolean isNodePersisted(String bleAddress) {
        NetworkNodeEnhanced nodeEnhanced = nodes.getNode(bleAddress);
        return nodeEnhanced != null
                && networkModelManager.hasNetwork(nodeEnhanced.asPlainNode().getNetworkId());
    }

    @Override
    public void updateAnchorDistances(long nodeId, List<RangingAnchor> distances) {
        NetworkNodeEnhanced node = nodes.getNode(nodeId);
        if (node == null || !node.isAnchor()) {
            return;
        } // else:
        AnchorNode anchor = (AnchorNode) node.asPlainNode();
        if(!Objects.equals(anchor.getDistances(), distances)) {
            ((NetworkNodePropertySetter) anchor).setProperty(NetworkNodeProperty.ANCHOR_DISTANCES, distances);
            if (isNodePersisted(nodeId)) {
                // broadcast now that the node distances have changed
                InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdated(node);
            }
        }
    }

    @Override
    public void updateAnchorPosition(long nodeId, Position position) {
        NetworkNodeEnhanced node = nodes.getNode(nodeId);
        if (node == null || !node.isAnchor()) {
            return;
        } // else:
        AnchorNode anchor = (AnchorNode) node.asPlainNode();
        if(!Objects.equals(anchor.extractPositionDirect(), position)) {
            anchor.setPosition(position);
            if (isNodePersisted(nodeId)) {
                // we should recompute warnings of this and all related nodes
                HashSet<Long> changedNodeIds = new HashSet<>();
                // optimization, position change can influence only anchor's warnings
                computeWarningsAndCheckDependentNodes(node, new HashSet<>(), changedNodeIds, false);
                // broadcast now that the node position has changed
                InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdated(node);
                // broadcast now that the dependent node warnings have changed
                if (!changedNodeIds.isEmpty()) {
                    reportChangedNodes(changedNodeIds);
                }
            }
        }
    }

    @Override
    public void updateTagLocationData(long nodeId, LocationData locationData) {
        NetworkNodeEnhanced node = nodes.getNode(nodeId);
        if (node == null || !node.isTag()) {
            return;
        } // else:
        TagNode tag = (TagNode) node.asPlainNode();
        if (!Objects.equals(tag.getLocationData(), locationData)) {
            ((NetworkNodePropertySetter) tag).setProperty(NetworkNodeProperty.TAG_LOCATION_DATA, locationData);
            if (isNodePersisted(nodeId)) {
                // broadcast now that the node location data have changed
                InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeUpdated(node);
            }
        }
        // no need to recompute warnings (for tags)
    }

    @Override
    public void updateLastSeen(String bleAddress) {
        NetworkNodeEnhanced nodeEnhanced = nodes.getNode(bleAddress);
        if (nodeEnhanced == null) {
            return;
        }
        nodeEnhanced.touchLastSeen();
    }

    @Override
    public void removeNetwork(short networkId) {
        removeNetwork(networkId, true);
    }

    @Override
    public void removeNetwork(short networkId, boolean explicitUserAction) {
        if (Constants.DEBUG) {
            log.d("removeNetwork: " + "networkId = [" + networkId + "], explicitUserAction = [" + explicitUserAction + "]");
        }
        // check if the network to be removed is the active network
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        if (activeNetworkId != null && activeNetworkId == networkId) {
            // do the remove carefully
            // get the previous active network id
            Short newActiveNetworkId = activeNetworkIdStack.pop();
            if (newActiveNetworkId == null) {
                // go one-by-one, find the first available
                for (Map.Entry<Short, NetworkModel> keyValue : networkModelManager.getNetworks().entrySet()) {
                    Short nuid = keyValue.getKey();
                    if (!nuid.equals(networkId)) {
                        newActiveNetworkId = nuid;
                        break;
                    }
                }
            }
            // set up the new active network (might be null)
            appPreferenceAccessor.setActiveNetworkId(newActiveNetworkId);
        }
        // now we can remove the network
        networkModelManager.removeNetwork(networkId, explicitUserAction);
           // keep the nodes
    }

    void setTransientNodeChangeListener(TransientNodeChangeHandler callback) {
        this.transientNodeChangeListener = callback;
    }

    private void persistCheckpoint() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(networkModelManager.getNetworks(), "cannot persist without previous load (getNetworks())");
        }
        // save only those nodes, which belong to a declared network
        Set<Short> knownNetworkIds = networkModelManager.getNetworks().keySet();
        storage.save(nodes.getNodes((n) -> knownNetworkIds.contains(n.getNetworkId())),
                networkModelManager.getNetworks().values());
    }


    private Runnable persistRunnable = this::persistCheckpoint;

    private void schedulePersist() {
        // unschedule
        ArgoApp.uiHandler.removeCallbacks(persistRunnable);
        // (re)schedule
        ArgoApp.uiHandler.postDelayed(persistRunnable, PERSIST_DELAY_MS);
    }

    private void onLoadedFromStorage(Collection<NetworkNodeEnhanced> nodeList,
                                     Collection<NetworkModel> networkList) {
        if (Constants.DEBUG) {
            log.d("onNetworksLoaded: " + "nodeList = [" + nodeList + "], networkList = [" + networkList + "]");
        }
        // set up nodes
        this.nodes = EnhancedNetworkNodeContainerFactory.createContainer(nodeList);
        // set up networks
        this.networkModelManager.init(networkList);
        Stream.of(nodes.getNodes(false))
                // compute initial warnings
                .peek(nne -> ((NetworkNodeEnhancedImpl) nne).setWarnings(computeWarnings(nne.asPlainNode())))
                .map(NetworkNodeEnhanced::asPlainNode)
                .filter((nn) -> nn.isAnchor() && nn.extractPositionDirect() != null)
                // set initial anchor positions to logger
                .forEach((node) -> locationDataLogger.setInitialPosition(node.getId(), node.getBleAddress(), node.extractPositionDirect()));
    }

    private List<NodeWarning> computeWarnings(NetworkNode networkNode) {
        List<NodeWarning> lst = new ArrayList<>();
        UwbMode uwbMode = networkNode.getUwbMode();
        if (uwbMode == UwbMode.OFF) {
            lst.add(NodeWarning.newUwbOffWarning());
        } else if (uwbMode == UwbMode.PASSIVE && networkNode.isTag()) {
            lst.add(NodeWarning.newTagUwbPassiveWarning());
        } else if (networkNode.isAnchor() && networkNode.getUwbMode() == UwbMode.ACTIVE) {
            List<NetworkNode> samePositionAnchors = anyOtherActiveAnchorSameNetworkSamePosition((AnchorNode) networkNode);
            if (!samePositionAnchors.isEmpty()) {
                lst.add(NodeWarning.newAnchorSamePositionWarning(Stream.of(samePositionAnchors).map(NetworkNode::getId).collect(Collectors.toSet())));
            }
        }
        return lst;
    }

    private @NotNull List<NetworkNode> anyOtherActiveAnchorSameNetworkSamePosition(AnchorNode nn) {
        Position nnPosition = nn.extractPositionDirect();
        if (nnPosition == null || nn.getNetworkId() == null || nn.getNetworkId() == 0) {
            return Collections.emptyList();
        } // else: there is set position and network
        return Stream.of(getNetworkNodes(nn.getNetworkId()))
                .filter(onne -> {
                    NetworkNode onn = onne.asPlainNode();
                    if (onn.isAnchor() && onn.getUwbMode() == UwbMode.ACTIVE) {
                        AnchorNode ann = (AnchorNode) onn;
                        Position position = ann.extractPositionDirect();
                        return position != null
                                && !Objects.equals(nn.getId(), onn.getId())
                                && nnPosition.equalsInCoordinates(position);
                    } // else:
                    return false;
                })
                .map(NetworkNodeEnhanced::asPlainNode)
                .collect(Collectors.toList());
    }

    @Override
    public void forgetNode(Long nodeId, boolean userInitiated) {
        NetworkNodeEnhanced node = getNode(nodeId);
        Preconditions.checkNotNull(node, "node " + nodeId + " is not known!");
        // do the remove
        nodes.removeNode(nodeId);
        //
        recomputeDependentNodesWarnings(node);
        Short networkId = node.asPlainNode().getNetworkId();
        if (networkId != null) {
            // broadcast that the node has been removed from the network
            InterfaceHub.getHandlerHub(IhPersistedNodeChangeListener.class).onNodeForgotten(nodeId, networkId, userInitiated);
        }
        // and schedule persist
        schedulePersist();
    }

    private void reportChangedNodes(Set<Long> changedNodeIds) {
        for (Long changedNodeId : changedNodeIds) {
            InterfaceHub.getHandlerHub(IhEnhancedNodePropertiesChangeListener.class).onPropertiesChanged(getNode(changedNodeId));
        }
    }

}