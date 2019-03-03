/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import android.os.SystemClock;

import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.ServiceData;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTag;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.exception.GattCharacteristicDecodeException;
import com.decawave.argomanager.util.gatt.GattDecoderCache;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Stateless discovery API - BLE implementation.
 * <p>
 * For an application-level component, see {@link com.decawave.argomanager.components.DiscoveryManager}
 */
public class DiscoveryApiBleImpl implements DiscoveryApi {
    // logging
    private static final ComponentLog log = new ComponentLog(DiscoveryApiBleImpl.class);
    private static final ApplicationComponentLog appLog = ApplicationComponentLog.newComponentLog(log, "DSRY");

    // handling strange state when: we are connected, still we receive an advertisement,
    // if the last connect attempt was made DISCONNECT_AFTER_TIMEOUT ago, we initiate a disconnect
    private static final int DISCONNECT_AFTER_TIMEOUT = 10000;
    // if there is detected change
    // we have to wait that long since the last connect attempt before we connect/discover again
    private static final int NODE_REDISCOVER_MIN_DELAY = 4000;
    // if there was sudden disconnect error,
    // we have to wait that long since the last connect attempt before we connect/discover again
    private static final int NODE_REDISCOVER_DELAY_ON_SUDDEN_DISCONNECT = BleConstants.RECONNECT_DELAY_ON_SUDDEN_DISCONNECT;
    private static final int NODE_REDISCOVER_DELAY_ON_CONNECT_TIMEOUT = BleConstants.RECONNECT_DELAY_ON_TIMEOUT;
    // if there was other than sudden disconnect error,
    // we have to wait that long since the last connect attempt before we connect/discover again
    private static final int NODE_REDISCOVER_DELAY_ON_OTHER_ERROR = BleConstants.RECONNECT_DELAY_ON_OTHER_ERROR;

    // dependencies
    private final PeriodicBleScanner bleScanner;
    private final BleConnectionApi bleConnectionApi;
    private final GattDecoderCache gattDecoderCache;
    private Func1<String, ConnectPriority> priorityResolver;

    // discovery FSM
    private DeviceDiscoveryFsm deviceDiscoveryFsm;

    // application level callback
    private Action2<ServiceData, NetworkNode> successCallback;
    private Action1<Fail> failCallback;
    private Map<String, ServiceData> cachedServiceData;
    private DiscoveryEventListener discoveryEventListener;
    // lower level scan/discovery callback - common
    private PeriodicBleScanner.Callback currentScanCallback;
    private Map<String, NodeInteractionContext> currentNodeInteractionContextMap = new HashMap<String, NodeInteractionContext>() {
        @Override
        public NodeInteractionContext remove(Object key) {
            if (Constants.DEBUG) {
                log.d("NICMAP@" + hashCode() + " remove: " + "key = [" + key + "]");
            }
            return super.remove(key);
        }

        @Override
        public NodeInteractionContext put(String key,
                                          NodeInteractionContext value) {
            if (Constants.DEBUG) {
                log.d("NICMAP@" + hashCode() + " put: " + "key = [" + key + "], value = [" + value + "]");
            }
            return super.put(key, value);
        }
    };

    /**
     * Complete node interaction context.
     */
    private class NodeInteractionContext {
        BleDevice device;
        NetworkNodeConnection connection;
        ServiceData serviceData;
        long lastConnectAttempt;
        long nextConnectAttempt;
        Boolean lastConnectEndedUpInError;

        @Override
        public String toString() {
            return "NodeInteractionContext{" + "device=" + device +
                    ", connection=" + connection +
                    ", serviceData=" + serviceData +
                    ", lastConnectAttempt=" + lastConnectAttempt +
                    ", nextConnectAttempt=" + nextConnectAttempt +
                    ", lastConnectEndedUpInError=" + lastConnectEndedUpInError +
                    '}';
        }
    }

    public interface DiscoveryEventListener {

        void onDiscoveryStarted();

        void onDiscoveryStopped();

        void onScanStarted();

        void onScanStopped();

        void onJustSeen(String bleAddress, int rssi);

    }

