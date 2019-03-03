/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter.discovery;

import android.support.annotation.NonNull;

import com.annimon.stream.function.Predicate;
import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.runner.NetworkAssignmentRunner;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment;
import com.decawave.argomanager.ui.dialog.NewNetworkNameDialogFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.util.NetworkIdGenerator;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */
public class DiscoveryListAdapter extends FlexibleAdapter<IFlexible>{
    private static final ComponentLog log = new ComponentLog(DiscoveryListAdapter.class);
    // dependencies
    private final NetworkNodeManager networkNodeManager;
    private final DiscoveryManager discoveryManager;
    private final MainActivity mainActivity;
    private final AppPreferenceAccessor appPreferenceAccessor;
    private final Supplier<NetworkAssignmentRunner> networkAssignmentRunnerSupplier;
    // polymorphic list
    private PolymorphicDiscoveryList polymorphicList;
    private boolean discoveryRunning;
    private Long lastSelectedNetworkNodeId;
    // prebuilt headers
    private DlSectionHeader networksHeader, unknownNetworkHeader, unassignedNodeHeader;
    // reusable comparator
    private static final Comparator<IFlexible> ITEM_COMPARATOR_BY_TYPE = (t1, t2) -> {
        int t1Ordinal, t2Ordinal;
        // progress header is always the first
        if (t1 instanceof DlProgressHeader) {
            if (Constants.DEBUG) {
                Preconditions.checkState(!(t2 instanceof DlProgressHeader), "there cannot be two progress headers");
            }
            return -1;
        } else if (t2 instanceof DlProgressHeader) {
            return 1;
        }
        //
        if (t1 instanceof DlSectionHeader) {
            t1Ordinal = ((DlSectionHeader) t1).sectionType.ordinal() * 2;
        } else {
            t1Ordinal = ((DlFlexibleItem) t1).item.type.ordinal() * 2 + 1;
        }
        if (t2 instanceof DlSectionHeader) {
            t2Ordinal = ((DlSectionHeader) t2).sectionType.ordinal() * 2;
        } else {
            t2Ordinal = ((DlFlexibleItem) t2).item.type.ordinal() * 2 + 1;
        }
        // we will consider only type - to let the item go to proper section
        return t1Ordinal - t2Ordinal;
    };

    public DiscoveryListAdapter(NetworkNodeManager networkNodeManager,
                                DiscoveryManager discoveryManager,
                                MainActivity mainActivity,
                                AppPreferenceAccessor appPreferenceAccessor,
                                Supplier<NetworkAssignmentRunner> networkAssignmentRunnerSupplier) {
        super(Collections.emptyList());
        //
        this.networkNodeManager = networkNodeManager;
        this.discoveryManager = discoveryManager;
        this.mainActivity = mainActivity;
        this.appPreferenceAccessor = appPreferenceAccessor;
        this.networkAssignmentRunnerSupplier = networkAssignmentRunnerSupplier;
        // init headers
        this.networksHeader = new DlSectionHeader(PolymorphicDiscoveryList.ItemType.DECLARED_NETWORK);
        this.unknownNetworkHeader = new DlSectionHeader(PolymorphicDiscoveryList.ItemType.UNKNOWN_NETWORK);
        this.unassignedNodeHeader = new DlSectionHeader(PolymorphicDiscoveryList.ItemType.UNKNOWN_NODE);
    }

    private boolean isAssigning() {
        NetworkAssignmentRunner runner = this.networkAssignmentRunnerSupplier.get();
        return runner != null && runner.getOverallStatus() == NetworkAssignmentRunner.OverallStatus.ASSIGNING;
    }

    public void handleRegularOnClick(int position) {
        IFlexible item = getItem(position);
        if (Constants.DEBUG) {
            // must be either node of network
            Preconditions.checkState(item instanceof DlFlexibleItem, "unexpected item class: " + item);
        }
        if (!isAssigning()) {
            // process click only if we are not assigning
            DlFlexibleItem dlItem = (DlFlexibleItem) item;
            //noinspection ConstantConditions
            switch (dlItem.item.type) {
                case DECLARED_NETWORK:
                    onDeclaredNetworkClick(((DlItemDeclaredNetwork) dlItem).item.networkId);
                    break;
                case UNKNOWN_NETWORK:
                    onUnknownNetworkClick(((DlItemUnknownNetwork) dlItem).item.networkId);
                    break;
                case UNKNOWN_NODE:
                    onNetworkNodeClick(((DlItemUnknownNode) dlItem).item.node.getId());
                    break;
                default:
                    throw new IllegalArgumentException("unexpected item type: " + dlItem.item.type);
            }
        }
    }

