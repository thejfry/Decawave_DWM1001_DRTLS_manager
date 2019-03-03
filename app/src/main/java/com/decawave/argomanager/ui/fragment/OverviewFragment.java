/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.IhConnectionStateListener;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.ble.signal.SignalStrength;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreter;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhEnhancedNodePropertiesChangeListener;
import com.decawave.argomanager.components.ih.IhNetworkChangeListener;
import com.decawave.argomanager.components.ih.IhNetworkChangeListenerAdapter;
import com.decawave.argomanager.components.ih.IhNodeDiscoveryListener;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.error.ErrorDetail;
import com.decawave.argomanager.error.IhErrorManagerListener;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreference;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.IhAppPreferenceListener;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.RenameNetworkDialogFragment;
import com.decawave.argomanager.ui.listadapter.NetworkOverviewNodeListAdapter;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.ConnectionUtil;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.decawave.argomanager.util.Util;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;

/**
 * Shows list of network nodes: anchors and tags, distinguishes between visible/invisible ones.
 */
public class OverviewFragment extends MainScreenFragment implements IhAppPreferenceListener,
        IhNodeDiscoveryListener, IhConnectionStateListener, IhErrorManagerListener, IhEnhancedNodePropertiesChangeListener {
    public static final int DISTANCE_TO_TRIGGER_SYNC = 240;
    public static final String BK_ADAPTER_STATE = "ADAPTER_STATE";
    public static final String BK_EXPANDED_NODE = "EXPANDED_NODE";
    public static final int INSTRUCTIONS_SNACKBAR_PERIOD = 120000;
    //
    private static boolean discoveryStarted = false;
    // final members
    @Inject
    DiscoveryManager discoveryManager;
    @Inject
    NetworkNodeManager networkNodeManager;
    @Inject
    SignalStrengthInterpreter signalStrengthInterpreter;
    @Inject
    NetworkNodePropertyDecorator propertyDecorator;
    @Inject
    ErrorManager errorManager;
    @Inject
    AndroidPermissionHelper permissionHelper;
    @Inject
    BlePresenceApi presenceApi;
    @Inject
    BleConnectionApi bleConnectionApi;
    @Inject
    AutoPositioningManager autoPositioningManager;
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;
    @Inject
    LocationDataObserver locationDataObserver;

    // adapter + node list
    private NetworkOverviewNodeListAdapter adapter;
    private short adapterNetworkId;
    private long instructionsSnackBarLastAppearanceSysTime;
    private RecyclerView nodeList;
    private boolean adapterAttachedToNodeList;
    // menu items (to be able do adjust visibility dynamically)
    private MenuItem forgetNetworkMenuItem;
    private MenuItem renameNetworkMenuItem;
    private MenuItem firmwareStatusMenuItem;
    private MenuItem autoPositioningMenuItem;
    private Set<String> ignoredNodeChanges = new HashSet<>();
    //
    private Bundle savedInstanceState;
    private View noNetworkView;
    private SwipeRefreshLayout refreshLayout;
    private IhPresenceApiListener presenceApiListener = new IhPresenceApiListener() {
        @Override
        public void onNodePresent(String bleAddress) {
            if (Constants.DEBUG) {
                log.d("onNodePresent: " + "bleAddress = [" + bleAddress + "]");
            }
            updateNodeCardContent(bleAddress, (v) -> updateSignalStrength(bleAddress, v));
        }

        @Override
        public void onNodeMissing(String bleAddress) {
            if (Constants.DEBUG) {
                log.d("onNodeMissing: " + "bleAddress = [" + bleAddress + "]");
            }
            updateNodeCardContent(bleAddress, (v) -> updateSignalStrength(bleAddress, v));
        }

        @Override
        public void onNodeRssiChanged(String bleAddress, int rssi) {
            if (Constants.DEBUG) {
                log.d("onNodeRssiChanged: " + "bleAddress = [" + bleAddress + "], rssi = [" + rssi + "]");
            }
            updateNodeCardContent(bleAddress, (v) -> updateSignalStrength(bleAddress, v));
        }

        @Override
        public void onTagDirectObserve(String bleAddress, boolean observe) {
            // do nothing
        }

        private void updateSignalStrength(String bleAddress, NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder v) {
            if (Constants.DEBUG) {
                log.d("updateSignalStrength: " + "bleAddress = [" + bleAddress + "], v = [" + v + "]");
            }
            SignalStrength strength = signalStrengthInterpreter.asSignalStrength(presenceApi.getNodeRssi(bleAddress));
            v.updateSignalStrengthAndEditIcon(strength, presenceApi.getNodeStatus(bleAddress));
        }

    };

    private IhNetworkChangeListener networkChangeListener = new IhNetworkChangeListenerAdapter() {

        @Override
        public void onNetworkUpdated(short networkId) {
            if (!networkNodeManager.isActiveNetworkId(networkId)) {
                return;
            }
            // redraw everything
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onNetworkRenamed(short networkId, String newName) {
            if (!networkNodeManager.isActiveNetworkId(networkId)) {
                return;
            }
            // redraw the summary
            adapter.notifyItemChanged(0);
        }

    };

    private IhPersistedNodeChangeListener persistedNodeChangeListener = new IhPersistedNodeChangeListener() {

        @Override
        public void onNodeUpdatedAndOrAddedToNetwork(short networkId, NetworkNodeEnhanced node) {
            if (!networkNodeManager.isInActiveNetwork(node) || ignoredNodeChanges.contains(node.getBleAddress())) {
                return;
            }
            adapter.addNode(node, discoveryManager.anyTransientNodeDiscovered());
        }

        @Override
        public void onNodeUpdatedAndRemovedFromNetwork(short networkId, long nodeId, boolean userInitiated) {
            if (!networkNodeManager.isActiveNetworkId(networkId)) {
                return;
            }
            adapter.removeNode(nodeId);

        }

        @Override
        public void onNodeUpdated(NetworkNodeEnhanced node) {
            onPropertiesChanged(node);
        }

        @Override
        public void onNodeForgotten(long nodeId, Short networkId, boolean userInitiated) {
            if (networkId == null || !networkNodeManager.isActiveNetworkId(networkId)) {
                return;
            }
            adapter.removeNode(nodeId);
        }

    };

    ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int position = viewHolder.getAdapterPosition();
            if (position == 0) {
                // swipe not allowed on summary
                return 0;
            } // else:
            if (position == 1 && adapter.isShowingNewNodesDiscovered()) {
                // swipe not allowed on new nodes discovered
                return 0;
            } // else: allow dismiss
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            throw new IllegalStateException("drag not supported");
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            Long nodeId = adapter.getNodeIdBehindViewHolder(viewHolder);
            if (nodeId != null) {
                String nodeBle = networkNodeManager.idToBle(nodeId);
                // is the node present?
                if (nodeBle != null) {
                    NetworkNodeEnhanced node = networkNodeManager.getNode(nodeId);
                    if (presenceApi.isNodePresent(nodeBle)) {
                        // we will clear the network id of the node
                        clearNodeNetworkId(node);
                    }
                    // and forget the node
                    networkNodeManager.forgetNode(nodeId, true);
                }
            } else {
                throw new IllegalStateException("FIXME: cannot swipe out non-node item!");
            }
        }
    };

    private void clearNodeNetworkId(NetworkNodeEnhanced node) {
        ignoredNodeChanges.add(node.getBleAddress());
        ConnectionUtil.connectAndUpdate(bleConnectionApi, node.getBleAddress(), 2,
                () -> {
                    // build the node to update - set network id to 0
                    return NodeFactory.getBasicCopyBuilder(node.asPlainNode()).setNetworkId((short) 0).build();
                },
                (nnc) -> {},
                (writeEffect, aNode) -> {
                    log.d("successfully cleared network ID");
                    // let the network node manager know that the network ID reset (if any) passed through
                    // the node is forgotten now, we may use the original instance
                    NetworkNodeEnhanced currNode = networkNodeManager.getNode(node.getId());
                    NetworkNode n;
                    if (currNode != null) {
                        // this is a problem - somebody rediscovered and added the node again in the meantime
                        // but we have just reset the network id, we have to use this one
                        n = NodeFactory.newNodeCopy(currNode.asPlainNode());
                    } else {
                        n = node.asPlainNode();
                    }
                    n.setNetworkId(null);
                    // de-ignore node changes
                    ignoredNodeChanges.remove(node.getBleAddress());
                    // notify with a complete node
                    networkNodeManager.onNodeIntercepted(n);
                },
                (fail) -> {
                    // on fail
                    log.w("failed to clear network ID: " + fail.message);
                },
                // cleanup
                (errCode) -> {
                    // de-ignore node changes (we have done this onSucess, but we need to make sure, that this gets called always)
                    ignoredNodeChanges.remove(node.getBleAddress());
                }
        );
    }


    public OverviewFragment() {
        super(FragmentType.OVERVIEW);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_overview, menu);
        configureBasicMenuItems(menu);
        configureInstructionsMenuItem(menu);
        // now configure our specific menu items
        renameNetworkMenuItem = menu.findItem(R.id.action_rename);
        renameNetworkMenuItem.setOnMenuItemClickListener((v) -> {
            //noinspection ConstantConditions
            RenameNetworkDialogFragment.showDialog(getMainActivity().getSupportFragmentManager(),
                    networkNodeManager.getActiveNetwork().getNetworkName());
            return true;
        });
        forgetNetworkMenuItem = menu.findItem(R.id.action_forget);
        forgetNetworkMenuItem.setOnMenuItemClickListener((v) -> {
            networkNodeManager.removeActiveNetwork();
            if (networkNodeManager.getActiveNetwork() == null) {
                getMainActivity().hideAbSpinnerNoNetwork();
                instructionsItem.setVisible(false);
            }
            return true;
        });
        firmwareStatusMenuItem = menu.findItem(R.id.action_firmware_status);
        firmwareStatusMenuItem.setOnMenuItemClickListener((v) -> {
            //
            getMainActivity().showFragment(FragmentType.FIRMWARE_UPDATE);
            return true;
        });
        autoPositioningMenuItem = menu.findItem(R.id.action_autopositioning);
        autoPositioningMenuItem.setOnMenuItemClickListener((v) -> {
            // make sure that discovery is stopped for sure
            if (discoveryManager.isDiscovering() && !discoveryManager.isStopping()) {
                discoveryManager.stopDiscovery();
            }
            autoPositioningManager.resetNodeSet(Stream.of(networkNodeManager.getActiveNetworkNodes())
                    .map(NetworkNodeEnhanced::asPlainNode)
                    // set up only anchors which do distance measurement, have a valid seat number and are present
                    .filter((n) -> n.isAnchor() && validSeatNumber(((AnchorNode) n).getSeatNumber()) && n.getUwbMode() == UwbMode.ACTIVE && presenceApi.isNodePresent(n.getBleAddress()))
                    // cast to anchor
                    .map((n) -> (AnchorNode) n)
                    .collect(Collectors.toList()));
            // show the auto-positioning fragment
            getMainActivity().showFragment(FragmentType.AUTO_POSITIONING);
            return true;
        });
        setMenuItemsVisibility();
    }

    private static boolean validSeatNumber(Byte seatNumber) {
        return seatNumber != null && seatNumber != -1;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.overview, container, false);
        nodeList = (RecyclerView) v.findViewById(R.id.nodeList);
        ((SimpleItemAnimator) nodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        noNetworkView = v.findViewById(R.id.noNetwork);
        refreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setDistanceToTriggerSync(DISTANCE_TO_TRIGGER_SYNC);
        refreshLayout.setOnRefreshListener(() -> {
            if (!discoveryApi.isDiscovering()) {
                permissionHelper.mkSureServicesEnabledAndPermissionsGranted(getMainActivity(), () -> discoveryManager.startTimeLimitedDiscovery(false));
            }
        });
        //
        Util.configureNoNetworkScreen(noNetworkView, permissionHelper, getMainActivity());
        // swipe to dismiss
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(nodeList);
        this.savedInstanceState = savedInstanceState;
        // configure the recycler view overall layout
        nodeList.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapterAttachedToNodeList = false;
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        InterfaceHub.registerHandler(persistedNodeChangeListener);
        InterfaceHub.registerHandler(networkChangeListener);
        InterfaceHub.registerHandler(presenceApiListener);
        if (networkNodeManager.getActiveNetwork() != null && permissionHelper.allSetUp() && !discoveryStarted) {
            // start / prolong discovery
            discoveryManager.startTimeLimitedDiscovery(true);
            discoveryStarted = true;
        } // else: no active network, let the user start the discovery
        updateUi();
    }


    @Override
    public void onPause() {
        super.onPause();
        InterfaceHub.unregisterHandler(persistedNodeChangeListener);
        InterfaceHub.unregisterHandler(networkChangeListener);
        InterfaceHub.unregisterHandler(presenceApiListener);
        // clean-up refresh layout
        if (refreshLayout!=null) {
            refreshLayout.setRefreshing(false);
            refreshLayout.destroyDrawingCache();
            refreshLayout.clearAnimation();
        }
    }

    private void updateUi() {
        NetworkModel network = networkNodeManager.getActiveNetwork();
        if (network == null) {
            // there is no selected active network
            noNetworkView.setVisibility(View.VISIBLE);
            nodeList.setVisibility(View.GONE);
            // reset the adapter
            adapter = null;
        } else {
            short networkId = network.getNetworkId();
            // selected active network
            noNetworkView.setVisibility(View.GONE);
            nodeList.setVisibility(View.VISIBLE);
            if (adapter == null) {
                Bundle b = null;
                Long expandedNodeId = null;
                Bundle args = getArguments();
                if (savedInstanceState != null) {
                    // we give preference to saved instance state
                    b = savedInstanceState.getBundle(BK_ADAPTER_STATE);
                } else if (args != null && args.containsKey(BK_EXPANDED_NODE)) {
                    expandedNodeId = args.getLong(BK_EXPANDED_NODE);
                }
                setupAdapter(b, expandedNodeId);
            } else if (adapterNetworkId != networkId || !adapterAttachedToNodeList) {
                setupAdapter(null, null);
            } else {
                // workaround {begin} for android bug https://stackoverflow.com/questions/30220771/recyclerview-inconsistency-detected-invalid-item-position
                adapter.notifyDataSetChanged();
                adapter.setShowNewNodesDiscovered(discoveryManager.anyTransientNodeDiscovered());
                // workaround {end}
            }
            if (!appPreferenceAccessor.isInstructionsRead()
                    && instructionsSnackBarLastAppearanceSysTime + INSTRUCTIONS_SNACKBAR_PERIOD < SystemClock.uptimeMillis()) {
                instructionsSnackBarLastAppearanceSysTime = SystemClock.uptimeMillis();
                FragmentManager fm = getMainActivity().getSupportFragmentManager();
                getMainActivity().makeSnackbarWithHelpIcon(daApp.getString(R.string.snackbar_read_instructions), 5000)
                        .setAction(R.string.snackbar_action_read, v -> {
                            // show instructions screen
                            MainActivity.showFragment(FragmentType.INSTRUCTIONS, fm, null);
                        }).show();
            }
        }
        updateUiRefreshLayoutState();
        setMenuItemsVisibility();
    }

    private void attachAdapterToNodeList() {
        nodeList.setAdapter(adapter);
        adapterAttachedToNodeList = true;
    }

    private void setupAdapter(Bundle savedState, Long expandedNodeId) {
        adapter = new NetworkOverviewNodeListAdapter(
                networkNodeManager.getActiveNetworkNodes(),
                permissionHelper, networkNodeManager,
                bleConnectionApi, locationDataObserver, appPreferenceAccessor, discoveryManager.anyTransientNodeDiscovered(),
                appPreferenceAccessor::getApplicationMode,
                getMainActivity(),
                signalStrengthInterpreter, presenceApi, propertyDecorator);
        adapterNetworkId = appPreferenceAccessor.getActiveNetworkId();
        attachAdapterToNodeList();
        if (savedState != null) {
            adapter.restoreState(savedState);
        } else if (expandedNodeId != null) {
            adapter.setExpandedNodeId(expandedNodeId);
            String nodeBle = networkNodeManager.idToBle(expandedNodeId);
            if (nodeBle != null) {
                Integer position = adapter.getNodePosition(nodeBle);
                if (position != null) {
                    nodeList.getLayoutManager().scrollToPosition(position);
                }
            }
        }
    }

    private void updateUiRefreshLayoutState() {
        if (refreshLayout != null) {
            if(discoveryApi.isDiscovering() || networkNodeManager.getActiveNetwork() == null) {
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
    // network change listener (keep datasource owned by the adapter in sync with the real list of items)
    //

    @Override
    public void onPreferenceChanged(AppPreference.Element element, Object oldValue, Object newValue) {
        if (element == AppPreference.Element.ACTIVE_NETWORK_ID) {
            // switch the active network
            updateUi();
        }
    }

    @Override
    public void onAfterDiscoveryStarted() {
        updateUiRefreshLayoutState();
    }

    @Override
    public void onAfterDiscoveryStopped() {
        updateUiRefreshLayoutState();
        if (adapter != null) {
            // make signal strength indicators obsolete
            adapter.makeSignalIndicatorsObsolete();
        }
    }

    /**
     * Universal routine to update network node card content if it is shown.
     * If the associated card is not shown, notifyItemChanged is called instead.
     * @param bleAddress identifies the node
     * @param updateViewConsumer generic action to be performed on the view
     */
    private void updateNodeCardContent(String bleAddress, Consumer<NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder> updateViewConsumer) {
        if (Constants.DEBUG) {
            log.d("updateNodeCardContent: " + "bleAddress = [" + bleAddress + "]");
        }
        // does it make sense to notify the adapter (is the item showing?)
        Integer position = adapter == null ? null : adapter.getNodePosition(bleAddress);
        if (position != null) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) nodeList.getLayoutManager();
            if (position >= layoutManager.findFirstVisibleItemPosition() && position <= layoutManager.findLastVisibleItemPosition()) {
                // let the consumer modify the view/viewholder
                View view = nodeList.getLayoutManager().findViewByPosition(position);
                if (bleAddress.equals(view.getTag())) {
                    NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder viewHolder = (NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder) nodeList.getChildViewHolder(view);
                    updateViewConsumer.accept(viewHolder);
                    return;
                }
            }
            // just notify the adapter (if it has a cached view somewhere invisibly)
            adapter.notifyItemChanged(position);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // discovery callbacks
    //
    // actually - we are not considered in discovered transient nodes
    // we show only persisted nodes - for which we get notified in IhPersistedNodeChangeListener
    //
    // we are registered here in order to get notifications about necessity of showing
    // the 'new nodes discovered...' card
    //
    @Override
    public void onNodeDiscovered(@NonNull NetworkNode node) {
        notifyAdapterPossibleTransientNodes();
    }

    @Override
    public void onDiscoveredNodeUpdate(@NonNull NetworkNode node) {
        // this might be the case that the node gets updated in such a way that it leaves/joins a persisted network
        // therefore we might need to show 'new nodes discovered...' card
        notifyAdapterPossibleTransientNodes();
    }

    @Override
    public void onDiscoveredNodeRemoved(long nodeId) {
        notifyAdapterPossibleTransientNodes();
    }

    private void notifyAdapterPossibleTransientNodes() {
        if (adapter != null) {
            adapter.setShowNewNodesDiscovered(discoveryManager.anyTransientNodeDiscovered());
        }
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (adapter != null) {
            outState.putBundle(BK_ADAPTER_STATE, adapter.saveState());
        }
    }

    @Override
    public void onErrorDetailAdded(@NonNull String deviceBleAddress, @NonNull ErrorDetail errorDetail) {
        setMenuItemsVisibility();
    }

    private void setMenuItemsVisibility() {
        if (showErrorsMenuItem != null) {
            showErrorsMenuItem.setVisible(errorManager.anyUnreadError());
            boolean anyActiveNetwork = networkNodeManager.getActiveNetwork() != null;
            if (!anyActiveNetwork) {
                renameNetworkMenuItem.setVisible(false);
                forgetNetworkMenuItem.setVisible(false);
                firmwareStatusMenuItem.setVisible(false);
                autoPositioningMenuItem.setVisible(false);
            } else {
                renameNetworkMenuItem.setVisible(true);
                forgetNetworkMenuItem.setVisible(true);
                firmwareStatusMenuItem.setVisible(true);
                autoPositioningMenuItem.setVisible(true);
                boolean nodeOperationEnabled;
                nodeOperationEnabled = !networkNodeManager.getActiveNetworkNodes().isEmpty();
                firmwareStatusMenuItem.setEnabled(nodeOperationEnabled);
                autoPositioningMenuItem.setEnabled(nodeOperationEnabled);
            }
        }
    }

    @Override
    public void onErrorRemoved(@NonNull String deviceBleAddress) {
        // do nothing
    }

    @Override
    public void onErrorsClear() {
        // do nothing
    }


    @Override
    public void onConnected(String bleAddress) {
        // do nothing
    }

    @Override
    public void onConnecting(String bleAddress) {
        NetworkNodeEnhanced node = networkNodeManager.getNode(bleAddress);
        if (ignoredNodeChanges.contains(bleAddress) || node == null || !networkNodeManager.isInActiveNetwork(node)) {
            return;
        }
        updateNodeCardContent(bleAddress, NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder::updateNodeState);
    }

    @Override
    public void onDisconnected(String bleAddress, Boolean sessionSuccess) {
        NetworkNodeEnhanced node = networkNodeManager.getNode(bleAddress);
        if (ignoredNodeChanges.contains(bleAddress) || node == null || !networkNodeManager.isInActiveNetwork(node)) {
            return;
        }
        updateNodeCardContent(bleAddress, NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder::updateNodeState);
    }

    @Override
    public void onDisconnecting(String address) {
        // do nothing
    }

    public static Bundle getBundleForExpandedNode(Long id) {
        Bundle b = new Bundle();
        b.putLong(BK_EXPANDED_NODE, id);
        return b;
    }

    @Override
    public void onPropertiesChanged(NetworkNodeEnhanced node) {
        if (Constants.DEBUG) {
            log.d("onPropertiesChanged: " + "node = [" + node + "]");
        }
        if (!networkNodeManager.isInActiveNetwork(node) || ignoredNodeChanges.contains(node.getBleAddress())) {
            return;
        }
        // make sure that the adapter has the most up-to-date representation
        adapter.updateNodeInCollection(node);
        // modify the visual representation directly if it is visible
        updateNodeCardContent(node.getBleAddress(), (vh) -> vh.bind(node, true));
    }

}
