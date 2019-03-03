/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import android.support.annotation.NonNull;

import com.annimon.stream.function.BiConsumer;
import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.interaction.ProxyPosition;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsmImpl;
import com.decawave.argomanager.argoapi.ble.IhConnectionStateListener;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.impl.ConcurrentOperationQueue;
import com.decawave.argomanager.components.impl.ConcurrentOperationQueueImpl;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.debuglog.LogBlockStatus;
import com.decawave.argomanager.util.Util;
import com.decawave.argomanager.util.gatt.GattDecoderCache;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Android BLE connection API implementation.
 */
public class BleConnectionApiImpl implements BleConnectionApi {
    private static final ComponentLog log = new ComponentLog(BleConnectionApiImpl.class);
    // dependencies
    private final BleAdapter bleAdapter;
    private final NetworkNodeManager networkNodeManager;
    private final LogBlockStatus logBlockStatus;
    private final GattDecoderCache gattDecoderCache;
    // cache
    private final NetworkNodeBleConnectionImpl.NetworkNodeInterceptor commonNetworkNodeInterceptor;
    // state
    // limits concurrently running connections
    private final ConcurrentOperationQueue concurrentQueue;
    // serializes connect routine (parallelism = 1)
    private final ConcurrentOperationQueue connectQueue;
    private ConnectionContainer connections;
    private Set<String> inError;
    private Set<String> ignoreSessionErrors;
    private Map<String, Boolean> lastSessionSuccessful;
    private ObservationListener observationListener;


    public interface ObservationListener {

        void onJustProxied(String bleAddress, int timeout);

        void onDirectLocationObservationStart(String bleAddress);

        void onDirectLocationObservationStop(String bleAddress);

        void onDirectLocationObservationFailed(String bleAddress);

    }

    @Inject
    BleConnectionApiImpl(BleAdapter bleAdapter,
                         NetworkNodeManager networkNodeManager,
                         LogBlockStatus logBlockStatus,
                         LocationDataLogger locationDataLogger,
                         GattDecoderCache gattDecoderCache) {
        this.bleAdapter = bleAdapter;
        this.networkNodeManager = networkNodeManager;
        this.logBlockStatus = logBlockStatus;
        this.gattDecoderCache = gattDecoderCache;
        this.concurrentQueue = new ConcurrentOperationQueueImpl(BleConstants.MAX_CONCURRENT_CONNECTION_COUNT);
        // we need extra queue specifically for handling connect: due to serialized connect request handling in Android OS
        this.connectQueue = new ConcurrentOperationQueueImpl(1);
        //
        this.connections = new ConnectionContainer();
        this.inError = new HashSet<>();
        this.lastSessionSuccessful = new HashMap<>();
        this.ignoreSessionErrors = new HashSet<>();
        // create a listener which propagates changes of node directly to network model manager
        this.commonNetworkNodeInterceptor = new NetworkNodeBleConnectionImpl.NetworkNodeInterceptor() {
            @Override
            public void onNodeIntercepted(NetworkNode node) {
                // propagate to manager
                networkNodeManager.onNodeIntercepted(node);
            }

            @Override
            public void onAnchorDistancesUpdate(String bleAddress, List<RangingAnchor> distances) {
                // propagate to manager
                Long nodeId = networkNodeManager.bleToId(bleAddress);
                if (nodeId != null) {
                    // let the network node manager know
                    networkNodeManager.updateAnchorDistances(nodeId, distances);
                    // log the received distances
                    locationDataLogger.logLocationData(nodeId, bleAddress, null, distances, false);
                }
            }

            @Override
            public void onTagLocationDataUpdate(String bleAddress, LocationData locationData) {
                // propagate to manager
                Long nodeId = networkNodeManager.bleToId(bleAddress);
                if (nodeId != null) {
                    // let the network node manager know
                    networkNodeManager.updateTagLocationData(nodeId, locationData);
                    // log the received location data
                    locationDataLogger.logLocationData(nodeId, bleAddress, locationData.position, locationData.distances, false);
                }
            }

            @Override
            public void onProxyPositionUpdate(ProxyPosition proxyPosition) {
                NetworkNodeEnhanced node = networkNodeManager.getNodeByShortId(proxyPosition.nodeId);
                if (node != null) {
                    // this is a known node
                    if (!node.isTag()) {
                        if (Constants.DEBUG) {
                            log.d("skipping proxied position data notification, the notified node is not a tag - probably discovery update pending");
                        }
                        return;
                    }
                    String bleAddress = node.getBleAddress();
                    Long nodeId = node.getId();
                    NetworkNodeConnection connection = connections.affinityGet(bleAddress);
                    // check if we are currently observing location data directly
                    if (connection == null || !connection.isConnected() || connection.isObservingLocationData() != YesNoAsync.YES) {
                        if (Constants.DEBUG) {
                            log.d("propagating proxied position to upper layers: " + proxyPosition);
                        }
                        // let the proxy listener know
                        observationListener.onJustProxied(bleAddress, getProxyPresenceTimeout((TagNode) node.asPlainNode()));
                        // propagate to manager
                        networkNodeManager.updateTagLocationData(nodeId, new LocationData(proxyPosition.position, null));
                        // log the received location data
                        locationDataLogger.logLocationData(nodeId, bleAddress, proxyPosition.position, null, true);
                    } else {
                        // skip
                        if (Constants.DEBUG) {
                            log.d("skipping proxied position data notification, there is a direct location data observation set-up");
                        }
                    }
                } else {
                    if (Constants.DEBUG) {
                        log.d("skipping proxy position notification: node " + Util.formatAsHexa(proxyPosition.nodeId, true) + " unknown");
                    }
                }
            }
        };
    }