    // this is not handled properly in FlexibleViewHolder line 225 (see https://github.com/davideas/FlexibleAdapter/issues/416)
    @Override
    public boolean isSelectable(int position) {
        // an item is selectable only if it is an unknown node
        return isItemUnknownNode(position);
    }

    private PolymorphicDiscoveryList createPolymorphicList() {
        PolymorphicDiscoveryList list = new PolymorphicDiscoveryList(new PolymorphicDiscoveryList.PersistedNetworkResolver() {
            @Override
            public String getNetworkName(short networkId) {
                return networkNodeManager.getNetworks().get(networkId).getNetworkName();
            }

            @Override
            public int getAnchorCount(short networkId) {
                return networkNodeManager.getNumberOfAnchors(networkId);
            }

            @Override
            public int getTagCount(short networkId) {
                return networkNodeManager.getNumberOfTags(networkId);
            }

            @Override
            public boolean isNodePersisted(long nodeId) {
                return networkNodeManager.isNodePersisted(nodeId);
            }
        });
        list.setCallback(new PolymorphicDiscoveryList.ModificationCallback() {

            ///////////////////////////////////////////////////////////////////////////
            // translated insert/update/delete event handling
            ///////////////////////////////////////////////////////////////////////////


            @Override
            public void onItemInserted(PolymorphicDiscoveryList.DiscoveryListBean item) {
                if (Constants.DEBUG) log.d("onItemInserted() called with: " + "item = [" + item + "]");
                DlFlexibleItem flexibleItem = asFlexibleItem(item);
                int pos = addItemToSection(flexibleItem, flexibleItem.getHeader(), ITEM_COMPARATOR_BY_TYPE);
                // workaround flexible list bug #451 (https://github.com/davideas/FlexibleAdapter/issues/451)
                int prevPos = pos - 1;
                IFlexible prevListItem = getItem(prevPos);
                if (prevListItem instanceof DlFlexibleItem) {
                    if (item.type == ((DlFlexibleItem) prevListItem).item.type) {
                        // notify about the change
                        updateItem(prevPos, prevListItem, null);
                    }
                }
                if (isSummaryShowing()) updateSummary();
            }

            @Override
            public void onItemUpdated(PolymorphicDiscoveryList.DiscoveryListBean item) {
                if (Constants.DEBUG) log.d("onItemUpdated() called with: " + "item = [" + item + "]");
                updateItem(asFlexibleItem(item));
                if (item.type == PolymorphicDiscoveryList.ItemType.UNKNOWN_NETWORK && isSummaryShowing()) {
                    // we need the isSummaryShowing because when a network is named, we still receive callbacks about all nodes in the network
                    // (that they are now persisted)
                    // possibly update also summary/status/count
                    updateSummary();
                }
            }

            @Override
            public void onItemRemoved(PolymorphicDiscoveryList.DiscoveryListBean item) {
                if (Constants.DEBUG) log.d("onItemRemoved() called with: " + "item = [" + item + "]");
                removeItem(getGlobalPositionOf(asFlexibleItem(item)));
                // update also status/count
                if (isSummaryShowing()) updateSummary();
            }

        });
        return list;
    }

    private void updateSummary() {
        if (Constants.DEBUG) {
            log.d("updateSummary()");
            Preconditions.checkNotNull(polymorphicList);
            Preconditions.checkState(isSummaryShowing());
        }
        // update also status/count
        updateItem(0, newSummaryItem(), null);
    }



    @NonNull
    private DlProgressHeader newSummaryItem() {
        return new DlProgressHeader(discoveryRunning, getNumberOfDiscoveredNodes());
    }

    private @NotNull DlFlexibleItem asFlexibleItem(PolymorphicDiscoveryList.DiscoveryListBean item) {
        switch (item.type) {
            case DECLARED_NETWORK:
                return new DlItemDeclaredNetwork((PolymorphicDiscoveryList.DeclaredNetworkListItem) item, networksHeader);
            case UNKNOWN_NETWORK:
                return new DlItemUnknownNetwork((PolymorphicDiscoveryList.UnknownNetworkListItem) item, unknownNetworkHeader);
            case UNKNOWN_NODE:
                return new DlItemUnknownNode((PolymorphicDiscoveryList.NodeListItem) item, unassignedNodeHeader, networkAssignmentRunnerSupplier);
            default:
                throw new IllegalStateException("unexpected item type " + item.type);
        }
    }

