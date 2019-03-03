/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.EnhancedNetworkNodeContainer;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.impl.EnhancedNetworkNodeContainerImpl;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.util.Util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.argoapi.ext.NodeFactory.newNodeCopy;

/**
 * Helps to manage different item/indexes/item types.
 *
 * This list is optimized for small number of items held.
 *
 */
class PolymorphicDiscoveryList {
    private static final ComponentLog log = new ComponentLog(PolymorphicDiscoveryList.class);
    // members
    private final PersistedNetworkResolver persistedNetworkResolver;
    private List<DiscoveryListBean> list;
    private ModificationCallback callback;


    interface PersistedNetworkResolver {

        String getNetworkName(short networkId);

        int getAnchorCount(short networkId);

        int getTagCount(short networkId);

        boolean isNodePersisted(long nodeId);

    }

    private enum ItemCategory {
        EXISTING,
        NEW
    }

    // order matters!
    enum ItemType {
        DECLARED_NETWORK(ItemCategory.EXISTING),
        UNKNOWN_NETWORK(ItemCategory.NEW),
        UNKNOWN_NODE(ItemCategory.NEW);

        public final ItemCategory category;

        ItemType(ItemCategory category) {
            this.category = category;
        }
    }

    // classes
    class DiscoveryListBean {
        final ItemType type;

        DiscoveryListBean(ItemType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "ListItem{" +
                    "type=" + type +
                    ", data=" + getData() +
                    "}";
        }

        public String getData() {
            return "";
        }
    }

    class NodeListItem extends DiscoveryListBean {
        NetworkNode node;

        NodeListItem(NetworkNode node) {
            super(ItemType.UNKNOWN_NODE);
            this.node = node;
        }

        @Override
        public String getData() {
            return node.getLabel();
        }
    }

    class UnknownNetworkListItem extends DiscoveryListBean {
        // network id
        public final short networkId;
        // container
        EnhancedNetworkNodeContainer nodeContainer;

        UnknownNetworkListItem(short networkId) {
            super(ItemType.UNKNOWN_NETWORK);
            this.networkId = networkId;
            this.nodeContainer = new EnhancedNetworkNodeContainerImpl();
        }

        void addNode(@NotNull NetworkNode node) {
            nodeContainer.addNode(node);
        }

        @Override
        public String getData() {
            return "" + Util.formatAsHexa(networkId);
        }
    }

    class DeclaredNetworkListItem extends DiscoveryListBean {
        // network id
        public final short networkId;

        DeclaredNetworkListItem(short networkId) {
            super(ItemType.DECLARED_NETWORK);
            this.networkId = networkId;
        }

        public String getNetworkName() {
            return persistedNetworkResolver.getNetworkName(networkId);
        }

        int getAnchorCount() {
            return persistedNetworkResolver.getAnchorCount(networkId);
        }

        int getTagCount() {
            return persistedNetworkResolver.getTagCount(networkId);
        }

        @Override
        public String getData() {
            return "" + Util.formatAsHexa(networkId);
        }
    }

    interface ModificationCallback {

        void onItemInserted(DiscoveryListBean item);

        void onItemUpdated(DiscoveryListBean item);

        void onItemRemoved(DiscoveryListBean item);

    }

    PolymorphicDiscoveryList(PersistedNetworkResolver persistedNetworkResolver) {
        this.persistedNetworkResolver = persistedNetworkResolver;
        this.list = new ArrayList<>(8);
    }

    private boolean isNodePersisted(NetworkNode networkNode) {
        return persistedNetworkResolver.isNodePersisted(networkNode.getId());
    }

    public DiscoveryListBean get(int position) {
        return list.get(position);
    }

    public void setCallback(ModificationCallback callback) {
        this.callback = callback;
    }

    int size() {
        return this.list.size();
    }

    void addTransientNode(@NotNull NetworkNode node) {
        addTransientNode(node, true);
    }

