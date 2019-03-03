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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.firmware.Firmware;
import com.decawave.argomanager.firmware.FirmwareRepository;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.runner.FirmwareUpdateRunner;
import com.decawave.argomanager.runner.FirmwareUpdateRunnerImpl;
import com.decawave.argomanager.runner.IhFwUpdateRunnerListener;
import com.decawave.argomanager.ui.listadapter.FirmwareUpdateNodeListAdapter;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.kryl.android.common.hub.InterfaceHub;

/**
 * Shows list of network nodes: anchors and tags, allows selection of particular nodes.
 */
public class FirmwareUpdateFragment extends DiscoveryProgressAwareFragment {
    public static final int DISTANCE_TO_TRIGGER_SYNC = 240;
    public static final String BK_ADAPTER_STATE = "ADAPTER_STATE";
    //
    public static FirmwareUpdateRunner firmwareUpdateRunner = null;
    // VOID runner (null design pattern)
    private final FirmwareUpdateRunner NULL_FIRMWARE_UPDATE_RUNNER = new FirmwareUpdateRunner() {

        @Override
        public void startFwUpdate(List<NetworkNode> nodes) {
            throw new UnsupportedOperationException("this is VOID FW update runner");
        }

        @Override
        public NodeUpdateStatus getNodeUpdateStatus(long nodeId) {
            return null;
        }

        @Override
        public OverallStatus getOverallStatus() {
            return FirmwareUpdateRunner.OverallStatus.NOT_STARTED;
        }

        @Override
        public void terminate() {
            throw new UnsupportedOperationException("this is VOID FW update runner");
        }

        @Override
        public Integer getUploadByteCounter(long nodeId) {
            return null;
        }

        @Override
        public Map<Long, NodeUpdateStatus> getNodeStatuses() {
            return null;
        }
    };

    // dependencies
    @Inject
    DiscoveryManager discoveryManager;
    @Inject
    NetworkNodeManager networkNodeManager;
    @Inject
    AndroidPermissionHelper permissionHelper;
    @Inject
    BlePresenceApi presenceApi;
    @Inject
    NetworkNodePropertyDecorator propertyDecorator;
    @Inject
    BleConnectionApi bleConnectionApi;