    private class ConnectionCallbackSet {
        Action1<String> onDisconnected;
        Action2<Fail,String> onFail;
        Action2<NetworkNode,ServiceData> onGetOtherSideEntity;

        boolean stillInterested() {
            // make sure this is still the 'current' session
            if (Constants.DEBUG) {
                Preconditions.checkState(currentCommonConnectionCallback == null && deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPED
                        ||
                        currentCommonConnectionCallback != null && deviceDiscoveryFsm.getState() != BleDeviceDiscoveryState.STOPPED);
            }
            return currentCommonConnectionCallback == this;
        }

        public void onConnected(NetworkNodeConnection networkNodeConnection) {
            if (stillInterested()) {
                // after we are connected, we initiate entity retrieval
                networkNodeConnection.getOtherSideEntity(
                        (nn) -> onGetOtherSideEntity(networkNodeConnection, nn),
                        (fail) -> this.onFail(networkNodeConnection, fail));
            }
        }

        public void onDisconnected(NetworkNodeConnection networkNodeConnection, Integer errorCode) {
            onDisconnected.call(networkNodeConnection.getOtherSideAddress());
        }

        public void onFail(NetworkNodeConnection connection,Fail fail) {
            onFail.call(fail,connection.getOtherSideAddress());
        }

        void onGetOtherSideEntity(NetworkNodeConnection connection, NetworkNode possiblyIncompleteNetworkNode) {
            if (stillInterested()) {
                // retrieve 'discovery info -> service data' from the session cache
                NodeInteractionContext nodeIc = currentNodeInteractionContextMap.get(connection.getOtherSideAddress());
                if (Constants.DEBUG) {
                    Preconditions.checkNotNull(nodeIc, "missing node interaction context for " + connection.getOtherSideAddress());
                }
                onGetOtherSideEntity.call(possiblyIncompleteNetworkNode, nodeIc.serviceData);
            }
            // initiate disconnect in each case
            connection.disconnect();
        }

    }
    private ConnectionCallbackSet currentCommonConnectionCallback;

    @Inject
    DiscoveryApiBleImpl(PeriodicBleScanner bleScanner,
                        BleConnectionApi bleConnectionApi,
                        GattDecoderCache gattDecoderCache) {
        this.bleScanner = bleScanner;
        this.bleConnectionApi = bleConnectionApi;
        this.gattDecoderCache = gattDecoderCache;
    }

    private void init() {
        if (deviceDiscoveryFsm == null) {
            //
            setupDiscoveryFsm();
        }
    }

    public void setDiscoveryEventListener(DiscoveryEventListener discoveryEventListener) {
        Preconditions.checkState(this.discoveryEventListener == null, "repeated call to setJustSeenListener? FIXME!");
        this.discoveryEventListener = discoveryEventListener;
    }

    private void setupDiscoveryFsm() {
        deviceDiscoveryFsm = new DeviceDiscoveryFsm();
        // configure DISCOVERING state
        deviceDiscoveryFsm.addOnStateEnteredHandler(BleDeviceDiscoveryState.DISCOVERING, this::doStartDiscovery);
        // configure stopping state
        deviceDiscoveryFsm.addOnStateEnteredHandler(BleDeviceDiscoveryState.STOPPING, this::onStopDiscoveryRequest);
        // configure stopped state
        deviceDiscoveryFsm.addOnStateEnteredHandler(BleDeviceDiscoveryState.STOPPED, fromState -> onDiscoveryStopped());
    }

    private void onDiscoveryStopped() {
        appLog.d("stopped");
        // stop the scan if necessary
        if (bleScanner.isStarted()) {
            Preconditions.checkNotNull(currentScanCallback);
            bleScanner.stopPeriodicScan();
        }
        // reset callbacks
        successCallback = null;
        failCallback = null;
        // reset variables
        currentNodeInteractionContextMap = null;
        currentScanCallback = null;
        currentCommonConnectionCallback = null;
        // broadcast internally first - so that onDiscoveryStarted is called before onScanStarted
        if (discoveryEventListener != null) discoveryEventListener.onDiscoveryStopped();
        // generic broadcast
        InterfaceHub.getHandlerHub(IhDiscoveryStateListener.class).afterDiscoveryStopped();
    }