    void addTransientNode(@NotNull NetworkNode node, boolean notifyCallback) {
        // create a deep copy
        node = newNodeCopy(node);
        // first check which kind of change this is
        Short networkId = node.getNetworkId();
        if (networkId != null) {
            _addOrUpdateUnknownNetwork(node, notifyCallback, networkId);
        } else {
            _addNodeInternal(node, notifyCallback);
        }
    }

    private void _addNodeInternal(NetworkNode node, boolean notifyCallback) {
        // this is a solo node
        NodeListItem nodeListItem = new NodeListItem(node);
        // we are adding at the very end
        list.add(nodeListItem);
        // post-processing
        // callback?
        if (notifyCallback) {
            callback.onItemInserted(nodeListItem);
        }
        if (Constants.DEBUG) {
            log.d("list after insertion: " + list);
        }
    }

    private void _addOrUpdateUnknownNetwork(@NotNull NetworkNode node, boolean notifyCallback, Short networkId) {
        // this node is already part of some network
        int position[] = { -1 };
        UnknownNetworkListItem unknownNetworkListItem = getUnknownNetworkById(networkId, position);
        if (unknownNetworkListItem == null) {
            unknownNetworkListItem = new UnknownNetworkListItem(networkId);
            unknownNetworkListItem.nodeContainer.addNode(node);
            // find the position where to insert the network list item
            ListIterator<DiscoveryListBean> it = getNextFreePositionBefore(ItemType.UNKNOWN_NODE);
            it.add(unknownNetworkListItem);
            // call the callback
            // callback?
            if (notifyCallback) {
                callback.onItemInserted(unknownNetworkListItem);
            }
            if (Constants.DEBUG) {
                log.d("list after insertion: " + list);
            }
        } else {
            // we already have this network
            unknownNetworkListItem.addNode(node);
            if (notifyCallback) {
                // notify about the update
                callback.onItemUpdated(unknownNetworkListItem);
            }
        }
    }

    private ListIterator<DiscoveryListBean> getNextFreePositionBefore(ItemType beforeItemType) {
        ListIterator<DiscoveryListBean> it = list.listIterator();
        int ordinal = beforeItemType.ordinal();
        while (it.hasNext()) {
            if (it.next().type.ordinal() >= ordinal) {
                // we have reached the limit
                it.previous();
                return it;
            }
        }
        // we haven't found the given itemtype
        return it;
    }

    private UnknownNetworkListItem getUnknownNetworkById(short networkId, int position[]) {
        int idx = 0;
        for (DiscoveryListBean bean : list) {
            if (bean.type == ItemType.UNKNOWN_NETWORK && ((UnknownNetworkListItem) bean).networkId == networkId) {
                if (position != null && position.length == 1) {
                    position[0] = idx;
                }
                return (UnknownNetworkListItem) bean;
            }
            idx++;
        }
        return null;
    }

    void removeDiscoveredNode(long nodeId) {
        // check if this is node which was part of an unknown network
        ListIterator<DiscoveryListBean> it = list.listIterator();
        while (it.hasNext()) {
            DiscoveryListBean item = it.next();
            if (item.type == ItemType.UNKNOWN_NETWORK) {
                EnhancedNetworkNodeContainer container = ((UnknownNetworkListItem) item).nodeContainer;
                NetworkNodeEnhanced node = container.getNode(nodeId);
                if (node != null) {
                    // yes, it's ours
                    container.removeNode(nodeId);
                    if (container.isEmpty()) {
                        // complete removal
                        removeItemBehindIterator(it, item);
                    } else {
                        // just change/update
                        callback.onItemUpdated(item);
                    }
                    return;
                }
            } else if (item.type == ItemType.UNKNOWN_NODE) {
                // the item is a node, check if it matches
                if (nodeId == ((NodeListItem) item).node.getId()) {
                    removeItemBehindIterator(it, item);
                    return;
                }
            }
        }
    }

    private void removeItemBehindIterator(ListIterator<DiscoveryListBean> it,
                                          DiscoveryListBean itemToRemove) {
        it.remove();
        // notify the callback
        callback.onItemRemoved(itemToRemove);
        if (Constants.DEBUG) {
            log.d("list after removal: " + list);
        }
    }