    private boolean anyDiscoveredItems() {
        boolean b = getNumberOfDiscoveredNodes() > 0;
        if (Constants.DEBUG) {
            log.d("anyDiscoveredItems: returning " + b);
        }
        return b;
    }

    private int getNumberOfDiscoveredNodes() {
        // this is ridiculous - but we need to keep the number of discovered nodes to be consistent with:
        // 1. what the user can see (once he enters the screen - immediately after discovery has started, when there is actually no node discovered)
        // 2. what the user has seen once the discovery is stopped
        int max = Math.max(discoveryManager.getDiscoveredTransientOnlyNodes().size(), discoveryManager.getNumberOfDiscoverySessionNodes());
        if (Constants.DEBUG) {
            log.d("getNumberOfDiscoveredNodes: returning " + max);
        }
        return max;
    }

    public void onDiscoveryStateChanged(boolean running) {
        if (Constants.DEBUG) {
            log.d("onDiscoveryStateChanged: " + "running = [" + running + "]");
        }
        if (discoveryRunning != running) {
            discoveryRunning = running;
            if (polymorphicList == null) {
                // no need to customize the view
                return;
            }
            boolean anyDiscoveredItems = anyDiscoveredItems();
            if (!running) {
                if (!anyDiscoveredItems) {
                    // just show that we have not found anything
                    updateSummary();
                } else {
                    // remove the title just show the discovered elements
                    removeItem(0);
                }
            } else {
                // the discovery is running (just started)
                if (anyDiscoveredItems) {
                    addItem(0, newSummaryItem());
                } else {
                    // no items were found previously, just change the title
                    updateSummary();
                }
            }
        }
    }

    private boolean isSummaryShowing() {
        return discoveryRunning || !anyDiscoveredItems();
    }

    public void removeDiscoveredNode(long nodeId) {
        if (Constants.DEBUG) {
            log.d("removeDiscoveredNode: " + "nodeId = [" + nodeId + "]");
        }
        if (polymorphicList != null) polymorphicList.removeDiscoveredNode(nodeId);
    }

    public void updateDiscoveredNode(NetworkNode node) {
        if (Constants.DEBUG) {
            log.d("updateDiscoveredNode: " + "node = [" + node + "]");
        }
        if (polymorphicList != null) polymorphicList.updateDiscoveredNode(node);
    }

    public void insertDiscoveredNode(@NotNull NetworkNode node) {
        if (Constants.DEBUG) {
            log.d("insertDiscoveredNode: " + "node = [" + node + "]");
        }
        if (polymorphicList != null) polymorphicList.addTransientNode(node);
    }

    public Long getLastSelectedNetworkNodeId() {
        return lastSelectedNetworkNodeId;
    }

    public boolean isInMultiSelectMode() {
        return getMode() == Mode.MULTI;
    }

    public void setInitialNodeSet() {
        if (Constants.DEBUG) {
            log.d("setInitialNodeSet");
        }
        // make sure there is nothing in the polymorphic discovery list
        if (polymorphicList == null) {
            polymorphicList = createPolymorphicList();
        } else {
            polymorphicList.clear();
        }
        // process the list one-by-one, create cloned copies
        for (NetworkNode node : discoveryManager.getDiscoveredTransientOnlyNodes()) {
            if (Constants.DEBUG) {
                Preconditions.checkState(!networkNodeManager.isNodePersisted(node.getId()), "node " + node + " is persisted?!");
            }
            polymorphicList.addTransientNode(NodeFactory.newNodeCopy(node), false);
        }
        List<NetworkModel> networks = new ArrayList<>(networkNodeManager.getNetworks().values());
        Collections.sort(networks, Util.NETWORK_NAME_COMPARATOR);
        for (NetworkModel networkModel : networks) {
            polymorphicList.declarePersistentNetwork(networkModel);
        }
        updateDataSet(asFlexibleListItems());
    }

    private List<IFlexible> asFlexibleListItems() {
        List<IFlexible> items = new ArrayList<>();
        if (isSummaryShowing()) {
            // just show that we are running/have not found anything
            items.add(newSummaryItem());
        }
        for (int i = 0; i < polymorphicList.size();i++) {
            items.add(asFlexibleItem(polymorphicList.get(i)));
        }
        return items;
    }

    private void onUnknownNetworkClick(short networkId) {
        lastSelectedNetworkNodeId = null;
        NewNetworkNameDialogFragment.showDialog(mainActivity.getSupportFragmentManager(), null, networkId, false);
    }
    