    private void onStopDiscoveryRequest(BleDeviceDiscoveryState fromState) {
        appLog.d("stopping");
        Map<String, NodeInteractionContext> nicMap = currentNodeInteractionContextMap;
        // let the ongoing connections finish first (if any) and then make a transition to STOPPED
        deviceDiscoveryFsm.scheduleRunnable(() -> {
            // check that we are still stopping (fixing ISE)
            if (getState() == BleDeviceDiscoveryState.STOPPING) finishDiscoveryIfAppropriate(nicMap);
        });
    }

    private void doStartDiscovery(BleDeviceDiscoveryState fromState) {
        if (fromState == BleDeviceDiscoveryState.STOPPING) {
            appLog.d("continuing");
        } else {
            appLog.d("starting");
            // start a new discovery session
            final Map<String, NodeInteractionContext> sessionNodeInteractionContextMap = new HashMap<>();
            currentNodeInteractionContextMap = sessionNodeInteractionContextMap;
            if (cachedServiceData != null) {
                log.d("reusing previously cached service data: " + cachedServiceData);
                for (Map.Entry<String, ServiceData> entry : cachedServiceData.entrySet()) {
                    NodeInteractionContext nic = new NodeInteractionContext();
                    nic.lastConnectEndedUpInError = false;
                    nic.serviceData = entry.getValue();
                    sessionNodeInteractionContextMap.put(entry.getKey(), nic);
                }
                cachedServiceData = null;
            }
            // setup the session
            // setup common callback first
            ConnectionCallbackSet sessionCommonCallbackSet = new ConnectionCallbackSet();
            sessionCommonCallbackSet.onGetOtherSideEntity = (node, serviceData) -> {
                // we have read all necessary characteristics
                appLog.imp("notifying about " + node, LogEntryTagFactory.getDeviceLogEntryTag(node.getBleAddress()));
                // notify the application callback
                successCallback.call(serviceData, node);
                // let the connection API ignore any errors - we have successfully notified
                bleConnectionApi.ignoreSessionErrors(node.getBleAddress());
            };
            sessionCommonCallbackSet.onDisconnected = (bleAddress) -> {
                NodeInteractionContext nodeIc = sessionNodeInteractionContextMap.get(bleAddress);
                // set error flag
                if (nodeIc.lastConnectEndedUpInError == null) {
                    nodeIc.lastConnectEndedUpInError = false;
                }
                // set up the next connect attempt
                if (nodeIc.nextConnectAttempt == Long.MAX_VALUE) {
                    nodeIc.nextConnectAttempt = SystemClock.uptimeMillis() + NODE_REDISCOVER_MIN_DELAY;
                }
                // check if we are STOPPING
                if (deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPING) {
                    // we need to finish discovery
                    finishDiscoveryIfAppropriate(sessionNodeInteractionContextMap);
                }
            };
            sessionCommonCallbackSet.onFail = (fail, bleAddress) -> {
                if (sessionCommonCallbackSet.stillInterested()) {
                    // it still makes sense to propagate to application level fail callback
                    failCallback.call(fail);
                }
                // set the next connect attempt if appropriate
                NodeInteractionContext nodeIc = sessionNodeInteractionContextMap.get(bleAddress);
                nodeIc.lastConnectEndedUpInError = true;
                if (fail.errorCode == ErrorCode.BLE_CONNECTION_DROPPED) {
                    nodeIc.nextConnectAttempt = SystemClock.uptimeMillis() + NODE_REDISCOVER_DELAY_ON_SUDDEN_DISCONNECT;
                } else if (fail.errorCode == ErrorCode.BLE_CONNECT_TIMEOUT) {
                    nodeIc.nextConnectAttempt = SystemClock.uptimeMillis() + NODE_REDISCOVER_DELAY_ON_CONNECT_TIMEOUT;
                } else {
                    // there was another error
                    nodeIc.nextConnectAttempt = SystemClock.uptimeMillis() + NODE_REDISCOVER_DELAY_ON_OTHER_ERROR;
                }
            };
            final boolean[] unblockNeeded = {false};
            // setup the scan callback
            currentScanCallback = new PeriodicBleScanner.Callback() {

                @Override
                public void onServiceDataScan(BleDevice bluetoothDevice, int rssi, byte[] serviceDataBytes) {
                    if (!sessionCommonCallbackSet.stillInterested()) {
                        // ignore
                        return;
                    } // else:
                    // check that the application callback is still set
                    Preconditions.checkNotNull(successCallback);
                    Preconditions.checkNotNull(failCallback);
                    String bleAddress = bluetoothDevice.getAddress();
                    LogEntryTag loggingTag = LogEntryTagFactory.getDeviceLogEntryTag(bleAddress);
                    // update last-seen first (if part of persisted network)
                    if (discoveryEventListener != null) {
                        discoveryEventListener.onJustSeen(bleAddress, rssi);
                    }
                    // make sure that we have not initialized connection to that node yet
                    NodeInteractionContext nodeIc = sessionNodeInteractionContextMap.get(bleAddress);
                    if (inProgressConnectionRequest(nodeIc)) {
                        // optimization: it might be our connection in PENDING state, in which case, we can still safely update
                        // service data
                        if (nodeIc.connection.getState() == ConnectionState.PENDING) {
                            // update service data
                            decodeServiceData(serviceDataBytes, nodeIc.serviceData, bleAddress, loggingTag);
                        }
                        // there already is a live/pending connection
                        return;
                    }
                    // check current state of discovery
                    if (getState() == BleDeviceDiscoveryState.STOPPING) {
                        // we cannot make new connections anyway
                        return;
                    }
                    // additional skip checks
                    ServiceData newServiceData = new ServiceData();
                    if (!decodeServiceData(serviceDataBytes, newServiceData, bleAddress, loggingTag)) {
                        // ignore this broadcast
                        return;
                    }
                    if (nodeIc != null) {
                        Boolean lastWasError = nodeIc.lastConnectEndedUpInError;
                        if (nodeIc.serviceData.equals(newServiceData) && (lastWasError != null && !lastWasError)) {
                            appLog.d("skipping " + bluetoothDevice + ", discovery info unchanged", loggingTag);
                            return;
                        } // else:
                        // make sure that this is not injected cached discovery info
                        if (nodeIc.connection != null) {
                            // this is our own discovery info
                            if (!nodeIc.connection.isDisconnected()) {
                                if (nodeIc.lastConnectAttempt + DISCONNECT_AFTER_TIMEOUT < SystemClock.uptimeMillis()) {
                                    ConnectionState state = nodeIc.connection.getState();
                                    if (state == ConnectionState.CONNECTED) {
                                        appLog.we("discovered " + bluetoothDevice + " but we are still connected (?), initiating disconnect", ErrorCode.DISCOVERY_OVERLAP, loggingTag);
                                        nodeIc.connection.disconnect();
                                    }
                                }
                                // wait for CLOSED
                                return;
                            } // else:
                            // make sure that there is enough time since last connect attempt
                            long toNextConnectAttempt = nodeIc.nextConnectAttempt - SystemClock.uptimeMillis();
                            if (toNextConnectAttempt > 0) {
                                if (Constants.DEBUG) {
                                    log.d("nextConnectAttempt (in " + toNextConnectAttempt + "ms) not reached yet, ignoring " + bluetoothDevice);
                                }
                                return;
                            }
                            appLog.imp("discovered " + bluetoothDevice + ", nextConnectAttempt elapsed, connecting, newServiceData = " + newServiceData, loggingTag);
                        } else {
                            // connection is null, these are injected service data
                            nodeIc.device = bluetoothDevice;
                            appLog.imp("discovered " + bluetoothDevice + ", overriding injected service data with = " + newServiceData, loggingTag);
                        }
                        // set the new service data
                        nodeIc.serviceData = newServiceData;
                    } else {
                        nodeIc = new NodeInteractionContext();
                        nodeIc.device = bluetoothDevice;
                        nodeIc.serviceData = newServiceData;
                        sessionNodeInteractionContextMap.put(bleAddress, nodeIc);
                        appLog.imp("discovered new " + bluetoothDevice + ", serviceData = " + nodeIc.serviceData, loggingTag);
                    }
                    nodeIc.lastConnectAttempt = SystemClock.uptimeMillis();
                    nodeIc.nextConnectAttempt = Long.MAX_VALUE;
                    nodeIc.lastConnectEndedUpInError = null;
                    // setup it's own connection (but with a common callback)
                    nodeIc.connection = bleConnectionApi.connect(bluetoothDevice.getAddress(), priorityResolver.call(bleAddress),
                            sessionCommonCallbackSet::onConnected,
                            sessionCommonCallbackSet::onFail,
                            sessionCommonCallbackSet::onDisconnected);
                }

                @Override
                public void onScanFailed() {
                    // we might need to distinguish failed / stopped state
                    appLog.we("discovery scan failed!", ErrorCode.DISCOVERY_FAILED);
                    // there might be ongoing connections, we have to go through STOPPING state
                    deviceDiscoveryFsm.setState(BleDeviceDiscoveryState.STOPPING);
                }

                @Override
                public void onScanStarted() {
                    if (Constants.DEBUG) {
                        appLog.i("onScanStarted");
                        Preconditions.checkState(bleConnectionApi.connectionRequestsBlocked() == YesNoAsync.YES, "connection requests are not blocked!");
                    }
                    // let the listener know
                    if (discoveryEventListener != null) discoveryEventListener.onScanStarted();
                }

                @Override
                public void onScanStopped() {
                    if (Constants.DEBUG) {
                        appLog.i("onScanStopped");
                    }
                    // broadcast internally first
                    if (discoveryEventListener != null) discoveryEventListener.onScanStopped();
                    //
                    unblockIfNecessary();
                    // check if there was issued a stop request
                    if (getState() == BleDeviceDiscoveryState.STOPPING) {
                        finishDiscoveryIfAppropriate(sessionNodeInteractionContextMap);
                    }
                }

                @Override
                public void onStarted() {
                    if (Constants.DEBUG) {
                        log.d("onStarted");
                    }
                    // broadcast internally first - so that onDiscoveryStarted is called before onScanStarted
                    if (discoveryEventListener != null) discoveryEventListener.onDiscoveryStarted();
                    // broadcast publicly
                    InterfaceHub.getHandlerHub(IhDiscoveryStateListener.class).afterDiscoveryStarted();
                }

                @Override
                public void onStopped() {
                    if (Constants.DEBUG) {
                        log.d("onStopped");
                    }
                    unblockIfNecessary();
                }

                private void unblockIfNecessary() {
                    if (unblockNeeded[0]) {
                        uiHandler.postDelayed(bleConnectionApi::unblockConnectionRequests, 200);
                        unblockNeeded[0] = false;
                    }
                }

            };
            // set up current common connection callback
            currentCommonConnectionCallback = sessionCommonCallbackSet;
            //
            bleScanner.startPeriodicScan((a) -> {
                bleConnectionApi.blockConnectionRequests(a);
                unblockNeeded[0] = true;
            }, currentScanCallback);
        }
    }

