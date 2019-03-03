/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.IhDiscoveryStateListener;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhNetworkChangeListener;
import com.decawave.argomanager.components.ih.IhNodeDiscoveryListener;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.runner.IhNetworkAssignmentRunnerListener;
import com.decawave.argomanager.runner.NetworkAssignmentRunner;
import com.decawave.argomanager.runner.NetworkAssignmentRunnerImpl;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment;
import com.decawave.argomanager.ui.dialog.NewNetworkNameDialogFragment;
import com.decawave.argomanager.ui.listadapter.discovery.DiscoveryListAdapter;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.NetworkIdGenerator;
import com.decawave.argomanager.util.ToastUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.FlexibleItemDecoration;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;

import static com.decawave.argomanager.util.Util.formatAsHexa;

/**
 * Shows recyclerView of networks and unassigned nodes.
 */
public class DiscoveryFragment extends DiscoveryProgressAwareFragment
        implements  IhNodeDiscoveryListener,
                    IhDiscoveryStateListener,
                    IhPersistedNodeChangeListener,
                    IhNetworkChangeListener,
                    IhNetworkAssignmentRunnerListener,
                    NewNetworkNameDialogFragment.IhCallback,
                    NetworkPickerDialogFragment.IhCallback, ActionMode.Callback {

    public static NetworkAssignmentRunner networkAssignmentRunner = null;

    public static NetworkAssignmentRunner NULL_NETWORK_ASSIGNMENT_RUNNER = new NetworkAssignmentRunner() {

        @Override
        public void startAssignment(List<Long> nodeIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeAssignStatus getNodeAssignStatus(long nodeId) {
            return null;
        }

        @Override
        public @NotNull OverallStatus getOverallStatus() {
            return OverallStatus.NOT_STARTED;
        }

        @Override
        public void terminate() {
            throw new UnsupportedOperationException();
        }

    };

    @Inject
    DiscoveryManager discoveryManager;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    @Inject
    AndroidPermissionHelper permissionHelper;

    @Inject
    BleConnectionApi bleConnectionApi;

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout refreshLayout;

    // state
    private DiscoveryListAdapter adapter;
    private ActionModeHelper mActionModeHelper;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // we could adjust status-bar color here
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_assign) {
            DiscoveryListAdapter.letUserChooseNetwork(networkNodeManager, getMainActivity());
            return true;
        }// else:
        ToastUtil.showToast("unknown action item " + item.getTitle() + "clicked?");
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // we could adjust status-bar color here
    }

    public DiscoveryFragment() {
        super(FragmentType.DISCOVERY);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.node_discovery, container, false);
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.elementList);
        // configure the recycler view overall layout
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // decorate with proper spacing
        recyclerView.addItemDecoration(
                new FlexibleItemDecoration(getActivity())
                .addItemViewType(R.layout.li_discovery_info, 0, 0, 0, 4)
                .addItemViewType(R.layout.li_discovered_network, 0, 4, 0, 4)
                .withSectionGapOffset(16)
                .withEdge(true)
                .addItemViewType(R.layout.li_declared_network, 0, 4, 0, 4)
                .withSectionGapOffset(16)
                .withEdge(true)
                .addItemViewType(R.layout.li_discovered_node, 0, 4, 0, 4)
                .withSectionGapOffset(16)
                .withEdge(true));
                // do not blink on item change - turn off animations
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        // configure the adapter
        Preconditions.checkState(recyclerView.getAdapter() == null);
        adapter = new DiscoveryListAdapter(networkNodeManager, discoveryManager, getMainActivity(), appPreferenceAccessor,
                () -> networkAssignmentRunner != null ? networkAssignmentRunner : NULL_NETWORK_ASSIGNMENT_RUNNER);
        adapter.setDisplayHeadersAtStartUp(true).setStickyHeaders(true);

        recyclerView.setAdapter(adapter);
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE);
        //
        if (savedInstanceState != null) {
            adapter.onRestoreInstanceState(savedInstanceState);
            mActionModeHelper.restoreSelection(getMainActivity());
        }
        //
        ButterKnife.bind(this, v);
        refreshLayout.setOnRefreshListener(() -> {
            if (!discoveryManager.isDiscovering()) {
                permissionHelper.mkSureServicesEnabledAndPermissionsGranted(getMainActivity(), () -> {
                    discoveryManager.startTimeLimitedDiscovery(false);
                    // scroll to the first position
                    recyclerView.scrollToPosition(0);
                });
            }
        });
        // the adapter will be set later (onResume)
        return v;
    }

    void initializeActionModeHelper(@SelectableAdapter.Mode int mode) {
        mActionModeHelper = new ActionModeHelper(adapter, R.menu.discovery_multi_select_menu, this) {

            @Override
            public void updateContextTitle(int count) {
                if (mActionMode != null) {
                    mActionMode.setTitle(daApp.getString(R.string.discovery_multiselect_title, count));
                }
            }

        }.withDefaultMode(mode);
        adapter.addListener((FlexibleAdapter.OnItemLongClickListener) position -> {
            // allow multi-select only when a previous assignment is not running
            if (networkAssignmentRunner == null || networkAssignmentRunner.getOverallStatus().terminal) {
                if (adapter.isItemUnknownNode(position)) {
                    mActionModeHelper.onLongClick(getMainActivity(), position);
                }
            }
        });
        adapter.addListener((FlexibleAdapter.OnItemClickListener) position -> {
            if (adapter.isInMultiSelectMode() && mActionModeHelper != null) {
                if (adapter.isItemUnknownNode(position)) {
                    boolean activate = mActionModeHelper.onClick(position);
                    // last activated position is not available
                    log.d("last activated position: " + position);
                    return activate;
                } else {
                    ToastUtil.showToast("You can select only unassigned nodes!");
                    return false;
                }
            } else {
                adapter.handleRegularOnClick(position);
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // bugfix: the discovery state might have changed in the meantime
        adapter.onDiscoveryStateChanged(discoveryManager.isDiscovering());
        // load the adapter with discovered nodes (only transient ones)
        adapter.setInitialNodeSet();
        // update the list
        adapter.notifyDataSetChanged();
        // update progress
        updateUiRefreshLayoutState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (refreshLayout!=null) {
            refreshLayout.setRefreshing(false);
            refreshLayout.destroyDrawingCache();
            refreshLayout.clearAnimation();
        }
    }

    private void updateUiRefreshLayoutState() {
        if (refreshLayout != null) {
            if (discoveryManager.isDiscovering() || (networkAssignmentRunner != null && !networkAssignmentRunner.getOverallStatus().terminal)) {
                // set visual state
                refreshLayout.setEnabled(false);
                refreshLayout.setRefreshing(false);
            } else {
                // we are not discovering, enable swipe to refresh pattern
                refreshLayout.setEnabled(true);
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // discover listener (keep datasource owned by the adapter in sync with the real recyclerView of items)
    //

    @Override
    public void onNodeDiscovered(@NotNull NetworkNode node) {
        if (Constants.DEBUG) {
            log.d("onNodeDiscovered: node = " + node);
            Preconditions.checkState(!networkNodeManager.isNodePersisted(node.getId()), "node " + node.getId() + " is persisted");
        }
        // we do not know this node yet
        adapter.insertDiscoveredNode(node);
    }

    @Override
    public void onDiscoveredNodeUpdate(@NonNull NetworkNode node) {
        if (Constants.DEBUG) {
            log.d("onDiscoveredNodeUpdate: node = " + node);
            Preconditions.checkState(!networkNodeManager.isNodePersisted(node.getId()), "node " + node.getId() + " is persisted");
        }
        adapter.updateDiscoveredNode(node);
    }

    @Override
    public void onDiscoveredNodeRemoved(long nodeId) {
        if (Constants.DEBUG) {
            log.d("onDiscoveredNodeRemoved: nodeId = " + formatAsHexa(nodeId));
        }
        adapter.removeDiscoveredNode(nodeId);
    }

    ///////////////////////////////////////////////////////////////////////////
    // persisted node change listener
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onNodeUpdatedAndOrAddedToNetwork(short networkId, NetworkNodeEnhanced node) {
        adapter.onPersistentNodeUpdatedAndOrAddedToNetwork(networkId);
    }

    @Override
    public void onNodeUpdatedAndRemovedFromNetwork(short networkId, long nodeId, boolean userInitiated) {
        adapter.onPersistentNodeUpdatedAndRemovedFromNetwork(networkId);
    }

    @Override
    public void onNodeForgotten(long nodeId, Short networkId, boolean userInitiated) {
        if (networkId != null) {
            adapter.onPersistentNodeUpdatedAndRemovedFromNetwork(networkId);
        }
    }

    @Override
    public void onNodeUpdated(NetworkNodeEnhanced node) {
        adapter.onPersistentNodeUpdated(node);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IhDiscoveryStateListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onAfterDiscoveryStarted() {
        adapter.onDiscoveryStateChanged(true);
        updateUiRefreshLayoutState();
    }

    @Override
    protected void onAfterDiscoveryStopped() {
        adapter.onDiscoveryStateChanged(false);
        updateUiRefreshLayoutState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        adapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onNewNetworkName(Short networkId, String networkName) {
        // we will declare the network in each case
        declareNewNetwork(networkId, networkName);
        // what was this?
        if (adapter.isInMultiSelectMode()) {
            // we have selected multiple devices
            launchNetworkAssignment(adapter.getSelectedNetworkNodeIds(), networkId, true);
        } else {
            Long nodeId = adapter.getLastSelectedNetworkNodeId();
            if (nodeId == null) {
                // show the just declared network and it's devices
                getMainActivity().showFragment(FragmentType.OVERVIEW);
            } else {
                // it was a single node
                launchNetworkAssignment(Lists.newArrayList(nodeId), networkId, true);
            }
        }
    }

    private void declareNewNetwork(Short networkId, String networkName) {
        // set the name
        NetworkModel networkModel = new NetworkModel(networkId, networkName);
        // declare the network
        networkNodeManager.declareNetwork(networkModel);
        // make it active
        appPreferenceAccessor.setActiveNetworkId(networkId);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // network picker dialog fragment callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onNetworkPicked(short networkId) {
        if (adapter.isInMultiSelectMode()) {
            //
            launchNetworkAssignment(adapter.getSelectedNetworkNodeIds(), networkId, false);
        } else {
            // user has clicked a single node
            launchNetworkAssignment(Lists.newArrayList(adapter.getLastSelectedNetworkNodeId()), networkId, false);
        }
    }

    public void launchNetworkAssignment(List<Long> nodeIds, short networkId, boolean removeNetworkOnFail) {
        // leave multi-select mode first
        if (adapter.isInMultiSelectMode()) {
            mActionModeHelper.destroyActionModeIfCan();
        }
        // make sure that the runner is either stopped or does not exist at all
        Preconditions.checkState(networkAssignmentRunner == null || networkAssignmentRunner.getOverallStatus().terminal, "network assignment runner state: " + networkAssignmentRunner);
        networkAssignmentRunner = new NetworkAssignmentRunnerImpl(bleConnectionApi, networkNodeManager, networkId, removeNetworkOnFail);
        //
        networkAssignmentRunner.startAssignment(nodeIds);
        // and that's it
        updateUiRefreshLayoutState();
    }

    @Override
    public void onNewNetworkPicked(String networkName) {
        // delegate to other IH method
        onNewNetworkName(NetworkIdGenerator.newNetworkId(), networkName);
    }

    @Override
    public void onNetworkAdded(short networkId) {
        adapter.onNetworkAdded(networkId);
    }

    @Override
    public void onNetworkUpdated(short networkId) {
        // ignore
    }

    @Override
    public void onNetworkRemoved(short networkId, String networkName, boolean explicitUserAction) {
        // ignore (network cannot be removed)
    }

    @Override
    public void onNetworkRenamed(short networkId, String newName) {
        adapter.onNetworkUpdated(networkId);
    }

    @Override
    public void onFloorPlanChanged(short networkId, FloorPlan floorPlan) {
        // ignore (floor plan is not visualized)
    }

    ///////////////////////////////////////////////////////////////////////////
    // IhNetworkAssignmentListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onNetworkAssignmentStatusChanged(NetworkAssignmentRunner.OverallStatus status) {
        updateUiRefreshLayoutState();
    }

    @Override
    public void onNodeStatusChanged(long nodeId) {
        if (Constants.DEBUG)
            log.d("onNodeStatusChanged() called with: " + "nodeId = [" + nodeId + "]");
        // translate to BLE address
        String ble = networkNodeManager.idToBle(nodeId);
        if (ble != null) {
            adapter.onNodeStatusChanged(ble);
        }
    }

}