    // view references
    @BindView(R.id.updateButton)
    Button btnUpdate;
    @BindView(R.id.nodeList)
    RecyclerView nodeList;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.tvNoNodes)
    View noNodesView;

    // adapter + node list
    private FirmwareUpdateNodeListAdapter adapter;
    private Bundle savedAdapterState;

    private IhPresenceApiListener presenceApiListener = new IhPresenceApiListener() {
        @Override
        public void onNodePresent(String bleAddress) {
            if (networkNodeManager.activeNetworkContainsNode(bleAddress)) {
                setUpdateButtonState();
                adapter.onNodePresenceChanged(bleAddress, true);
            }
        }

        @Override
        public void onNodeMissing(String bleAddress) {
            if (networkNodeManager.activeNetworkContainsNode(bleAddress)) {
                setUpdateButtonState();
                adapter.onNodePresenceChanged(bleAddress, false);
            }
        }

        @Override
        public void onNodeRssiChanged(String bleAddress, int rssi) {
            // do nothing
        }

        @Override
        public void onTagDirectObserve(String bleAddress, boolean observe) {
            // do nothing
        }
    };

    private IhPersistedNodeChangeListener nodeChangeListener = new IhPersistedNodeChangeListener() {

        @Override
        public void onNodeUpdatedAndOrAddedToNetwork(short networkId, NetworkNodeEnhanced node) {
            if (!networkNodeManager.isInActiveNetwork(node) || fwUpdateStarted()) {
                return;
            }
            adapter.addNode(node);
        }

        private boolean fwUpdateStarted() {
            return firmwareUpdateRunner != null && firmwareUpdateRunner.getOverallStatus() != FirmwareUpdateRunner.OverallStatus.NOT_STARTED;
        }

        @Override
        public void onNodeUpdatedAndRemovedFromNetwork(short networkId, long nodeId, boolean userInitiated) {
            if (!networkNodeManager.isActiveNetworkId(networkId) || fwUpdateStarted()) {
                return;
            }
            adapter.removeNode(nodeId);
        }

        @Override
        public void onNodeUpdated(NetworkNodeEnhanced node) {
            if (!networkNodeManager.isInActiveNetwork(node)) {
                return;
            }
            // make sure that the adapter has the most up-to-date representation
            adapter.updateNode(node);
        }

        @Override
        public void onNodeForgotten(long nodeId, Short networkId, boolean userInitiated) {
            if (networkId == null || !networkNodeManager.isActiveNetworkId(networkId) || fwUpdateStarted()) {
                return;
            }
            adapter.removeNode(nodeId);
        }

    };

    private IhFwUpdateRunnerListener fwUpdateRunnerListener = new IhFwUpdateRunnerListener() {

        @Override
        public void onNodeStatusChanged(long nodeId) {
            if (Constants.DEBUG)
                log.d("onNodeStatusChanged() called with: " + "nodeId = [" + nodeId + "]");
            // translate to BLE address
            String ble = networkNodeManager.idToBle(nodeId);
            if (ble != null) {
                adapter.onFwUpdateNodeStatusChanged(ble);
            }
        }

        @Override
        public void onNodeUploadProgressChanged(long nodeId, int uploadByteCount) {
            String ble = networkNodeManager.idToBle(nodeId);
            if (ble != null) {
                adapter.onUploadProgressChanged(ble, uploadByteCount);
            }
        }

        @Override
        public void onFwUpdateStatusChanged(FirmwareUpdateRunner.OverallStatus status) {
            updateUi();
            adapter.onFwUpdateOverallStatusChanged(status);
        }
    };

    public FirmwareUpdateFragment() {
        super(FragmentType.FIRMWARE_UPDATE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remember the state
        if (savedInstanceState != null) {
            savedAdapterState = savedInstanceState.getBundle(BK_ADAPTER_STATE);
        } else if (firmwareUpdateRunner != null) {
            // fake saved state
            savedAdapterState = FirmwareUpdateNodeListAdapter.getState(firmwareUpdateRunner.getNodeStatuses().keySet());
        } // else: keep the saved Adapter state null - the logic will preselect appropriate nodes
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.firmware_update, container, false);
        // extract view references
        ButterKnife.bind(this, v);
        // set listeners
        ((SimpleItemAnimator) nodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        refreshLayout.setDistanceToTriggerSync(DISTANCE_TO_TRIGGER_SYNC);
        refreshLayout.setOnRefreshListener(() -> {
            if (!discoveryApi.isDiscovering()) {
                permissionHelper.mkSureServicesEnabledAndPermissionsGranted(getMainActivity(), () -> discoveryManager.startTimeLimitedDiscovery(false));
            }
        });
        // configure the recycler view overall layout
        nodeList.setLayoutManager(new LinearLayoutManager(getActivity()));
        NetworkModel activeNetwork = networkNodeManager.getActiveNetwork();
        if (activeNetwork != null) {
            setupAdapter(activeNetwork, savedAdapterState);
        }
        return v;
    }

    @OnClick(R.id.updateButton)
    void onUpdateButtonClicked() {
        if (firmwareUpdateRunner != null) {
            FirmwareUpdateRunner.OverallStatus fwUpdateStatus = firmwareUpdateRunner.getOverallStatus();
            if (fwUpdateStatus == FirmwareUpdateRunner.OverallStatus.UPDATING) {
                firmwareUpdateRunner.terminate();
            } else if (fwUpdateStatus.terminal) {
                // firmware update has stopped/is cancelled, forget the runner
                firmwareUpdateRunner = null;
                NetworkModel network = networkNodeManager.getActiveNetwork();
                // recreate the adapter with no node selected
                setupAdapter(network, FirmwareUpdateNodeListAdapter.getState(Collections.emptySet()));
                updateUi();
            } else {
                throw new IllegalStateException("status = " + fwUpdateStatus);
            }
        } else {
            List<NetworkNode> nodes = adapter.getCheckedNodesInOrder();
            // we need to consider only those nodes which are present
            nodes = Stream.of(nodes).filter((nn) -> presenceApi.isNodePresent(nn.getBleAddress())).collect(Collectors.toList());
            if (Constants.DEBUG) {
                FirmwareUpdateRunner.OverallStatus overallStatus = firmwareUpdateRunner == null ? null : firmwareUpdateRunner.getOverallStatus();
                Preconditions.checkState(overallStatus == null || overallStatus.terminal, "overall FW-UP status: " + overallStatus);
            }
            // start the update
            firmwareUpdateRunner = new FirmwareUpdateRunnerImpl(this.bleConnectionApi,
                    FirmwareRepository.DEFAULT_FIRMWARE[0],
                    FirmwareRepository.DEFAULT_FIRMWARE[1]);
            firmwareUpdateRunner.startFwUpdate(nodes);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        InterfaceHub.registerHandler(nodeChangeListener);
        InterfaceHub.registerHandler(presenceApiListener);
        InterfaceHub.registerHandler(fwUpdateRunnerListener);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUi();
    }


    @Override
    public void onPause() {
        super.onPause();
        InterfaceHub.unregisterHandler(nodeChangeListener);
        InterfaceHub.unregisterHandler(presenceApiListener);
        InterfaceHub.unregisterHandler(fwUpdateRunnerListener);
        // clean-up refresh layout
        if (refreshLayout!=null) {
            refreshLayout.setRefreshing(false);
            refreshLayout.destroyDrawingCache();
            refreshLayout.clearAnimation();
        }
    }

    private void updateUi() {
        NetworkModel network = networkNodeManager.getActiveNetwork();
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(network);
        }
        // selected active network
        noNodesView.setVisibility(View.GONE);
        nodeList.setVisibility(View.VISIBLE);
        updateUiRefreshLayoutState();
        setUpdateButtonState();
    }

    private void setupAdapter(NetworkModel network, Bundle savedAdapterState) {
        Firmware firmware1 = FirmwareRepository.DEFAULT_FIRMWARE[0];
        Firmware firmware2 = FirmwareRepository.DEFAULT_FIRMWARE[1];
        //noinspection Convert2MethodRef
        Collection<NetworkNodeEnhanced> nodes = networkNodeManager.getNetworkNodes(network.getNetworkId());
        boolean fwUpdateStarted = firmwareUpdateRunner != null && firmwareUpdateRunner.getOverallStatus() != FirmwareUpdateRunner.OverallStatus.NOT_STARTED;
        if (fwUpdateStarted) {
            // we will provide only those nodes which were selected/are being acted upon
            nodes = Stream.of(nodes)
                    .filter((n) -> firmwareUpdateRunner.getNodeStatuses().containsKey(n.getId()))
                    .collect(Collectors.toList());
        }
        adapter = new FirmwareUpdateNodeListAdapter(
                nodes,
                firmware1.getMeta(),
                firmware2.getMeta(),
                propertyDecorator,
                getMainActivity(),
                presenceApi,
                () -> firmwareUpdateRunner == null ? NULL_FIRMWARE_UPDATE_RUNNER : firmwareUpdateRunner,
                (checkedChangedNodeId) -> setUpdateButtonState());
        nodeList.setAdapter(adapter);
        if (savedAdapterState != null) {
            adapter.restoreState(savedAdapterState);
        } else if (!fwUpdateStarted) {
            // we will pre-select those entries which have a different firmware version/checksum AND are present
            adapter.setCheckedNodeIds(Stream.of(nodes)
                    .filter((n) -> presenceApi.isNodePresent(n.getBleAddress())
                            && nodeNeedsNewFirmware(n, firmware1.getMeta(), firmware2.getMeta()))
                    .map(NetworkNodeEnhanced::getId)
                    .collect(Collectors.toSet()));
        }
    }

    private void setUpdateButtonState() {
        Boolean enabled = null;
        if (firmwareUpdateRunner != null) {
            if (firmwareUpdateRunner.getOverallStatus().terminal) {
                // set 'okay' label
                btnUpdate.setText(R.string.btn_okay);
                enabled = true;
            } else {
                // firmware update is just running
                btnUpdate.setText(R.string.btn_cancel);
                enabled = true;
            }
        } else {
            // firmware update is stopped
            btnUpdate.setText(R.string.btn_update);
            // firmware update is not running
            Set<Long> checkedNodeIds = adapter.getCheckedNodeIds();
            // check if at least one of the checked nodes is present
            for (Long nodeId : checkedNodeIds) {
                String bleAddress = networkNodeManager.idToBle(nodeId);
                if (bleAddress != null && presenceApi.isNodePresent(bleAddress)) {
                    enabled = true;
                    break;
                }
            }
            if (enabled == null) {
                // none of the checked nodes is present
                enabled = false;
            }
        }
        // final correction: check if there is discovery running
        if (discoveryManager.isDiscovering()) {
            enabled = false;
        }
        btnUpdate.setEnabled(enabled);
    }

    private boolean nodeNeedsNewFirmware(NetworkNodeEnhanced networkNode, FirmwareMeta firmware1Meta, FirmwareMeta firmware2Meta) {
        NetworkNode nn = networkNode.asPlainNode();
        return FirmwareUpdateRunnerImpl.needsNewFirmware(firmware1Meta, nn.getFw1Version(), nn.getFw1Checksum())
                ||
                FirmwareUpdateRunnerImpl.needsNewFirmware(firmware2Meta, nn.getFw2Version(), nn.getFw2Checksum());
    }

    private void updateUiRefreshLayoutState() {
        if (refreshLayout != null) {
            if (discoveryApi.isDiscovering() || (firmwareUpdateRunner != null && !firmwareUpdateRunner.getOverallStatus().terminal)) {
                // disable swipe to refresh
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
    public void onAfterDiscoveryStarted() {
        updateUiRefreshLayoutState();
        setUpdateButtonState();
    }

    @Override
    public void onAfterDiscoveryStopped() {
        updateUiRefreshLayoutState();
        setUpdateButtonState();
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            outState.putBundle(BK_ADAPTER_STATE, adapter.saveState());
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

}