    private boolean decodeServiceData(byte[] serviceDataBytes,
                                      ServiceData newServiceData,
                                      String bleAddress,
                                      LogEntryTag loggingTag) {
        try {
            gattDecoderCache.getDecoder(bleAddress).decodeServiceData(serviceDataBytes, newServiceData);
            return true;
        } catch (GattCharacteristicDecodeException exc) {
            // report
            appLog.we("error while decoding service data of " + bleAddress, ErrorCode.GATT_REPRESENTATION, exc, loggingTag);
            // ignore this broadcast
            return false;
        }
    }

    private boolean inProgressConnectionRequest(NodeInteractionContext currContext) {
        return currContext != null && currContext.connection != null && currContext.connection.getState().inProgress;
    }

    private void finishDiscoveryIfAppropriate(Map<String, NodeInteractionContext> nodeInteractionContextMap) {
        if (Constants.DEBUG) {
            log.d("finishDiscoveryIfAppropriate: " + "nodeInteractionContextMap = [" + nodeInteractionContextMap + "], getState(): " + deviceDiscoveryFsm.getState());
            Preconditions.checkState(deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPING, "current device discovery FSM state: " + deviceDiscoveryFsm.getState());
        }
        boolean anyLiveConnection = false;
        if (nodeInteractionContextMap != null) {
            for (NodeInteractionContext nodeIc : nodeInteractionContextMap.values()) {
                // connection might be null in case of injected cached service data
                if (inProgressConnectionRequest(nodeIc)) {
                    if (Constants.DEBUG) {
                        log.d("connection to " + nodeIc.connection.getOtherSideAddress() + " still not finished: " + nodeIc.connection.getState());
                    }
                    anyLiveConnection = true;
                    break;
                }
            }
        }
        if (!anyLiveConnection && !bleScanner.isBleScanning()) {
            deviceDiscoveryFsm.setState(BleDeviceDiscoveryState.STOPPED);
        } else if (Constants.DEBUG) {
            log.d("cannot finish discovery: either there are some pending connections or BLE scanner is in scan period");
        }
    }

