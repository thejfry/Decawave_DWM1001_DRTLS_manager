/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.IhConnectionStateListener;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.AutoPositioningState;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.impl.AutoPositioningAlgorithm;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.runner.IhAutoPositioningManagerListener;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.ZaxisValueDialogFragment;
import com.decawave.argomanager.ui.listadapter.AutoPositioningNodeListAdapter;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;
import com.emtronics.dragsortrecycler.DragSortRecycler;
import com.google.common.base.Preconditions;

import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.kryl.android.common.hub.InterfaceHub;

/**
 * Shows list of network nodes: anchors and tags, allows selection of particular nodes.
 */
public class AutoPositioningFragment extends DiscoveryProgressAwareFragment
        implements DragSortRecycler.OnItemMovedListener, ZaxisValueDialogFragment.IhCallback {
    // dependencies
    @Inject
    AutoPositioningManager autoPositioningManager;
    @Inject
    NetworkNodeManager networkNodeManager;
    @Inject
    AndroidPermissionHelper permissionHelper;
    @Inject
    BleConnectionApi bleConnectionApi;
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    // view references
    @BindView(R.id.footerButtonBar)
    View footerButtonBar;
    @BindView(R.id.measureButton)
    Button measureBtn;
    @BindView(R.id.saveButton)
    Button saveBtn;
    @BindView(R.id.nodeList)
    RecyclerView nodeList;
    @BindView(R.id.contentView)
    View contentView;
    @BindView(R.id.tvNoNodes)
    View noNodesView;

    // adapter + node list
    private AutoPositioningNodeListAdapter adapter;
    private boolean snackbarShown;

    private IhConnectionStateListener connectionStateListener = new IhConnectionStateListener() {

        @Override
        public void onConnecting(String bleAddress) {
            // this might mean a change
            updateUi();
        }

        @Override
        public void onConnected(String bleAddress) {
            // do nothing
        }

        @Override
        public void onDisconnecting(String bleAddress) {
            // do nothing
        }

        @Override
        public void onDisconnected(String bleAddress, Boolean sessionSuccess) {
            // this might potentially lead to enabled swipe-to-refresh
            updateUi();
        }

    };

    private IhAutoPositioningManagerListener autoPositioningManagerListener = new IhAutoPositioningManagerListener() {

        @Override
        public void onNodeStateChanged(long nodeId) {
            if (Constants.DEBUG) {
                log.d("onNodeStateChanged: " + "nodeId = [" + Util.shortenNodeId(nodeId, false) + "]");
            }
            adapter.notifyItemChanged(networkNodeManager.idToBle(nodeId));
        }

        @Override
        public void onApplicationStateChanged(AutoPositioningState.ApplicationState state, AutoPositioningAlgorithm.ResultCode computationResult) {
            if (Constants.DEBUG) {
                log.d("onApplicationStateChanged: " + "state = [" + state + "], computationResult = [" + computationResult + "]");
            }
            doCompleteUpdate();
        }

        @Override
        public void onNodeSetChange(AutoPositioningAlgorithm.ResultCode code) {
            if (Constants.DEBUG) {
                log.d("onNodeSetChange: " + "code = [" + code + "]");
            }
            doCompleteUpdate();
        }

        private void doCompleteUpdate() {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            updateUi();
        }

    };
    private DragSortRecycler dragSortRecycler;

    public AutoPositioningFragment() {
        super(FragmentType.AUTO_POSITIONING);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_autopos, menu);
        configureBasicMenuItems(menu);
        // autopositioning instructions are special because they are always present
        MenuItem instructionsItem = menu.findItem(R.id.action_instructions);
        FragmentManager fm = getMainActivity().getSupportFragmentManager();
        instructionsItem.setOnMenuItemClickListener(item -> {
            showApInstructions(fm);
            return true;
        });

    }

    public static void showApInstructions(FragmentManager fragmentManager) {
        MainActivity.showFragment(FragmentType.INSTRUCTIONS, fragmentManager, InstructionsFragment.getBundleForAnchor("autopositioning"));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.auto_positioning, container, false);
        // extract view references
        ButterKnife.bind(this, v);
        // TODO: is this visually OK?
        //((SimpleItemAnimator) nodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        // drag-n-drop support
        dragSortRecycler = new DragSortRecycler() {
            @Override
            protected boolean canDragOver(int position) {
                return position > 0;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                if (autoPositioningManager.getApplicationState().idle) {
                    // propagate
                    super.onTouchEvent(rv, e);
                }
            }
        };
        dragSortRecycler.setViewHandleId(R.id.btnDragHandle);
        dragSortRecycler.setOnItemMovedListener(this);
        dragSortRecycler.setFloatingAlpha(1f);
        //
        nodeList.addItemDecoration(dragSortRecycler);
        nodeList.addOnItemTouchListener(dragSortRecycler);
        nodeList.addOnScrollListener(dragSortRecycler.getScrollListener());
        // configure the recycler view overall layout
        nodeList.setLayoutManager(new LinearLayoutManager(getActivity()));
        setupAdapter();
        return v;
    }

    private boolean autoPositioningSuccessDistanceCollectionFail() {
        NetworkNode initiator = null;
        for (NetworkNode nn : autoPositioningManager.getNodes()) {
            if (Util.isRealInitiator(nn)) {
                initiator = nn;
                break;
            }
        }
        return initiator != null &&
                autoPositioningManager.getNodeDistanceCollectionStatus(initiator.getId()) == AutoPositioningState.TaskState.SUCCESS &&
                autoPositioningManager.getApplicationState() == AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_FAILED;
    }

    @OnClick(R.id.measureButton)
    void onMeasureButtonClicked() {
        // this button changes state from: measure to cancel
        AutoPositioningState.ApplicationState applicationState = autoPositioningManager.getApplicationState();
        if (applicationState == AutoPositioningState.ApplicationState.COLLECTING_DISTANCES || applicationState == AutoPositioningState.ApplicationState.CHECKING_INITIATOR) {
            // cancel
            autoPositioningManager.terminate();
        } else if (applicationState.idle) {
            // the application state is idle
            permissionHelper.mkSureServicesEnabledAndPermissionsGranted(getMainActivity(), () -> {
                if (autoPositioningSuccessDistanceCollectionFail()) {
                    autoPositioningManager.retrieveDistancesFromFailingNodesAndComputePositions();
                } else if (!autoPositioningManager.measure()) {
                    ToastUtil.showToast(R.string.ap_initiator_not_found_ap_cannot_be_started);
                }
            });
        } else {
            throw new IllegalStateException("what should we do when clicked on measure in " + applicationState);
        }
    }

    @OnClick(R.id.saveButton)
    void onSaveButtonClicked() {
        AutoPositioningState.ApplicationState appState = autoPositioningManager.getApplicationState();
        if (appState == AutoPositioningState.ApplicationState.SAVING_POSITIONS) {
            // terminate
            autoPositioningManager.terminate();
        } else if (appState.idle) {
            autoPositioningManager.savePositions();
        } else {
            throw new IllegalStateException("what should we do when clicked on save in " + appState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        InterfaceHub.registerHandler(autoPositioningManagerListener);
        InterfaceHub.registerHandler(connectionStateListener);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUi();
    }


    @Override
    public void onPause() {
        super.onPause();
        InterfaceHub.unregisterHandler(autoPositioningManagerListener);
        InterfaceHub.unregisterHandler(connectionStateListener);
    }

    private void updateUi() {
        NetworkModel network = networkNodeManager.getActiveNetwork();
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(network);
        }
        // selected active network
        if (autoPositioningManager.getNodes().isEmpty()) {
            noNodesView.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
        } else {
            noNodesView.setVisibility(View.GONE);
            nodeList.setVisibility(View.VISIBLE);
            setActionButtonState();
        }
        uiSetMenuItemsVisibility();
    }

    private void setupAdapter() {
        //noinspection Convert2MethodRef
        adapter = new AutoPositioningNodeListAdapter(
                getMainActivity(),
                autoPositioningManager,
                appPreferenceAccessor
        );
        nodeList.setAdapter(adapter);
    }

    private void setActionButtonState() {
        boolean allDisconnected = bleConnectionApi.allConnectionsClosed();
        switch (autoPositioningManager.getApplicationState()) {
            case CHECKING_INITIATOR:
                setButtonState(true, R.string.btn_cancel);
                break;
            case INITIATOR_CHECK_FAILED:
            case INITIATOR_CHECK_MISSING:
                setButtonState(allDisconnected, false);
                break;
            case INITIATOR_CHECK_TERMINATED:
                setButtonState(allDisconnected, false);
                break;
            case DISTANCE_COLLECTION_FAILED:
                if (autoPositioningSuccessDistanceCollectionFail()) {
                    setButtonState(allDisconnected, R.string.ap_btn_collect_distances_again);
                } else {
                    setButtonState(allDisconnected, false);
                }
                break;
            case DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL:
                setButtonState(allDisconnected, false);
                break;
            case NOT_STARTED:
                setButtonState(allDisconnected, false);
                break;
            case DISTANCE_COLLECTION_TERMINATED:
                setButtonState(allDisconnected, false);
                break;
            case POSITIONS_SAVE_SUCCESS:
                setButtonState(allDisconnected, false);
                break;
            case COLLECTING_DISTANCES:
                setButtonState(true, R.string.btn_cancel);
                break;
            case SAVING_POSITIONS:
                setButtonState(R.string.btn_cancel);
                break;
            case DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS:
                // check if there is at least one node which needs to have the position saved
                boolean enableSave = autoPositioningManager.anyNodeNeedsPositionSave();
                setButtonState(allDisconnected, enableSave && allDisconnected);
                break;
            case POSITIONS_SAVE_FAILED:
                setButtonState(allDisconnected, allDisconnected);
                break;
            case POSITIONS_SAVE_TERMINATED:
                setButtonState(allDisconnected, allDisconnected);
                break;
        }
    }

    private void setButtonState(boolean measureEnabled, Integer measureTitle) {
        setButtonState(measureEnabled, measureTitle, false, null);
    }

    private void setButtonState(boolean measureEnabled, boolean actionEnabled) {
        setButtonState(measureEnabled, null, actionEnabled, null);
    }

    private void setButtonState(Integer actionTitle) {
        setButtonState(false, null, true, actionTitle);
    }

    private void setButtonState(boolean measureEnabled, Integer measureTitle, boolean saveEnabled, Integer saveTitle) {
        // measure
        measureBtn.setEnabled(measureEnabled);
        if (measureTitle != null) measureBtn.setText(measureTitle);
        else measureBtn.setText(R.string.ap_btn_measure);
        // save
        saveBtn.setEnabled(saveEnabled);
        if (saveTitle != null) saveBtn.setText(saveTitle);
        else saveBtn.setText(R.string.save);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // network change listener (keep datasource owned by the adapter in sync with the real list of items)
    //

    @Override
    protected boolean showProgress() {
        if (!autoPositioningManager.getApplicationState().idle) {
            return true;
        } // else:
        if (bleConnectionApi.allConnectionsClosed()) {
            return false;
        } // else: this is a bit complicated
        Set<String> inProgressDevices = bleConnectionApi.getInProgressDevices();
        for (String deviceBle : inProgressDevices) {
            if (!autoPositioningManager.hasInProgressConnection(deviceBle)) {
                return true;
            }
        } // else:
        return false;
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
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
    public void onItemMoved(int from, int to) {
        if (Constants.DEBUG)
            log.d("onItemMoved() called with: " + "from = [" + from + "], to = [" + to + "]");
        if (from != to) {
            adapter.moveNetworkNode(from - 1, to - 1); // due to header
            // reorder -> recompute based on the new order
            autoPositioningManager.reorder(Stream.of(adapter.getNodes()).map(NetworkNode::getId).collect(Collectors.toList()));
        }
    }

    @Override
    public void onNewZAxisValue(String zAxisValue) {
        autoPositioningManager.setZaxis(Util.parseLength(zAxisValue, appPreferenceAccessor.getLengthUnit()));
        // let the adapter redraw positions
        adapter.notifyDataSetChanged();
    }
}