    private void onDeclaredNetworkClick(short networkId) {
        lastSelectedNetworkNodeId = null;
        appPreferenceAccessor.setActiveNetworkId(networkId);
        // go to network overview
        mainActivity.showFragment(FragmentType.OVERVIEW);
    }

    private void onNetworkNodeClick(long nodeId) {
        lastSelectedNetworkNodeId = nodeId;
        letUserChooseNetwork(networkNodeManager, mainActivity);
    }

    public static void letUserChooseNetwork(NetworkNodeManager networkNodeManager, MainActivity mainActivity) {
        if (networkNodeManager.getNetworks().size() == 0) {
            NewNetworkNameDialogFragment.showDialog(mainActivity.getSupportFragmentManager(), null, NetworkIdGenerator.newNetworkId(), true);
        } else {
            NetworkPickerDialogFragment.showDialog(mainActivity.getSupportFragmentManager(), (String) null);
        }
    }

    public void onPersistentNodeUpdatedAndOrAddedToNetwork(short networkId) {
        if (Constants.DEBUG) {
            log.d("onPersistentNodeUpdatedAndOrAddedToNetwork: " + "networkId = [" + networkId + "]");
        }
        polymorphicList.onDeclaredNetworkUpdate(networkId);
    }

    public void onPersistentNodeUpdatedAndRemovedFromNetwork(short networkId) {
        if (Constants.DEBUG) {
            log.d("onPersistentNodeUpdatedAndRemovedFromNetwork: " + "networkId = [" + networkId + "]");
        }
        polymorphicList.onDeclaredNetworkUpdate(networkId);
    }

    public void onPersistentNodeUpdated(NetworkNodeEnhanced node) {
        if (Constants.DEBUG) {
            log.d("onPersistentNodeUpdated: " + "node = [" + node + "]");
        }
        // it might have been anchor <-> tag change
        polymorphicList.onDeclaredNetworkUpdate(node.asPlainNode().getNetworkId());
    }

    public void onNetworkAdded(short networkId) {
        if (Constants.DEBUG) {
            log.d("onNetworkAdded: " + "networkId = [" + networkId + "]");
        }
        polymorphicList.onDeclaredNetworkAdded(networkId);
    }


    public void onNetworkUpdated(short networkId) {
        if (Constants.DEBUG) {
            log.d("onNetworkUpdated: " + "networkId = [" + networkId + "]");
        }
        polymorphicList.onDeclaredNetworkUpdate(networkId);
    }

    public boolean isItemUnknownNode(int position) {
        IFlexible item = getItem(position);
        if (item == null || item instanceof DlProgressHeader || item instanceof DlSectionHeader) {
            // simply ignore
            return false;
        }
        // must be either node or network
        if (Constants.DEBUG) {
            Preconditions.checkState(item instanceof DlFlexibleItem, "unexpected item class: " + item);
        }
        DlFlexibleItem dlItem = (DlFlexibleItem) item;
        //noinspection ConstantConditions
        return dlItem.item.type == PolymorphicDiscoveryList.ItemType.UNKNOWN_NODE;
    }

    public List<Long> getSelectedNetworkNodeIds() {
        int count = getItemCount();
        List<Long> ids = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            if (isItemUnknownNode(i)) {
                IFlexible dlItem = getItem(i);
                Preconditions.checkNotNull(dlItem, "cannot get item on position " + i);
                if (isSelected(i)) {
                    ids.add(((DlItemUnknownNode) dlItem).item.node.getId());
                }
            }
        }
        return ids;
    }

    public void onNodeStatusChanged(@NotNull String bleAddress) {
        if (Constants.DEBUG) {
            log.d("onNodeStatusChanged: " + "bleAddress = [" + bleAddress + "]");
        }
        Integer pos = getItemPosition(flexible ->
            flexible instanceof DlItemUnknownNode
                    && bleAddress.equals(((DlItemUnknownNode) flexible).item.node.getBleAddress())
        );
        if (pos != null) {
            if (Constants.DEBUG) {
                log.d("onNodeStatusChanged: notifyItemChanged, position = " + pos);
            }
            notifyItemChanged(pos);
        }
    }

    private Integer getItemPosition(Predicate<IFlexible> filter) {
        if (Constants.DEBUG) {
            log.d("getItemPosition: " + "filter = [" + filter + "]");
        }
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            IFlexible item = getItem(i);
            if (filter.test(item)) {
                return i;
            }
        }
        return null;
    }

}