    @Override
    public boolean isDiscovering() {
        return deviceDiscoveryFsm != null && deviceDiscoveryFsm.getState() != BleDeviceDiscoveryState.STOPPED;
    }

    @Override
    public boolean isStopping() {
        return deviceDiscoveryFsm != null && deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPING;
    }

    @Override
    public void startDiscovery(@NotNull Action2<ServiceData, NetworkNode> serviceDataListener,
                               @NotNull Action1<Fail> onFailCallback,
                               @NotNull Func1<String, ConnectPriority> priorityResolver,
                               @Nullable Map<String, ServiceData> initialServiceData) {
        if (Constants.DEBUG) {
            log.d("startDiscovery: " + "onSuccessCallback = [" + serviceDataListener + "], onFailCallback = [" + onFailCallback + "], initialServiceData = [" + initialServiceData + "]");
            Preconditions.checkNotNull(serviceDataListener, "serviceDataListener must NOT be null");
            Preconditions.checkNotNull(priorityResolver, "priorityResolver must NOT be null");
            Preconditions.checkNotNull(onFailCallback, "onFailCallback must NOT be null");
        }
        // initialize
        init();
        Preconditions.checkState(deviceDiscoveryFsm.getState() == null || deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPED);
        Preconditions.checkState(bleConnectionApi.connectionRequestsBlocked() == YesNoAsync.NO, "connection requests are blocked!");
        this.successCallback = serviceDataListener;
        this.failCallback = onFailCallback;
        this.priorityResolver = priorityResolver;
        if (initialServiceData != null && !initialServiceData.isEmpty()) {
            // inject service data
            this.cachedServiceData = new HashMap<>();
            for (Map.Entry<String, ServiceData> entry : initialServiceData.entrySet()) {
                cachedServiceData.put(entry.getKey(), new ServiceData(entry.getValue()));
            }
        } else {
            cachedServiceData = null;
        }
        deviceDiscoveryFsm.setState(BleDeviceDiscoveryState.DISCOVERING);
    }

    @Override
    public void stopDiscovery() {
        if (Constants.DEBUG) {
            log.d("stopDiscovery()");
        }
        BleDeviceDiscoveryState s = deviceDiscoveryFsm.getState();
        if (Constants.DEBUG) {
            Preconditions.checkState(s != BleDeviceDiscoveryState.STOPPED && s != BleDeviceDiscoveryState.STOPPING, "state is " + s);
        }
        deviceDiscoveryFsm.setState(BleDeviceDiscoveryState.STOPPING);
    }

    @Override
    public void continueDiscovery() {
        if (Constants.DEBUG) {
            log.d("continueDiscovery");
            Preconditions.checkState(deviceDiscoveryFsm.getState() == BleDeviceDiscoveryState.STOPPING);
        }
        deviceDiscoveryFsm.setState(BleDeviceDiscoveryState.DISCOVERING);
    }

    public BleDeviceDiscoveryState getState() {
        return deviceDiscoveryFsm.getState();
    }

}