    void updateDiscoveredNode(@NotNull NetworkNode node) {
        Long nodeId = node.getId();

        // lookup the node by id
        for (DiscoveryListBean bean : list) {
            if (bean.type == ItemType.UNKNOWN_NETWORK) {
                EnhancedNetworkNodeContainer nodeContainer = ((UnknownNetworkListItem) bean).nodeContainer;
                NetworkNodeEnhanced nne = nodeContainer.getNode(nodeId);
                if (nne != null) {
                    NetworkNode storedNode = nne.asPlainNode();
                    // scenarios:
                    // A-1. transient network -> the same transient network
                    // A-2. transient network -> another transient network
                    // A-3. transient network -> persistent network
                    // A-4. transient network -> no network
                    // check if we are still part of the same network
                    if (storedNode.getNetworkId().equals(node.getNetworkId())) {
                        // case A-1. simply update the node and we are done (no visual changes in discovery list)
                        nodeContainer.addNode(newNodeCopy(node));
                        return;
                    } else {
                        // the node is not part of the same network
                        // cases A-2.3.4.
                        removeDiscoveredNode(nodeId);
                        // check if we should insert it or keep it removed
                        if (!isNodePersisted(node)) {
                            // cases A-2.4.
                            // we will show it somewhere else
                            addTransientNode(node);
                        }
                    }
                    // we are done with update
                    return;
                } // else: this network does not know anything about the node
            } else if (bean.type == ItemType.UNKNOWN_NODE) {
                NodeListItem nodeListItem = (NodeListItem) bean;
                if (nodeListItem.node.getId().equals(nodeId)) {
                    // we haven't found the node in any of our transient networks, it could be that:
                    // solo node changes:
                    // B-1. no network -> persistent network
                    // B-2. no network -> transient network
                    // B-3. no network -> no network
                    if (isNodePersisted(node)) {
                        // case B-1. just remove
                        removeDiscoveredNode(nodeId);
                    } else if (node.getNetworkId() == null) {
                        // case B-3. still no network, just update the properties
                        nodeListItem.node = node;
                        callback.onItemUpdated(bean);
                    } else {
                        // case B-2. remove the node
                        removeDiscoveredNode(nodeId);
                        addTransientNode(node);
                    }
                    // we are done
                    return;
                }
            }
        } // else: the node was not found in neither network nor discovered nodes it might have been persisted node
        // persisted node changes:
        // C-1. persistent network -> persistent network
        // C-2. persistent network -> transient network
        // C-3. persistent network -> no network
        if (!isNodePersisted(node)) {
            // cases C-2.3.
            addTransientNode(node);
        } // else: case C-1. - do nothing (node is part of a known known network anyway)
    }

    void clear() {
        list.clear();
    }

    void declarePersistentNetwork(NetworkModel networkModel) {
        ListIterator<DiscoveryListBean> it = getNextFreePositionBefore(ItemType.UNKNOWN_NETWORK);
        // insert the new network
        DeclaredNetworkListItem listItem = new DeclaredNetworkListItem(networkModel.getNetworkId());
        it.add(listItem);
        if (Constants.DEBUG) {
            log.d("list after insertion: " + list);
        }
    }

    void onDeclaredNetworkUpdate(short networkId) {
        // find the declared network
        for (DiscoveryListBean item : list) {
            if (item.type == ItemType.DECLARED_NETWORK && ((DeclaredNetworkListItem) item).networkId == networkId) {
                // properties get update on request
                callback.onItemUpdated(item);
                break;
            }
        }
    }

    void onDeclaredNetworkAdded(short networkId) {
        ListIterator<DiscoveryListBean> it = getNextFreePositionBefore(ItemType.UNKNOWN_NETWORK);
        // insert the new network
        DeclaredNetworkListItem listItem = new DeclaredNetworkListItem(networkId);
        it.add(listItem);
        // callback
        callback.onItemInserted(listItem);
        if (Constants.DEBUG) {
            log.d("list after insertion: " + list);
        }
    }

}