    private static int getProxyPresenceTimeout(TagNode tagNode) {
        // 5s is default minimum
        int i = 5000;
        Integer[] bis = { tagNode.getStationaryUpdateRate(), tagNode.getUpdateRate() };
        for (Integer bi : bis) {
            if (bi != null && bi * 2 > i) {
                i = bi * 2;
            }
        }
        return i;
    }

    public void setObservationListener(ObservationListener observationListener) {
        this.observationListener = observationListener;
    }

    @Override
    public NetworkNodeBleConnection connect(@NotNull String address,
                                         @NotNull ConnectPriority connectPriority,
                                         @NotNull Action1<NetworkNodeConnection> onConnectedCallback,
                                         @Nullable Action2<NetworkNodeConnection, Fail> onFailCallback,
                                         @Nullable Action2<NetworkNodeConnection, Integer> onDisconnectedCallback) {
        if (Constants.DEBUG) {
            log.d("connectToNetworkNode: " + "address = [" + address + "]");
        }
        // initiate the connection with concurrent operation limit
        ConcurrentOperationQueue.Token token[] = { null };
        BleDevice bleDevice = bleAdapter.getRemoteDevice(address);
        // create the connection wrapper
        NetworkNodeConnectionWrapper connectionWrapper = new NetworkNodeConnectionWrapper(address);
        // initialize forward references
        boolean[] doExtraLoop = {true};
        ConcurrentOperationQueue.Token[] connectToken = {null};
        // create the interaction FSM
        final GattInteractionFsm gattInteractionFsm = new GattInteractionFsmImpl(bleDevice, networkNodeManager,

                new GattInteractionFsmImpl.ConnectionListener() {
                    boolean reportedOnDisconnecting = false;
                    boolean outerConnectReported = false;
                    boolean innerConnectReported = false;
                    boolean directLocationObservationSetup = false;

                    // we are overriding only connection-specific routines here (connectionWrapper,onDisconnectedCallback,token[0])
                    @Override
                    public void onConnecting(String address) {
                        // set the state to connecting
                        connectionWrapper.setInjectedState(ConnectionState.CONNECTING);
                        // call the generic routine now
                        _onConnecting(address);
                    }

                    @Override
                    public void onConnected(String address,
                                            GattInteractionFsm fsm,
                                            SynchronousBleGatt syncGatt) {
                        reportConnectFinished(address);
                    }

                    @Override
                    public void onConnectFailed(String address, GattInteractionFsm fsm) {
                        reportConnectFinished(address);
                    }

                    @Override
                    public void onServicesDiscovered(String address,
                                                     GattInteractionFsm fsm,
                                                     SynchronousBleGatt bleGatt) {
                        // there was a disconnect request in the meantime
                        if (checkDisconnectRequest(address, fsm)) return;
                        // create the real connection instance
                        NetworkNodeBleConnectionImpl connection = new NetworkNodeBleConnectionImpl(fsm,
                                bleGatt,
                                logBlockStatus,
                                () -> gattDecoderCache.getDecoder(address),
                                bleAddress -> processOnDisconnecting(bleAddress, false),
                                commonNetworkNodeInterceptor) {

                            // override the location observation - we need to be notified about start/stop events
                            @Override
                            public void observeLocationData(LocationDataChangedCallback locationDataChangedCallback) {
                                super.observeLocationData(new LocationDataChangedCallbackWrapper(locationDataChangedCallback) {

                                    @Override
                                    public void onStarted() {
                                        observationListener.onDirectLocationObservationStart(address);
                                        super.onStarted();
                                        directLocationObservationSetup = true;
                                    }

                                    @Override
                                    public void onStopped() {
                                        super.onStopped();
                                        observationListener.onDirectLocationObservationStop(address);
                                        directLocationObservationSetup = false;
                                    }

                                    @Override
                                    public void onFail(Fail fail) {
                                        super.onFail(fail);
                                        observationListener.onDirectLocationObservationFailed(address);
                                        directLocationObservationSetup = false;
                                    }
                                });
                            }

                        };
                        // bugfix: inconsistent anchor list and location data (declared and real length) negotiate a new MTU
                        // partly initialize wrapper - so that it can process results of asynchronous operations
                        connectionWrapper.setGattCallback(connection.asGattCallback());
                        // run the async operation
                        connection.changeMtu(BleConstants.MTU_ON_LOCATION_DATA_OBSERVE, () -> {
                            // onSuccess
                            // we have to check if we were disconnected in the meantime - fixing bug #194
                            if (checkDisconnectRequest(address, fsm)) return;
                            // we've got fully initialized connection, inject the connection to wrapper
                            connectionWrapper.setDelegate(connection);
                            // call the generic routine - pretend that the connection has been established now
                            _onConnectionInitialized(address);
                            // notify onConnected callback
                            onConnectedCallback.call(connectionWrapper);
                            // from outer POV, we are connected now, we should notify about disconnecting
                            outerConnectReported = true;
                        }, (fail) -> {
                            // onFail, we will get disconnected automatically
                            log.w("MTU change failed");
                            // notify the callback - pretend as if the connection was not established
                            if (onFailCallback != null) onFailCallback.call(connectionWrapper, fail);
                        });
                    }

                    private void reportConnectFinished(String address) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(!innerConnectReported, address);
                        }
                        // let the serialized queue know that we are connected - so that other connection requests may take place
                        connectQueue.onOperationFinished(connectToken[0]);
                        innerConnectReported = true;
                    }

                    private boolean checkDisconnectRequest(String address, GattInteractionFsm fsm) {
                        if (connectionWrapper.isDisconnectRequired()) {
                            // there was a disconnect request in the meantime
                            log.d("onServiceDiscovered: " + address + ", but there was an asynchronous disconnect request, disconnecting");
                            // do not notify anybody - we want to do the disconnect sequence only
                            // pretend as if the connection did not happen at all
                            fsm.initiateDisconnect();
                            return true;
                        } // else:
                        return false;
                    }

                    @Override
                    public void onDisconnecting(String address) {
                        if (outerConnectReported) {
                            // propagate
                            processOnDisconnecting(address, true);
                        }
                    }

                    // we might receive this callback from
                    // 1. connection: higher level API call - NetworkNodeConnection#disconnect()
                    // 2. low level API: GattInteractionFsmImpl when it reaches DISCONNECTING state
                    private void processOnDisconnecting(String address, boolean notifyConnection) {
                        if (!reportedOnDisconnecting) {
                            // this layer has to unify and make the broadcast consistent
                            reportedOnDisconnecting = true;
                            // did the notification arrive from low-level
                            if (notifyConnection) {
                                NetworkNodeConnection delegate = connectionWrapper.getDelegate();
                                if (delegate != null) {
                                    // let the instance know - directly
                                    ((NetworkNodeBleConnectionImpl) delegate).onDisconnecting();
                                }
                            }
                            _onDisconnecting(address);
                        }
                    }

                    @Override
                    public void onDisconnected(String address, Integer sessionErrorCode) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(innerConnectReported, address);
                        }
                        // propagate to callbacks now
                        NetworkNodeConnection delegate = connectionWrapper.getDelegate();
                        if (delegate != null) {
                            // let the instance know - directly
                            ((NetworkNodeBleConnectionImpl) delegate).onDisconnected();
                        } else {
                            // we have just the wrapper
                            connectionWrapper.setInjectedState(ConnectionState.CLOSED);
                        }
                        long nextConnectAttemptDelay;
                        if (sessionErrorCode != null && sessionErrorCode != ErrorCode.NO_ERROR) {
                            // we need to let the observation listener know (for sure)
                            if (directLocationObservationSetup) observationListener.onDirectLocationObservationFailed(address);
                            // set next connect attempt delay
                            switch (sessionErrorCode) {
                                case ErrorCode.BLE_CONNECTION_DROPPED:
                                    nextConnectAttemptDelay = BleConstants.RECONNECT_DELAY_ON_SUDDEN_DISCONNECT;
                                    break;
                                case ErrorCode.BLE_CONNECT_TIMEOUT:
                                case ErrorCode.GATT_DISCONNECT_TIMEOUT:
                                    nextConnectAttemptDelay = BleConstants.RECONNECT_DELAY_ON_TIMEOUT;
                                    break;
                                case ErrorCode.GATT_ASYNC:
                                case ErrorCode.GATT_OPERATION_INIT:
                                case ErrorCode.GATT_OPERATION_TIMEOUT:
                                    nextConnectAttemptDelay = BleConstants.RECONNECT_DELAY_ON_OPERATION_ERROR;
                                    break;
                                default:
                                    nextConnectAttemptDelay = BleConstants.RECONNECT_DELAY_ON_OTHER_ERROR;
                            }
                        } else {
                            nextConnectAttemptDelay = BleConstants.RECONNECT_DELAY_MS;
                        }
                        // let the queue know that the connection is finished now
                        concurrentQueue.onOperationFinished(token[0], nextConnectAttemptDelay);
                        // call the generic routine - skip state check if we have skipped on disconnecting
                        _onDisconnected(address);
                        // call the onDisconnected callback (if any) now
                        if (onDisconnectedCallback != null) onDisconnectedCallback.call(connectionWrapper, sessionErrorCode == null ? ErrorCode.NO_ERROR : sessionErrorCode);
                    }

                });
        // set up the state (to be in sync)
        connectionWrapper.setInjectedState(ConnectionState.PENDING);
        // remember the returned connection
        connections.put(address, connectionWrapper);
        ConcurrentOperationQueue.Priority opQueuePriority = asOperationQueuePriority(connectPriority);
        token[0] = concurrentQueue.operationEnqueue((opToken) -> {
            log.d("enqueueing serialized connection request for " + address);
            // we need to serialize specifically connection routine - workaround for Android BLE stack
            // otherwise we might get unnecessary application level timeouts (GattInteractionFsm)
            connectQueue.operationEnqueue((connToken) -> {
                // do the connect
                if (connectionWrapper.isDisconnectRequired()) {
                    // there was a request to discard the connection in the meantime
                    log.d("about to dispatch connection request for " + address + ", but disconnect request received in the meantime, skipping");
                    // go directly to CLOSED
                    connectionWrapper.setInjectedState(ConnectionState.CLOSED);
                    concurrentQueue.onOperationFinished(opToken, 0);
                    // let cleanup operations know
                    InterfaceHub.getHandlerHub(IhConnectionStateListener.class).onDisconnected(address, null);
                    // call the onDisconnected callback (if any)
                    if (onDisconnectedCallback != null) onDisconnectedCallback.call(connectionWrapper, null);
                    // let other connect requests pass through
                    connectQueue.onOperationFinished(connToken);
                } else {
                    // save the connect token - so that the connect listener can let the queue know when the connect routine is finished
                    connectToken[0] = connToken;
                    if (doExtraLoop[0]) {
                        // perform the connection attempt in the next UI handler loop - so that the calling
                        // code can first store the connection reference and then handle callback methods
                        ArgoApp.uiHandler.post(() -> // create the interaction -> connection wrapper
                                gattInteractionFsm.initiateConnect(new GattInteractionToConnectionWrapperCallback(connectionWrapper, onFailCallback))
                        );
                    } else {
                        // do it immediately
                        gattInteractionFsm.initiateConnect(new GattInteractionToConnectionWrapperCallback(connectionWrapper, onFailCallback));
                    }
                }
                // connect queue params
            }, opQueuePriority, null);
        // concurrent queue params
        }, opQueuePriority, address);
        // no need to perform extra loop
        doExtraLoop[0] = false;
        return connectionWrapper;
    }

    private static ConcurrentOperationQueue.Priority asOperationQueuePriority(@NotNull ConnectPriority connectPriority) {
        return ConcurrentOperationQueue.Priority.values()[connectPriority.ordinal()];
    }

    @Override
    public Boolean lastSessionSuccessful(@NonNull String bleAddress) {
        return lastSessionSuccessful.get(bleAddress);
    }

    @Override
    public void ignoreSessionErrors(String deviceAddress) {
        if (Constants.DEBUG) {
            log.d("ignoreSessionErrors: " + "deviceAddress = [" + deviceAddress + "]");
        }
        ignoreSessionErrors.add(deviceAddress);
    }

    @Override
    public Set<String> getInProgressDevices() {
        return new HashSet<>(connections.values((nnc) -> nnc.getState().inProgress)
                .map(NetworkNodeConnection::getOtherSideAddress)
                .toList());
    }

    @Override
    public void blockConnectionRequests(Action0 onBlocked) {
        connectQueue.blockProcessing(onBlocked);
    }

    @Override
    public void unblockConnectionRequests() {
        connectQueue.unblockProcessing();
    }

    @Override
    public YesNoAsync connectionRequestsBlocked() {
        return connectQueue.isProcessingBlocked();
    }

    @Override
    public void onSessionError(@NonNull String bleAddress, int errorCode) {
        if (Constants.DEBUG) {
            log.d("onSessionError: " + "bleAddress = [" + bleAddress + "], errorCode = [" + errorCode + "]");
        }
        if (isClosedOrPending(bleAddress)) {
            Preconditions.checkState(!Constants.DEBUG, "onSessionError reported while device is not connected?! FIXME");
        } else {
            inError.add(bleAddress);
        }
    }

    @Override
    public void limitLowPriorityConnections(int limit) {
        if (Constants.DEBUG) {
            Preconditions.checkState(limit <= BleConstants.MAX_CONCURRENT_CONNECTION_COUNT, "limit cannot be greater than " + BleConstants.MAX_CONCURRENT_CONNECTION_COUNT);
            log.d("limitLowPriorityConnections: " + "limit = [" + limit + "]");
        }
        if (limit == -1) {
            limit = BleConstants.MAX_CONCURRENT_CONNECTION_COUNT;
        }
        this.concurrentQueue.limitOperationExecutionByPriority(ConcurrentOperationQueue.Priority.LOW, limit);
    }

    @Override
    public boolean isClosedOrPending(@NotNull String bleAddress) {
        NetworkNodeConnection nnc = this.connections.affinityGet(bleAddress);
        ConnectionState state = nnc == null ? null : nnc.getState();
        return state == null || state.disconnected;
    }

    @Override
    public boolean allConnectionsClosed() {
        // find first non-closed connection - negate
        return !connections.values((nnc) -> !isClosed(nnc.getState())).findFirst().isPresent();
    }

    @NonNull
    @Override
    public ConnectionState getConnectionState(@NotNull String bleAddress) {
        NetworkNodeConnection nnc = connections.affinityGet(bleAddress);
        return nnc == null ? ConnectionState.CLOSED : nnc.getState();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // implementation methods
    //
    private static boolean isClosed(ConnectionState state) {
        return state == null || state == ConnectionState.CLOSED;
    }

    private void _onConnecting(@NonNull String bleAddress) {
        if (Constants.DEBUG) {
            log.d("_onConnecting: " + "bleAddress = [" + bleAddress + "]");
        }
        // reset error flag
        inError.remove(bleAddress);
        // set new state and notify via IH
        callIh(bleAddress, IhConnectionStateListener::onConnecting);
    }

    private void callIh(String bleAddress, BiConsumer<IhConnectionStateListener, String> ihFunction) {
        ihFunction.accept(InterfaceHub.getHandlerHub(IhConnectionStateListener.class), bleAddress);
    }

    private void _onConnectionInitialized(String address) {
        if (Constants.DEBUG) {
            log.d("_onConnectionInitialized: " + "address = [" + address + "]");
        }
        // set the new state and notify via IH
        callIh(address, IhConnectionStateListener::onConnected);
    }

    private void _onDisconnecting(@NotNull String bleAddress) {
        if (Constants.DEBUG)
            log.d("_onDisconnecting() called with: " + "bleAddress = [" + bleAddress + "]");
        callIh(bleAddress, IhConnectionStateListener::onDisconnecting);
    }

    private void _onDisconnected(@NonNull String bleAddress) {
        if (Constants.DEBUG) {
            log.d("_onDisconnected: " + "bleAddress = [" + bleAddress + "]");
        }
        // compute the error flag
        boolean error = false;
        if (!ignoreSessionErrors.remove(bleAddress)) {
            // we should not ignore inError flag
            error = inError.remove(bleAddress);
        }
        lastSessionSuccessful.put(bleAddress, !error);
        // set new state
        InterfaceHub.getHandlerHub(IhConnectionStateListener.class).onDisconnected(bleAddress, !error);
    }


}