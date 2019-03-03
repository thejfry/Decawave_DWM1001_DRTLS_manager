/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.interaction.ProxyPosition;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.IhConnectionStateListener;
import com.decawave.argomanager.argoapi.ble.IhFirmwareUploadListener;
import com.decawave.argomanager.argoapi.ble.connection.NetworkNodeBleConnection;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogBuffer;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.ui.listadapter.DebugLogBufferEntryAdapter;
import com.decawave.argomanager.ui.listadapter.LogMessageHolder;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Fragment showing debug logs.
 */
public class DeviceDebugConsoleFragment extends LogBufferFragment implements IhConnectionStateListener, IhFirmwareUploadListener {
    private static final ComponentLog log = new ComponentLog(DeviceDebugConsoleFragment.class);
    private static final String BK_DEVICE_BLE_ADDRESS = "DEVICE_BLE";

    private ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "CONSOLE");

    @Inject
    BleConnectionApi bleConnectionApi;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    DiscoveryManager discoveryManager;

    @Inject
    LocationDataLogger locationDataLogger;

    // UI elements
    @BindView(R.id.connectButton)
    Button btnConnect;

    @BindView(R.id.disconnectButton)
    Button btnDisconnect;

    @BindView(R.id.fetchButton)
    Button btnFetch;

    @BindView(R.id.observePositionButton)
    Button btnObserve;

    @BindView(R.id.observePositionFiller)
    View observeFiller;

    @BindView(R.id.nodeTypeView)
    NodeStateView nodeStateView;

    // members
    private String deviceBleAddress;
    private String shortDeviceId;


    private static final Object DISCONNECT_RUNNABLE_TOKEN = new Object();

    // static references
    private static NetworkNodeBleConnection connection;

    public DeviceDebugConsoleFragment() {
        super(FragmentType.DEVICE_DEBUG_CONSOLE, daApp.getString(R.string.screen_title_debug_console), "device-debug.log");
    }

    @Override
    public String getScreenTitle() {
        return daApp.getString(R.string.screen_title_debug_console, shortDeviceId);
    }

    @Override
    LogBuffer getLogBuffer() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(deviceBleAddress, "please call handleOnCreate() first");
        }
        return logEntryCollector.getDeviceDebugLog(deviceBleAddress);
    }

    @Override
    protected RecyclerView.Adapter<LogMessageHolder> createAdapter() {
        return new DebugLogBufferEntryAdapter(getLogBuffer());
    }

    @Override
    protected void handleOnCreate(Bundle savedInstanceState) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(getArguments(), "must set arguments for device");
            Preconditions.checkState(getArguments().containsKey(BK_DEVICE_BLE_ADDRESS), "must set BLE_ADDRESS argument for device");
        }
        // extract device BLE address
        deviceBleAddress = getArguments().getString(BK_DEVICE_BLE_ADDRESS);
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(deviceBleAddress, "please use getArgsForDevice() to retrieve initial bundle");
        }
        Long deviceId = networkNodeManager.bleToId(deviceBleAddress);
        if (deviceId != null) {
            shortDeviceId = Util.shortenNodeId(deviceId, true);
        } else {
            shortDeviceId = deviceBleAddress.substring(0, 6) + "…";
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_debug_console_fragment, container, false);
        // generic configuration routine
        configureListView(v);
        // bind buttons
        ButterKnife.bind(this, v);
        // configure button handlers
        btnConnect.setOnClickListener((b) -> connect());
        btnFetch.setOnClickListener((b) -> fetchProperties());
        btnDisconnect.setOnClickListener((b) -> {
            closeConnectionIfOpen(deviceBleAddress, true);
            updateUi();
        });
        btnObserve.setOnClickListener((b) -> toggleObserve());
        return v;
    }

    private void toggleObserve() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(connection);
            Preconditions.checkState(connection.isConnected());
        }
        YesNoAsync observing = isObserving();
        if (observing == YesNoAsync.YES) {
            // stop observation
            stopObserve();
        } else if (observing == YesNoAsync.NO){
            if (getObserveMode() == ObserveMode.LOCATION_DATA) {
                // start location data observation
                startLocationDataObserve();
            } else {
                // start proxy position observation
                startProxyPositionDataObserve();
            }
        }
    }

    private void startLocationDataObserve() {
        connection.observeLocationData(new NetworkNodeConnection.LocationDataChangedCallback() {
            @Override
            public void onStarted() {
                logImp("observing location data");
                if (isResumed()) {
                    updateUi();
                }
            }

            @Override
            public void onChange(LocationData locationData) {
                // network node manager and location data logger are already notified
            }

            @Override
            public void onFail(Fail fail) {
                logWe("failed to enable/disable location data observation: " + fail.message, fail.errorCode);
                if (isResumed()) {
                    updateUi();
                }
            }

            @Override
            public void onStopped() {
                logImp("location data observation stopped");
                if (isResumed()) {
                    updateUi();
                }
            }
        });
    }

    private void startProxyPositionDataObserve() {
        Map<Short, Integer> proxyPositionCounter = new HashMap<>();
        connection.observeProxyPositionData(new NetworkNodeConnection.ProxyPositionDataChangedCallback() {
            @Override
            public void onStarted() {
                logImp("observing proxy position data");
                if (isResumed()) {
                    updateUi();
                }
            }

            @Override
            public void onChange(List<ProxyPosition> proxyPositions) {
                // network node manager and location data logger are already notified
                StringBuilder sb = new StringBuilder();
                for (ProxyPosition proxyPosition : proxyPositions) {
                    sb.append("nodeId = ").append(Util.formatAsHexa(proxyPosition.nodeId, true))
                            .append(", position = ").append(proxyPosition.position).append("; ");
                    Integer oldCounter = proxyPositionCounter.get(proxyPosition.nodeId);
                    proxyPositionCounter.put(proxyPosition.nodeId, oldCounter == null ? 1 : oldCounter + 1);
                }
                appLog.d("proxy position notification: " + sb.toString(), deviceTag());
            }

            @Override
            public void onFail(Fail fail) {
                logWe("failed to enable/disable proxy position data observation: " + fail.message, fail.errorCode);
                if (isResumed()) {
                    updateUi();
                }
            }

            @Override
            public void onStopped() {
                logImp("proxy position data observation stopped, summary:");
                for (Map.Entry<Short, Integer> longIntegerEntry : proxyPositionCounter.entrySet()) {
                    appLog.d("device " + Util.formatAsHexa(longIntegerEntry.getKey(), true) + " positions: " + longIntegerEntry.getValue(), deviceTag());
                }
                if (isResumed()) {
                    updateUi();
                }
            }
        });
    }

    enum ObserveMode {
        LOCATION_DATA, PROXY_POSITIONS
    }

    private ObserveMode getObserveMode() {
        NetworkNode node = networkNodeManager.getNode(deviceBleAddress).asPlainNode();
        return node.isAnchor() && node.getUwbMode() == UwbMode.PASSIVE ? ObserveMode.PROXY_POSITIONS : ObserveMode.LOCATION_DATA;
    }

    private YesNoAsync isObserving() {
        if (getObserveMode() == ObserveMode.LOCATION_DATA) {
            return connection.isObservingLocationData();
        } else {
            return connection.isObservingProxyPositionData();
        }
    }

    private void stopObserve() {
        if (connection.isObservingLocationData() == YesNoAsync.YES) {
            connection.stopObserveLocationData();
        } else if (connection.isObservingProxyPositionData() == YesNoAsync.YES) {
            connection.stopObserveProxyPositionData();
        }
    }

    private void fetchProperties() {
        connection.getOtherSideEntity(
                (networkNode) -> logImp("fetched network node: " + networkNode),
                (fail) -> logWe("cannot fetch properties! " + fail.message, fail.errorCode));
    }

    private LogEntryTag deviceTag() {
        return LogEntryTagFactory.getDeviceLogEntryTag(deviceBleAddress);
    }

    public static Bundle getArgsForDevice(String nodeBle) {
        Bundle b = new Bundle();
        b.putString(BK_DEVICE_BLE_ADDRESS, nodeBle);
        return b;
    }

    private void connect() {
        // bugfix: it might happen that there was initiated a connection attempt in the meantime
        if (connection == null || connection.getState() == ConnectionState.CLOSED) {
            // create a new connection
            connection = bleConnectionApi.connect(deviceBleAddress,
                    ConnectPriority.HIGH,
                    (c) -> {
                        logImp("established connection");
                        // do not disconnect automatically in case of a problem - let the user decide what does
                        // he want to do next
                        connection.setDisconnectOnProblem(false);
                        if (isResumed()) {
                            updateUi();
                        }
                    },
                    (c, f) -> {
                        logWe("connection failed!", ErrorCode.BLE_CONNECTION_DROPPED);
                        connection = null;
                        if (isResumed()) {
                            updateUi();
                        }
                    }, (nnc,err) -> logImp("disconnected"));
        }
    }

    private void logWe(String message, int bleConnectionDropped) {
        appLog.we(message, bleConnectionDropped, deviceTag());
    }

    private void logImp(String message) {
        appLog.imp(message, deviceTag());
    }

    private static void closeConnectionIfOpen(String deviceBleAddress, boolean ifEquals) {
        if (connection != null
                && connection.isConnected()
                && (ifEquals == connection.getOtherSideAddress().equals(deviceBleAddress))) {
            connection.disconnect();
            connection = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // unschedule the disconnect
        uiHandler.removeCallbacks(DISCONNECT_RUNNABLE_TOKEN);
        // disconnect if we are still connected to some other node
        closeConnectionIfOpen(deviceBleAddress, false);
        //
        NetworkNodeEnhanced networkNode = networkNodeManager.getNode(deviceBleAddress);
        if (networkNode != null) {
            nodeStateView.setVisibility(View.VISIBLE);
            nodeStateView.setNetworkNode(networkNode.asPlainNode());
        } else {
            nodeStateView.setVisibility(View.GONE);
        }
        //
        updateUi();
    }

    public void onPause() {
        super.onPause();
        // schedule disconnect
        uiHandler.postDelayed(() -> closeConnectionIfOpen(deviceBleAddress, true), 2000, DISCONNECT_RUNNABLE_TOKEN);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    void updateUi() {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        // button appearance handling
        if (connection == null || connection.getState() == ConnectionState.CLOSED) {
            // we do not have a connection, or the connection is already closed
            disableButtons();
            btnConnect.setEnabled(true);
        } else {
            // we have a live or pending connection
            ConnectionState connectionState = connection.getState();
            switch (connectionState) {
                case PENDING:
                case CONNECTING:
                case DISCONNECTING:
                    disableButtons();
                    break;
                case CONNECTED:
                    btnConnect.setEnabled(false);
                    btnDisconnect.setEnabled(true);
                    // it depends whether we are doing firmware update
                    btnFetch.setEnabled(connection.isUploadingFirmware() == YesNoAsync.NO);
                    // these buttons have their own complicated logic
                    configureObserveButton();
                    break;
                default:
                    throw new IllegalStateException("FIXME: should never happen!: " + connectionState);
            }
        }
        // nodeStateView handling
        ConnectionState connectionState = bleConnectionApi.getConnectionState(deviceBleAddress);
        nodeStateView.setState(connectionState.disconnected ? null : NodeStateView.State.CONNECTED, true);
    }

    private void configureObserveButton() {
        if (connection != null && connection.getState() == ConnectionState.CONNECTED) {
            if (connection.isUploadingFirmware() != YesNoAsync.NO) {
                btnObserve.setEnabled(false);
            } else {
                YesNoAsync observing = isObserving();
                switch (observing) {
                    case TO_YES:
                    case TO_NO:
                        btnObserve.setEnabled(false);
                        break;
                    case YES:
                        btnObserve.setText(R.string.btn_observe_stop);
                        btnObserve.setEnabled(true);
                        break;
                    case NO:
                        btnObserve.setText(R.string.btn_observe);
                        btnObserve.setEnabled(true);
                        break;
                }
            }
        } else {
            // we are not connected
            btnObserve.setText(R.string.btn_observe);
            btnObserve.setEnabled(false);
        }
    }

    private void disableButtons() {
        btnConnect.setEnabled(false);
        btnFetch.setEnabled(false);
        btnDisconnect.setEnabled(false);
        btnObserve.setEnabled(false);
    }

    @Override
    public void onConnecting(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onConnected(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onDisconnecting(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onDisconnected(String bleAddress, Boolean sessionSuccess) {
        upUi(bleAddress);
    }

    @Override
    public void onInitiating(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onUploading(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onCleaningUp(String bleAddress) {
        upUi(bleAddress);
    }

    @Override
    public void onFinished(String bleAddress) {
        upUi(bleAddress);
    }

    private void upUi(String bleAddress) {
        if (bleAddress.equals(deviceBleAddress)) {
            updateUi();
        }
    }

    // TODO: we should have callbacks also for position observation status change and firmware upload status change
}
