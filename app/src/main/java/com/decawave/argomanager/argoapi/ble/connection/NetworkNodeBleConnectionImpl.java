/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.interaction.ProxyPosition;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.argoapi.ble.BleGattServiceRdonly;
import com.decawave.argomanager.argoapi.ble.GattInteractionCallback;
import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;
import com.decawave.argomanager.argoapi.ble.IhFirmwareUploadListener;
import com.decawave.argomanager.argoapi.ble.ReadCharacteristicRequest;
import com.decawave.argomanager.argoapi.ble.ReadCharacteristicRequestsBuilder;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.argoapi.ble.WriteCharacteristicRequest;
import com.decawave.argomanager.argoapi.ble.WriteCharacteristicRequestsBuilder;
import com.decawave.argomanager.argoapi.ble.WriteDescriptorRequest;
import com.decawave.argomanager.argoapi.ext.AnchorNodeDiffingWrapper;
import com.decawave.argomanager.argoapi.ext.Holder;
import com.decawave.argomanager.argoapi.ext.NetworkNodeDiffingWrapper;
import com.decawave.argomanager.argoapi.ext.NetworkNodePropertySetter;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.argoapi.ext.PropertySet;
import com.decawave.argomanager.argoapi.ext.TagNodeDiffingWrapper;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.decawave.argomanager.ble.ConnectionSpeed;
import com.decawave.argomanager.ble.WriteType;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogBlockStatus;
import com.decawave.argomanager.debuglog.Severity;
import com.decawave.argomanager.exception.GattRepresentationException;
import com.decawave.argomanager.util.gatt.GattDecoder;
import com.decawave.argomanager.util.gatt.GattEncoder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Network node connection implementation on top of GattInteractionFsm.
 */
class NetworkNodeBleConnectionImpl implements NetworkNodeBleConnection {
    private static final byte[] BYTE_1 = {1};
    // logging
    public static final ComponentLog log = new ComponentLog(NetworkNodeBleConnectionImpl.class);

    private static final Set<UUID> WRITE_DELAYED_CHARACTERISTICS = Sets.newHashSet(
            BleConstants.CHARACTERISTIC_OPERATION_MODE,
            BleConstants.CHARACTERISTIC_NETWORK_ID
    );

    private enum FwUploadState {
        OTHER(false),
        INITIATING(true),
        TRANSMITTING_CHUNKS(true),
        CLEANUP(false);

        public final boolean acceptingPollChanges;

        FwUploadState(boolean acceptingPollChanges) {
            this.acceptingPollChanges = acceptingPollChanges;
        }
    }
    // dependencies
    private final ApplicationComponentLog appLog;
    private final GattInteractionFsm fsm;
    private final SynchronousBleGatt synchronousBleGatt;
    private final LogBlockStatus logBlockStatus;
    private final Action1<String> onDisconnectingListener;
    private final Supplier<GattDecoder> gattDecoderSupplier;
    // constant values
    private final String bleAddress;

    // state/members
    private NodeType nodeType;
    private boolean locationDataNotificationSet;
    private boolean proxyPositionDataNotificationSet;

    // firmware update
    private boolean fwUpdatePollIndicationSet;
    private FwUploadState fwUploadState;
    private SlidingWindowDataAccessor fwBinaryDataAccessor;
    private Object fwUploadBatchTag;
    // fw update cache
    private WriteCharacteristicRequest.ByteArray fwUpdatePushRequest = new WriteCharacteristicRequest.ByteArray(
            BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_FW_UPDATE_PUSH, WriteType.NO_RESPONSE, null);
    private List<WriteCharacteristicRequest> fwUpdatePushRequestL = Collections.unmodifiableList(Lists.newArrayList(fwUpdatePushRequest));

    // current location data changed callback
    private Holder<LocationDataChangedCallback> currentLocationDataChangedCallbackWrapperHolder = new Holder<>();
    private Holder<ProxyPositionDataChangedCallback> currentProxyPositionDataChangedCallbackWrapperHolder = new Holder<>();
    private FwUpdatePollCallback currentFwUploadPollInternalCallback;
    private GattDecoder gattDecoder;
    private GattInteractionCallback gattInteractionCallback;
    // notify/listen capability
    private NetworkNodeInterceptor networkNodeSystemInterceptor;
    private NetworkNode lastNetworkNodeSnapshot;
    // current connection state
    @NotNull
    private ConnectionState state;
    // operation queue
    private SequentialGattOperationQueue gattOperationQueue;

    interface NetworkNodeInterceptor {

        void onNodeIntercepted(NetworkNode node);

        // CCCD notification routines
        void onAnchorDistancesUpdate(String nodeBleAddress, List<RangingAnchor> distances);

        void onTagLocationDataUpdate(String nodeBleAddress, LocationData locationData);

        void onProxyPositionUpdate(ProxyPosition proxyPosition);

    }

    private static NetworkNodeInterceptor VOID_INTERCEPTOR = new NetworkNodeInterceptor() {
        @Override
        public void onNodeIntercepted(NetworkNode node) {

        }

        @Override
        public void onAnchorDistancesUpdate(String nodeBleAddress, List<RangingAnchor> distances) {

        }

        @Override
        public void onTagLocationDataUpdate(String nodeBleAddress, LocationData locationData) {

        }

        @Override
        public void onProxyPositionUpdate(ProxyPosition proxyPosition) {

        }

    };

    interface FwUpdatePollCallback extends DataChangedCallback<FwPollCommand> {

    }

    NetworkNodeBleConnectionImpl(@NotNull GattInteractionFsm fsm,
                                 @NotNull SynchronousBleGatt gatt,
                                 @NotNull LogBlockStatus logBlockStatus,
                                 @NotNull Supplier<GattDecoder> gattDecoderSupplier,
                                 @NotNull Action1<String> onDisconnectingListener,
                                 @Nullable NetworkNodeInterceptor networkNodeSystemInterceptor) {
        this.fsm = fsm;
        this.synchronousBleGatt = gatt;
        this.appLog = ApplicationComponentLog.newNetworkNodeLog(log, gatt.getDeviceAddress());
        this.logBlockStatus = logBlockStatus;
        this.onDisconnectingListener = onDisconnectingListener;
        this.bleAddress = gatt.getDeviceAddress();
        this.gattDecoderSupplier = gattDecoderSupplier;
        // when this class instance gets created, it means we've got a living connection
        this.state = ConnectionState.CONNECTED;
        this.fwUploadState = FwUploadState.OTHER;
        this.networkNodeSystemInterceptor = networkNodeSystemInterceptor == null ? VOID_INTERCEPTOR : networkNodeSystemInterceptor;
        this.gattOperationQueue = new SequentialGattOperationQueueImpl(fsm);
        // we will activate the queue immediately (the connection/FSM is alive)
        this.gattOperationQueue.activate();
    }

    /**
     * @return gatt interaction callback used to pass asynchronous results of initiated operations
     */
    GattInteractionCallback asGattCallback() {
        if (gattInteractionCallback == null) {
            gattInteractionCallback = new GattInteractionCallback() {

                @Override
                public boolean stillInterested() {
                    return state != ConnectionState.CLOSED;
                }

                @Override
                public void onCharacteristicReadComplete(SynchronousBleGatt gatt) {
                    onOperationComplete(gatt);
                }

                @Override
                public void onDescriptorReadComplete(SynchronousBleGatt gatt) {
                    onOperationComplete(gatt);
                }

                @Override
                public void onDescriptorWriteComplete(SynchronousBleGatt gatt) {
                    onOperationComplete(gatt);
                }

                @Override
                public void onCharacteristicWriteComplete(SynchronousBleGatt gatt) {
                    onOperationComplete(gatt);
                }

                @Override
                public void onMtuChangeComplete(SynchronousBleGatt gatt) {
                    onOperationComplete(gatt);
                }

                @Override
                public void onFail(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    throw new IllegalStateException("general onFail() callback invocation should get passed to the upper-most callback only!");
                }

                @Override
                public void onCharacteristicReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    onOperationFailed(gatt, errorCode, failMessage);
                }

                @Override
                public void onCharacteristicWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    onOperationFailed(gatt, errorCode, failMessage);
                }

                @Override
                public void onDescriptorReadFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    onOperationFailed(gatt, errorCode, failMessage);
                }

                @Override
                public void onDescriptorWriteFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    onOperationFailed(gatt, errorCode, failMessage);
                }

                @Override
                public void onMtuChangeFailed(SynchronousBleGatt gatt, int errorCode, String failMessage) {
                    onOperationFailed(gatt, errorCode, failMessage);
                }

                @Override
                public void onCharacteristicChanged(SynchronousBleGatt gatt, BleGattCharacteristic characteristic, byte[] value) {
                    // delegate to current location data changed callback (this operation is actually not initialized)
                    NetworkNodeBleConnectionImpl.this.onCharacteristicChanged(characteristic, value);
                }

            };
        }
        return gattInteractionCallback;
    }

    // we might have missed the disconnect initiative - when the low-level API decides to close the connection/disconnects suddenly
    void onDisconnecting() {
        if (Constants.DEBUG) {
            log.d("onDisconnecting() state = " + state);
        }
        // the lower level layer
        if (state != ConnectionState.DISCONNECTING) {
            state = ConnectionState.DISCONNECTING;
            // deactivate the operation queue
            gattOperationQueue.deactivate();
            // presuming the low-level API is already in the process of disconnect, there is nothing more to do
        }
    }


    // notification from the low-level API
    void onDisconnected() {
        // deactivate the operation queue (it may have been already deactivated)
        gattOperationQueue.deactivate();
        // enable debug logs for sure
        logBlockStatus.unblockDeviceLog(bleAddress);
        // set the state
        state = ConnectionState.CLOSED;
    }

    @Override
    public String getOtherSideAddress() {
        return bleAddress;
    }

    @Override
    public boolean isConnected() {
        // the network node connection translates the FSM connection state to a different semantics
        // for NNC, we are connected, once we reach the services discovered state
        return NetworkNodeConnectionWrapper.asConnected(state);
    }

    @NonNull
    @Override
    public ConnectionState getState() {
        return state;
    }

    @Override
    public void disconnect() {
        if (Constants.DEBUG) {
            log.d("disconnect() [" + bleAddress + "]");
        }
        // set up the state
        state = ConnectionState.DISCONNECTING;
        // call the listener (so that the connection API knows that we are disconnecting)
        onDisconnectingListener.call(bleAddress);
        // TODO: maybe it makes sense to enforce IDLE state of the underlying FSM here
        SequentialGattOperationQueue.Token token = null;
        if (isObservingLocationData() == YesNoAsync.YES) {
            token = _stopObserveLocationData();
        } else if (isObservingProxyPositionData() == YesNoAsync.YES) {
            token = _stopObserveProxyPositionData();
        }
        // initiate write of CHARACTERISTIC_DISCONNECT
        WriteCharacteristicRequestsBuilder builder = new WriteCharacteristicRequestsBuilder();
        builder.setService(BleConstants.SERVICE_UUID_NETWORK_NODE).addValue(BleConstants.CHARACTERISTIC_DISCONNECT, BYTE_1);
        // in each case perform the final action - hardDisconnect
        writeCharacteristics(builder.build(), writeEffect -> hardDisconnect(), fail -> {
            // log the fail
            appLog.we("unable to properly disconnect", fail);
            // do the hard disconnect
            hardDisconnect();
        }, token);
        // we cannot deactivate the gatt operation queue now, because we would not receive write result/callback
        // we have to deactivate the queue in the callback
    }

    private void hardDisconnect() {
        if (Constants.DEBUG) {
            log.d("hardDisconnect() [" + bleAddress + "]");
        }
        // deactivate queue processing - so that any other possibly enqueued tasks are not executed
        gattOperationQueue.deactivate();
        // do the low-level disconnect
        fsm.initiateDisconnect();
    }

    @Override
    public void setDisconnectOnProblem(boolean doDisconnect) {
        fsm.setDisconnectOnProblem(doDisconnect);
    }

    @Override
    public void getOtherSideEntity(@NonNull Action1<NetworkNode> onSuccess,
                                   @NonNull Action1<Fail> onFail,
                                   NetworkNodeProperty... properties) {
        if (Constants.DEBUG) {
            Preconditions.checkState(!fwUpdatePollIndicationSet, "cannot get other side entity while uploading firmware");
        }
        Set<ReadCharacteristicRequest> readRequests;
        if (this.nodeType == null) {
            if (!decodeAndSaveNodeType(onFail, true)) {
                // GATT representation problem
                return;
            }
        }
        if (properties != null && properties.length > 0) {
            // we want to fetch just specific properties
            ReadCharacteristicRequestsBuilder builder = new ReadCharacteristicRequestsBuilder();
            if (nodeType == null) {
                // make sure that nodetype is known
                builder.addProperty(NetworkNodeProperty.NODE_TYPE);
            }
            // now add all other properties
            for (NetworkNodeProperty property : properties) {
                builder.addProperty(property);
            }
            fireGetOtherSideEntityReadRequests(onSuccess, onFail, builder.build());  // no dependency
                // if the operation mode is needed it is added directly to readRequests
        } else {
            // we want to fetch complete set of properties
            if (nodeType == null) {
                // we need to fetch operation mode first - initial fetch
                ReadCharacteristicOperation.enqueue(gattOperationQueue,
                        new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_OPERATION_MODE),
                        (synchronousBleGatt1 -> {
                            // onSuccess
                            if (decodeAndSaveNodeType(onFail, false)) {
                                fireGetFullOtherSideEntity(onSuccess, onFail);
                            }
                        }),
                        (synchronousBleGatt1, fail) -> callFailConditionalDisconnect(onFail, fail),
                        getGattDecoder().getContext(),
                        null    // no dependency
                        );
            } else {
                fireGetFullOtherSideEntity(onSuccess, onFail);
            }
        }
    }

    private boolean decodeAndSaveNodeType(@NonNull Action1<Fail> onFail, boolean acceptNullNodeType) {
        try {
            GattDecoder.GattOperationMode opMode = getGattDecoder().getOperationMode(synchronousBleGatt);
            if (!acceptNullNodeType && (opMode == null || opMode.nodeType == null)) {
                throw new GattRepresentationException(bleAddress, "there is missing operation mode/node type value after explicit fetch");
            }
            nodeType = opMode != null ? opMode.nodeType : null;
            return true;
        } catch (GattRepresentationException exc) {
            handleGattRepresentationException(onFail, exc);
        }
        return false;
    }

    private void fireGetFullOtherSideEntity(@NonNull Action1<NetworkNode> onSuccess,
                                            @NonNull Action1<Fail> onFail) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(nodeType, "nodeType must NOT be null!");
        }
        // and this synchronous operation fires property fetch based on operation mode/node type
        fireGetOtherSideEntityReadRequests(onSuccess, onFail, getRequests(nodeType, lastNetworkNodeSnapshot == null));
    }

    private void fireGetOtherSideEntityReadRequests(Action1<NetworkNode> onSuccess,
                                                    Action1<Fail> onFail,
                                                    Set<ReadCharacteristicRequest> readRequests) {
        if (!readRequests.isEmpty()) {
            ReadCharacteristicOperation.enqueue(gattOperationQueue,
                    new ArrayList<>(readRequests),
                    synchronousBleGatt1 -> {
                        try {
                            // construct node with diffing ability
                            NetworkNode node = getGattDecoder().decode(synchronousBleGatt, readRequests);
                            NetworkNode diffNode = NodeFactory.newDiffingWrapper(node);
                            onNewEntityProperties(diffNode);
                            // now call the success callback
                            onSuccess.call(diffNode);
                        } catch (GattRepresentationException exc) {
                            handleGattRepresentationException(onFail, exc);
                        }
                    },
                    (synchronousBleGatt1, fail) -> callFailConditionalDisconnect(onFail, fail),
                    getGattDecoder().getContext(),
                    null
            );
        }
    }

    private void handleGattRepresentationException(Action1<Fail> onFail,
                                                   GattRepresentationException exc) {
        appLog.we(exc.getMessage(), ErrorCode.GATT_REPRESENTATION, exc);
        callFailConditionalDisconnect(onFail, new Fail(ErrorCode.GATT_REPRESENTATION, exc.getMessage()));
    }

    private void callFailConditionalDisconnect(Action1<Fail> onFail, Fail failReason) {
        callFailConditionalDisconnect(onFail, failReason, null);
    }

    private void callFailConditionalDisconnect(Action1<Fail> onFail, Fail failReason, Action0 onConnectionKeptAction) {
        onFail.call(failReason);
        if (fsm.doDisconnectOnProblem()) {
            if (failReason.errorCode == ErrorCode.GATT_BROKEN
                    || failReason.errorCode == ErrorCode.GATT_MISSING_DESCRIPTOR
                    || failReason.errorCode == ErrorCode.GATT_MISSING_CHARACTERISTIC) {
                hardDisconnect();
            } else {
                // make sure that the disconnect request will be accepted
                fsm.makeSureIdle();
                // do a clean disconnect (with WRITE to disconnect characteristic)
                disconnect();
            }
        } else {
            if (onConnectionKeptAction != null) onConnectionKeptAction.call();
        }
    }

    private void onNewEntityProperties(@NotNull NetworkNode otherSideEntity) {
        if (nodeType == null) {
            nodeType = otherSideEntity.getType();
        } else if (Constants.DEBUG) {
            Preconditions.checkState(nodeType == otherSideEntity.getType(), "nodeType and other side entity type do not match! " + otherSideEntity);
        }
        if (lastNetworkNodeSnapshot == null || otherSideEntity.getType() != lastNetworkNodeSnapshot.getType()) {
            // different type or the first snapshot received
            lastNetworkNodeSnapshot = otherSideEntity;
        } else {
            // types are the same, copy the non-null properties to our snapshot
            lastNetworkNodeSnapshot.copyFrom(otherSideEntity);
        }
        // notify the listener
        networkNodeSystemInterceptor.onNodeIntercepted(lastNetworkNodeSnapshot);
    }

    private void onNewProxyPositions(@NotNull List<ProxyPosition> proxyPositions) {
        for (ProxyPosition proxyPosition : proxyPositions) {
            networkNodeSystemInterceptor.onProxyPositionUpdate(proxyPosition);
        }
    }

    private void onNewLocationData(@NotNull LocationData locationData) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(nodeType, "node type is not known");
        }
        // it depends on node type
        if (nodeType == NodeType.TAG) {
            if (!locationData.isEmpty()) {
                if (lastNetworkNodeSnapshot != null) {
                    ((NetworkNodePropertySetter) lastNetworkNodeSnapshot).setProperty(NetworkNodeProperty.TAG_LOCATION_DATA, locationData);
                }
                networkNodeSystemInterceptor.onTagLocationDataUpdate(bleAddress, locationData);
            }
        } else {
            // we are ANCHOR, we will easily ignore position compound of the location data
            // and consider only distances
            if (locationData.distances != null) {
                if (lastNetworkNodeSnapshot != null) {
                    ((NetworkNodePropertySetter) lastNetworkNodeSnapshot).setProperty(NetworkNodeProperty.ANCHOR_DISTANCES, locationData.distances);
                }
                networkNodeSystemInterceptor.onAnchorDistancesUpdate(bleAddress, locationData.distances);
            }
        }
    }

    @Override
    public void updateOtherSideEntity(NetworkNode networkNode, boolean nodeTypeChanged, final Action1<WriteEffect> onSuccess, Action1<Fail> onFail) {
        if (Constants.DEBUG) {
            Preconditions.checkState(!fwUpdatePollIndicationSet, "cannot get other side entity while uploading firmware");
            Preconditions.checkArgument(networkNode != null, "networkNode");
            Preconditions.checkArgument(onSuccess != null, "onSuccess");
            Preconditions.checkArgument(onFail != null, "onFail");
        }
        // convert network node to write request list
        final List<WriteCharacteristicRequest> writeReq = convertNetworkNodeToWriteRequests(networkNode, nodeTypeChanged);
        if (writeReq.isEmpty()) {
            appLog.d("no change detected, disconnecting");
            onSuccess.call(WriteEffect.WRITE_SKIPPED);
        } else {
            writeCharacteristics(writeReq, onSuccess, onFail, null);
        }
    }

    private void writeCharacteristics(List<WriteCharacteristicRequest> writeReq,
                                      Action1<WriteEffect> onSuccess, Action1<Fail> onFail,
                                      SequentialGattOperationQueue.Token dependsOn) {
        if (Constants.DEBUG) {
            Preconditions.checkState(!writeReq.isEmpty(), "write request list empty?");
        }
        // check that we have loaded (non-null) properties which we are going to write
        WriteEffect writeEffect = WriteEffect.WRITE_IMMEDIATE_EFFECT;
        Set<ReadCharacteristicRequest> readReq = new LinkedHashSet<>();
        for (WriteCharacteristicRequest writeCharacteristicRequest : writeReq) {
            // determine write type
            if (writeEffect == WriteEffect.WRITE_IMMEDIATE_EFFECT && WRITE_DELAYED_CHARACTERISTICS.contains(writeCharacteristicRequest.characteristicUuid)) {
                writeEffect = WriteEffect.WRITE_DELAYED_EFFECT;
            }
            // now determine what needs to be read
            BleGattServiceRdonly service = synchronousBleGatt.getService(writeCharacteristicRequest.serviceUuid);
            if (service == null) {
                callFailConditionalDisconnect(onFail, new Fail(ErrorCode.GATT_BROKEN, "BLE service " + writeCharacteristicRequest.serviceUuid + " is missing, despite that it was there few moments ago"));
                return;
            }
            BleGattCharacteristic characteristic = service.getCharacteristic(writeCharacteristicRequest.characteristicUuid);
            // check that the characteristic is present on our cached bleGatt representation/characteristic
            if (characteristic == null) {
                callFailConditionalDisconnect(onFail, new Fail(ErrorCode.GATT_MISSING_CHARACTERISTIC,
                        "missing characteristic '" + BleConstants.MAP_CHARACTERISTIC_TITLE.get(writeCharacteristicRequest.characteristicUuid)
                        + "' for service " + writeCharacteristicRequest.serviceUuid));
                return;
            }
            UUID chUuid = characteristic.getUuid();
            if (!characteristic.valueLoaded() && !chUuid.equals(BleConstants.CHARACTERISTIC_PERSISTED_POSITION) && !chUuid.equals(BleConstants.CHARACTERISTIC_DISCONNECT)) {
                // this characteristic is missing value (and it makes sense to read it)
                readReq.add(new ReadCharacteristicRequest(writeCharacteristicRequest.serviceUuid, writeCharacteristicRequest.characteristicUuid));
            }
        }
        final WriteEffect fWriteEffect = writeEffect;
        // fill the operation queue now
        SequentialGattOperationQueue.Token dep;
        if (!readReq.isEmpty()) {
            // we must initiate a read request first, then we can initiate the write request
            appLog.d("initiating READ request of TO-BE-WRITTEN properties first");
            dep = ReadCharacteristicOperation.enqueue(gattOperationQueue, new ArrayList<>(readReq),
                    null,   // success, the next operation in the queue does the WRITE and notifies the response callback
                    (gatt, fail) -> {
                        // pass directly to the passed app-level callback
                        callFailConditionalDisconnect(onFail, fail);
            }, getGattDecoder().getContext(), dependsOn);
        } else {
            dep = dependsOn;
        }
        WriteCharacteristicOperation.enqueue(gattOperationQueue, writeReq,
                // notify the final callback about success/failingTask
                gatt1 -> onSuccess.call(fWriteEffect),
                (gatt1, fail) -> callFailConditionalDisconnect(onFail, fail),
                dep);
    }

    @Override
    public void changeMtu(int mtu, @Nullable Action0 onSuccess, @Nullable Action1<Fail> onFailCallback) {
        if (Constants.DEBUG) {
            Preconditions.checkState(mtu >= 23, "MTU must be >= 23, given mtu: " + mtu);
        }
        Integer systemMtu = fsm.getLastNegotiatedSystemMtu();
        if (systemMtu == null || ChangeMtuOperation.getSystemMtu(mtu) != systemMtu) {
            ChangeMtuOperation.enqueue(gattOperationQueue, mtu, (gatt) -> {
                if (onSuccess != null ) onSuccess.call();
            }, onFailCallback);
        } else {
            // the MTU is already configured properly
            if (onSuccess != null ) onSuccess.call();
        }
    }

    @Override
    public void uploadFirmware(FirmwareMeta firmwareMeta, InputStream firmwareData, Action0 onSuccessCallback, Action1<Integer> progressListener, Action1<Fail> onFailCallback) {
        appLog.d("initiating uploadFirmware: " + firmwareMeta);
        if (Constants.DEBUG) {
            Preconditions.checkState(fwUploadState == FwUploadState.OTHER, "current FW upload state = " + fwUploadState);
        }
        setFwUploadState(FwUploadState.INITIATING);
        // change connection priority - we do not have callback for this...
        boolean b = fsm.changeConnectionSpeed(ConnectionSpeed.HIGH);
        if (!b) {
            appLog.we("connection priority could not be changed", ErrorCode.UNSPECIFIC);
        }
        // check if we need to negotiate different MTU
        Integer systemMtu = fsm.getLastNegotiatedSystemMtu();
        SequentialGattOperationQueue.Token mtuOpToken = null;
        if (systemMtu == null || systemMtu < ChangeMtuOperation.getSystemMtu(BleConstants.MTU_ON_FW_UPLOAD)) {
            mtuOpToken = ChangeMtuOperation.enqueue(gattOperationQueue, BleConstants.MTU_ON_FW_UPLOAD, onFailCallback);
        }
        // and then enqueue synchronous operation
        SynchronousOperation.enqueue(gattOperationQueue, (fsm) -> {
            // first setup a notification
            // set up the callback
            currentFwUploadPollInternalCallback = new FwUpdatePollCallback() {

                @Override
                public void onStarted() {
                    fwUpdatePollIndicationSet = true;
                    // successfully set up, send firmware meta data
                    offerNewFirmware(firmwareMeta);
                }

                @Override
                public void onStopped() {
                    if (Constants.DEBUG) {
                        Preconditions.checkState(fwUploadState == FwUploadState.CLEANUP, "current FW upload state = " + fwUploadState);
                    }
                    fwUpdatePollIndicationSet = false;
                    appLog.d("stopped FW poll command indication");
                    finalActions();
                    // notify the callback
                    if (onSuccessCallback != null) onSuccessCallback.call();
                }

                @Override
                public void onChange(FwPollCommand fwPollCommand) {
                    appLog.i("incoming FW update poll command: " + fwPollCommand);
                    // dispatch the command
                    fwPollCommand.accept(fwCommandDispatcher);
                }

                @Override
                public void onFail(Fail fail) {
                    // log
                    appLog.we(fail.message, fail.errorCode);
                    finalActions();
                    // delegate to application-level callback
                    callFailConditionalDisconnect((aFail) -> { if (onFailCallback != null) onFailCallback.call(aFail); }, fail);
                }

                // processing commands from FW update poll characteristic
                PollPayloadVisitor fwCommandDispatcher = new PollPayloadVisitor() {

                    @Override
                    public void visit(FwPollCommand.UploadRefused uploadRefused) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(fwUploadState == FwUploadState.INITIATING, "current FW upload state = " + fwUploadState);
                        }
                        setFwUploadState(FwUploadState.OTHER);
                        onFail(new Fail(ErrorCode.FAILED_FIRMWARE_UPLOAD_BASE + uploadRefused.errorCode,
                                "refused firmware upload: ec = " + uploadRefused.errorCode));
                    }

                    @Override
                    public void visit(FwPollCommand.BufferRequest bufferRequest) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(fwUploadState != FwUploadState.OTHER, "current FW upload state = " + fwUploadState);
                        }
                        if (fwUploadState == FwUploadState.CLEANUP) {
                            // ignore
                            return;
                        }
                        if (fwUploadState == FwUploadState.INITIATING) {
                            // configure the accessor
                            fwBinaryDataAccessor = new InputStreamSlidingWindowDataAccessor(firmwareData);
                            fwBinaryDataAccessor.setChunkSize(BleConstants.FW_UPLOAD_CHUNK_SIZE);
                            fwUploadState = FwUploadState.TRANSMITTING_CHUNKS;
                            appLog.imp("suppressing DEBUG logs - there would be too much of output");
                            logBlockStatus.blockDeviceLog(bleAddress, Severity.DEBUG);
                            fsm.setDebugLoggingEnabled(false);
                        }
                        if (Constants.DEBUG) {
                            Preconditions.checkState(fwUploadState == FwUploadState.INITIATING
                                    || fwUploadState == FwUploadState.TRANSMITTING_CHUNKS, "invalid state = " + fwUploadState);
                        }
                        fwBinaryDataAccessor.setWindow(bufferRequest.offset, bufferRequest.size);
                        fwUploadBatchTag = new Object();
                        appLog.i("sending first chunk at offset " + fwBinaryDataAccessor.getCurrentPosition());
                        // let the progress listener know
                        if (progressListener != null) progressListener.call(fwBinaryDataAccessor.getCurrentPosition());
                        fwUploadSendNextChunk(fwUploadBatchTag);
                    }

                    @Override
                    public void visit(FwPollCommand.UploadComplete uploadComplete) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(fwUploadState == FwUploadState.TRANSMITTING_CHUNKS,
                                    "current FW upload state = " + fwUploadState);
                        }
                        // let the progress listener know
                        if (progressListener != null) progressListener.call(fwBinaryDataAccessor.getCurrentPosition());
                        // cancel indication
                        fwBinaryDataAccessor = null;
                        setFwUploadState(FwUploadState.CLEANUP);
                        setCccdFwUpdatePollIndication(false);
                    }

                    @Override
                    public void visit(FwPollCommand.SaveFailed saveFailed) {
                        if (Constants.DEBUG) {
                            Preconditions.checkState(fwUploadState == FwUploadState.TRANSMITTING_CHUNKS,
                                    "current FW upload state = " + fwUploadState);
                        }
                        onFail(new Fail(ErrorCode.FAILED_FIRMWARE_UPLOAD_BASE + saveFailed.errorCode, "cannot save image to flash memory"));
                    }
                };

                private void finalActions() {
                    if (logBlockStatus.isDeviceLogBlocked(bleAddress)) {
                        // unblock DEBUG logging
                        appLog.imp("enabling debug logs");
                        logBlockStatus.unblockDeviceLog(bleAddress);
                        fsm.setDebugLoggingEnabled(true);
                    }
                    // close the stream
                    try {
                        firmwareData.close();
                    } catch (IOException e) {
                        appLog.we("cannot close firmware stream: " + e.getMessage(), ErrorCode.STREAM_CLOSE_ERROR, e);
                    }
                    // clear the inner helper callback
                    currentFwUploadPollInternalCallback = null;
                    setFwUploadState(FwUploadState.OTHER);
                    fsm.changeConnectionSpeed(ConnectionSpeed.BALANCED);
                }
            };
            // we will setup the indication on FW upload poll characteristic
            return setCccdFwUpdatePollIndication(true) != null;
        }, mtuOpToken);
    }

    private void setFwUploadState(FwUploadState state) {
        if (state != this.fwUploadState) {
            this.fwUploadState = state;
            switch (state) {
                case OTHER:
                    InterfaceHub.getHandlerHub(IhFirmwareUploadListener.class).onFinished(bleAddress);
                    break;
                case INITIATING:
                    InterfaceHub.getHandlerHub(IhFirmwareUploadListener.class).onInitiating(bleAddress);
                    break;
                case TRANSMITTING_CHUNKS:
                    InterfaceHub.getHandlerHub(IhFirmwareUploadListener.class).onUploading(bleAddress);
                    break;
                case CLEANUP:
                    InterfaceHub.getHandlerHub(IhFirmwareUploadListener.class).onCleaningUp(bleAddress);
                    break;
                default:
                    throw new IllegalStateException("unexpected state: " + state);
            }
        }
    }


    private void fwUploadSendNextChunk(Object batchTag) {
        if (Constants.DEBUG) {
            Preconditions.checkState(fwUploadState == FwUploadState.TRANSMITTING_CHUNKS);
        }
        //
        int offset = fwBinaryDataAccessor.getCurrentPosition();
        byte[] chunk = fwBinaryDataAccessor.nextChunk();
        if (chunk == null) {
            appLog.i("no more chunks, current offset " + offset);
        } else if (chunk.length == 0) {
            appLog.i("no more chunks, we have reached FW EOF, current offset " + offset);
        } else {
            // check if there wasn't a DISCONNECT request in the meantime
            if (fsm.isTerminate()) {
                // do not bother anymore
                return;
            }
            // send the chunk
            fwUpdatePushRequest.setValue(GattEncoder.encodeFwChunk(offset, chunk));
            WriteCharacteristicOperation.enqueue(gattOperationQueue, fwUpdatePushRequestL,
                    synchronousBleGatt1 -> {
                        if (batchTag == fwUploadBatchTag && fwUploadState == FwUploadState.TRANSMITTING_CHUNKS) {
                            // continue, there hasn't been any other request
                            fwUploadSendNextChunk(batchTag);
                        } else {
                            log.d("terminating send loop, state = " + fwUploadState);
                        }
                    },
                    null, null);
        }
    }

    private void offerNewFirmware(FirmwareMeta firmwareMeta) {
        if (Constants.DEBUG) {
            Preconditions.checkState(fwUploadState == FwUploadState.INITIATING);
        }
        appLog.d("offering firmware " + firmwareMeta);
        WriteCharacteristicRequestsBuilder builder = new WriteCharacteristicRequestsBuilder();
        builder.setService(BleConstants.SERVICE_UUID_NETWORK_NODE);
        //
        builder.addValue(BleConstants.CHARACTERISTIC_FW_UPDATE_PUSH, GattEncoder.encodeUpdateFirmwareOffer(firmwareMeta));
        // enqueue write to firmware push
        WriteCharacteristicOperation.enqueue(gattOperationQueue, builder.build(), ignore-> {}, (gatt, fail) -> {}, null);
        // we will receive real response asynchronously via FW poll characteristic
    }

    private SequentialGattOperationQueue.Token setCccdFwUpdatePollIndication(boolean enable) {
        return genericSetCccdNotification(BleConstants.CHARACTERISTIC_FW_UPDATE_POLL, enable, currentFwUploadPollInternalCallback, null);
    }

    private SequentialGattOperationQueue.Token genericSetCccdNotification(UUID characteristicUuid,
                                                                          boolean enable,
                                                                          DataChangedCallback<?> dataChangedCallback,
                                                                          SequentialGattOperationQueue.Token dependsOn) {
        BleGattServiceRdonly service = synchronousBleGatt.getService(BleConstants.SERVICE_UUID_NETWORK_NODE);
        if (service == null) {
            dataChangedCallback.onFail(new Fail(ErrorCode.GATT_BROKEN, "network node service is missing, despite that it was there few moments ago"));
            return null;
        }
        BleGattCharacteristic charsic = service.getCharacteristic(characteristicUuid);
        String chLabel = BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristicUuid);
        // check that the characteristic is not missing
        if (charsic == null) {
            dataChangedCallback.onFail(new Fail(ErrorCode.GATT_MISSING_CHARACTERISTIC, "characteristic " + chLabel + " is missing"));
            return null;
        }
        // check that the CCCD is present
        BleGattDescriptor descriptor = charsic.getDescriptor(BleConstants.DESCRIPTOR_CCC);
        if (descriptor == null) {
            dataChangedCallback.onFail(new Fail(ErrorCode.GATT_MISSING_DESCRIPTOR, "CCC descriptor missing on "
                    + chLabel + " characteristic!"));
            return null;
        }
        // 1. enqueue synchronous call - set up the notification
        SequentialGattOperationQueue.Token opSetChrNotificationToken = SynchronousOperation.enqueue(gattOperationQueue, (giFsm) -> {
            if (!synchronousBleGatt.setCharacteristicNotification(charsic, enable)) {
                // this is failingTask
                dataChangedCallback.onFail(new Fail(ErrorCode.GATT_OPERATION_INIT, "cannot perform setCharacteristicNotification(" + enable + ") on " + chLabel));
                //
                return false;
            } // else: success
            return true;
        }, dependsOn);
        List<WriteDescriptorRequest> writeDescriptorRequests = new ArrayList<>(1);
        writeDescriptorRequests.add(new WriteDescriptorRequest(BleConstants.SERVICE_UUID_NETWORK_NODE,
                characteristicUuid,
                BleConstants.DESCRIPTOR_CCC,
                enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
        // 2. enqueue descriptor write and wait for the callback to notify us about result
        return WriteDescriptorOperation.enqueue(gattOperationQueue, writeDescriptorRequests,
                synchronousBleGatt1 -> {
                    appLog.d(chLabel + " notification successfully "
                            + (enable ? "enabled" : "disabled"));
                    if (enable) {
                        dataChangedCallback.onStarted();
                    } else {
                        dataChangedCallback.onStopped();
                    }
                },
                (synchronousBleGatt1, fail) -> dataChangedCallback.onFail(fail),
                opSetChrNotificationToken
        );
    }

    @Override
    public void observeLocationData(LocationDataChangedCallback locationDataChangedCallback) {
        genericObserveData(locationDataChangedCallback, currentLocationDataChangedCallbackWrapperHolder, (delegate) ->
                        new LocationDataChangedCallbackWrapper(delegate) {

                            @Override
                            public void onStarted() {
                                locationDataNotificationSet = true;
                                super.onStarted();
                            }

                            @Override
                            public void onStopped() {
                                locationDataNotificationSet = false;
                                super.onStopped();
                            }

                            @Override
                            public void onFail(Fail fail) {
                                locationDataNotificationSet = false;
                                callFailConditionalDisconnect(super::onFail, fail);
                            }

                        },
                currentProxyPositionDataChangedCallbackWrapperHolder.value, BleConstants.MTU_ON_LOCATION_DATA_OBSERVE, BleConstants.CHARACTERISTIC_LOCATION_DATA
        );
    }

    @Override
    public void stopObserveLocationData() {
        _stopObserveLocationData();
    }

    private SequentialGattOperationQueue.Token _stopObserveLocationData() {
        return genericStopObserveData(BleConstants.CHARACTERISTIC_LOCATION_DATA, locationDataNotificationSet, currentLocationDataChangedCallbackWrapperHolder);
    }


    @Override
    public YesNoAsync isObservingLocationData() {
        return genericIsObservingData(locationDataNotificationSet, currentLocationDataChangedCallbackWrapperHolder.value);
    }

    @Override
    public void observeProxyPositionData(ProxyPositionDataChangedCallback proxyPositionDataChangedCallback) {
        // change connection priority - we do not have callback for this...
        boolean b = fsm.changeConnectionSpeed(ConnectionSpeed.HIGH);
        if (!b) {
            appLog.we("connection priority could not be changed", ErrorCode.UNSPECIFIC);
        }
        // now start the observation
        genericObserveData(proxyPositionDataChangedCallback, currentProxyPositionDataChangedCallbackWrapperHolder, (delegate) ->
                        new ProxyPositionDataChangedCallbackWrapper(delegate) {
                            @Override
                                public void onStarted() {
                                proxyPositionDataNotificationSet = true;
                                super.onStarted();
                            }

                            @Override
                            public void onStopped() {
                                proxyPositionDataNotificationSet = false;
                                // revert the connection priority to BALANCED
                                fsm.changeConnectionSpeed(ConnectionSpeed.BALANCED);
                                // notify the callback
                                super.onStopped();
                            }

                            @Override
                            public void onFail(Fail fail) {
                                proxyPositionDataNotificationSet = false;
                                // check if we should disconnect
                                callFailConditionalDisconnect(super::onFail, fail, () -> fsm.changeConnectionSpeed(ConnectionSpeed.BALANCED));
                                    // revert the connection priority to BALANCED if the connection is kept
                            }

                        },
                            currentLocationDataChangedCallbackWrapperHolder.value, BleConstants.MTU_ON_PROXY_POSITION_DATA_OBSERVE, BleConstants.CHARACTERISTIC_PROXY_POSITIONS
        );
    }

    private <T extends DataChangedCallback> void genericObserveData(final T dataChangedCallback,
                                                                    Holder<T> callbackHolder,
                                                                    Function<T, T> callbackWrapperSupplier,
                                                                    DataChangedCallback otherCallback,
                                                                    int requiredMtu, UUID observedCharacteristic) {
        if (Constants.DEBUG) {
            Preconditions.checkState(callbackHolder.value == null, "there is already set a callback!");
            Preconditions.checkState(otherCallback == null, "there is ongoing other-data observation!");
            Preconditions.checkState(fwUploadState == FwUploadState.OTHER, "cannot observe data while uploading firmware!, state = " + fwUploadState);
        }
        // wrap the location data changed callback
        callbackHolder.value = callbackWrapperSupplier.apply(dataChangedCallback);
        // check that there is sufficient MTU
        Integer systemMtu = fsm.getLastNegotiatedSystemMtu();
        SequentialGattOperationQueue.Token token = null;
        if (systemMtu == null || systemMtu < ChangeMtuOperation.getSystemMtu(requiredMtu)) {
            token = ChangeMtuOperation.enqueue(gattOperationQueue, requiredMtu, dataChangedCallback::onFail);
        }
        // callback change always goes first, then it is asynchronously confirmed by locationDataNotificationSet
        genericSetCccdNotification(observedCharacteristic, true, callbackHolder.value, token);
    }

    @Override
    public void stopObserveProxyPositionData() {
        _stopObserveProxyPositionData();
    }

    private SequentialGattOperationQueue.Token _stopObserveProxyPositionData() {
        return genericStopObserveData(BleConstants.CHARACTERISTIC_PROXY_POSITIONS, proxyPositionDataNotificationSet, currentProxyPositionDataChangedCallbackWrapperHolder);
    }

    @Override
    public YesNoAsync isObservingProxyPositionData() {
        return genericIsObservingData(proxyPositionDataNotificationSet, currentProxyPositionDataChangedCallbackWrapperHolder.value);
    }

    private SequentialGattOperationQueue.Token genericStopObserveData(UUID characteristicUuid,
                                                                      boolean notificationSetFlag,
                                                                      Holder<? extends DataChangedCallback> callbackHolder) {
        if (Constants.DEBUG) {
            Preconditions.checkState(notificationSetFlag, "data observation not set up");
            Preconditions.checkState(callbackHolder.value != null, "there is not set a callback!");
        }
        // callback change always goes first, then it is asynchronously confirmed by locationDataNotificationSet
        DataChangedCallback callback = callbackHolder.value;
        callbackHolder.value = null;
        return genericSetCccdNotification(characteristicUuid, false, callback, null);
    }

    @NonNull
    private YesNoAsync genericIsObservingData(boolean notificationSetFlag,
                                              DataChangedCallback callback) {
        if (!notificationSetFlag) {
            // notification is not set
            if (callback != null) {
                return YesNoAsync.TO_YES;
            } else {
                return YesNoAsync.NO;
            }
        } else {
            // notification is set
            if (callback != null) {
                return YesNoAsync.YES;
            } else {
                return YesNoAsync.TO_NO;
            }
        }
    }

    @Override
    public YesNoAsync isUploadingFirmware() {
        switch (fwUploadState) {
            case OTHER:
                return YesNoAsync.NO;
            case INITIATING:
                return YesNoAsync.TO_YES;
            case TRANSMITTING_CHUNKS:
                return YesNoAsync.YES;
            case CLEANUP:
                return YesNoAsync.TO_NO;
            default:
                throw new IllegalStateException("fwUploadState = " + fwUploadState);
        }
    }

    @Override
    public boolean isDisconnected() {
        if (Constants.DEBUG) {
            // state should never be PENDING or CONNECTING
            Preconditions.checkState(state != ConnectionState.PENDING);
            Preconditions.checkState(state != ConnectionState.CONNECTING);
        }
        return state.disconnected;
    }

    @Override
    public void setConnectionSpeed(@NonNull ConnectionSpeed connectionSpeed) {
        if (Constants.DEBUG) {
            Preconditions.checkState(getState() == ConnectionState.CONNECTED, "cannot change speed in " + getState());
            Preconditions.checkNotNull(connectionSpeed);
        }
        fsm.changeConnectionSpeed(connectionSpeed);
    }


    private GattDecoder getGattDecoder() {
        if (gattDecoder == null) {
            gattDecoder = gattDecoderSupplier.get();
        }
        return gattDecoder;
    }

    void onCharacteristicChanged(BleGattCharacteristic characteristic, byte[] value) {
        UUID charsicUuid = characteristic.getUuid();
        if (charsicUuid.equals(BleConstants.CHARACTERISTIC_FW_UPDATE_POLL)) {
            onFwUpdatePollCharacteristicChange(value);
        } else if (charsicUuid.equals(BleConstants.CHARACTERISTIC_LOCATION_DATA)) {
            onLocationDataCharacteristicChange(value);
        } else if (charsicUuid.equals(BleConstants.CHARACTERISTIC_PROXY_POSITIONS)) {
            onProxyPositionDataCharacteristicChange(value);
        } else {
            Preconditions.checkState(Constants.DEBUG,
                    "unexpected characteristic change: " + BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristic.getUuid()));
        }

    }

    private void onLocationDataCharacteristicChange(byte[] value) {
        // location data
        if (currentLocationDataChangedCallbackWrapperHolder.value == null) {
            // it might happen that we have reset the location data change callback while change was being processed
            appLog.we("got onCharacteristicChanged() while currentLocationDataChangedCallbackWrapperHolder is null, ignoring", ErrorCode.GATT_OBSOLETE_CALLBACK);
        } else {
            try {
                LocationData locationData = getGattDecoder().decodeLocationData(value);
                if (locationData != null) {
                    onNewLocationData(locationData);
                    currentLocationDataChangedCallbackWrapperHolder.value.onChange(locationData);
                }
            } catch (GattRepresentationException e) {
                // report the error
                handleGattRepresentationException(currentLocationDataChangedCallbackWrapperHolder.value::onFail, e);
            }
        }
    }

    private void onProxyPositionDataCharacteristicChange(byte[] value) {
        // location data
        if (currentProxyPositionDataChangedCallbackWrapperHolder.value == null) {
            // it might happen that we have reset the location data change callback while change was being processed
            appLog.we("got onCharacteristicChanged() while currentProxyPositionDataChangedCallbackWrapperHolder is null, ignoring", ErrorCode.GATT_OBSOLETE_CALLBACK);
        } else {
            try {
                List<ProxyPosition> proxyPositions = getGattDecoder().decodeProxyPositionData(value);
                if (proxyPositions != null) {
                    onNewProxyPositions(proxyPositions);
                    currentProxyPositionDataChangedCallbackWrapperHolder.value.onChange(proxyPositions);
                }
            } catch (GattRepresentationException e) {
                // report the error
                handleGattRepresentationException(currentProxyPositionDataChangedCallbackWrapperHolder.value::onFail, e);
            }
        }
    }

    private void onFwUpdatePollCharacteristicChange(byte[] value) {
        if (!fwUploadState.acceptingPollChanges) {
            appLog.we("got onCharacteristicChanged() while fwUploadState is " + fwUploadState + ", ignoring", ErrorCode.GATT_OBSOLETE_CALLBACK);
            return;
        }
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(currentFwUploadPollInternalCallback, "currentFwUploadPollInternalCallback is null while state is " + fwUploadState);
        }
        // notify the callback
        try {
            FwPollCommand pollCommand = getGattDecoder().decodeFwPollCommand(value);
            Preconditions.checkNotNull(pollCommand, "how could we get notification about new FW poll command if the value is empty?");
            currentFwUploadPollInternalCallback.onChange(pollCommand);
        } catch (GattRepresentationException exc) {
            appLog.we(exc.getMessage(), ErrorCode.GATT_REPRESENTATION, exc);
            currentFwUploadPollInternalCallback.onFail(new Fail(ErrorCode.GATT_REPRESENTATION, exc.getMessage()));
        }
    }

    @NonNull
    private List<WriteCharacteristicRequest> convertNetworkNodeToWriteRequests(NetworkNode networkNode, boolean nodeTypeChanged) {
        WriteCharacteristicRequestsBuilder builder = new WriteCharacteristicRequestsBuilder();
        NetworkNodeDiffingWrapper diffNode;
        if (!(networkNode instanceof NetworkNodeDiffingWrapper)) {
            NetworkNode origNode = networkNode;
            // wrap with pseudo-diffing wrapper - the original will be an empty node - everything will be different
            networkNode = NodeFactory.newDiffingWrapper(origNode.getId(), origNode.getType());
            // and the diff will be driven by what is present (non-null) in the passed original node
            networkNode.copyFrom(origNode);
        }
        diffNode = (NetworkNodeDiffingWrapper) networkNode;
        if (diffNode.isLabelChanged()) {
            appLog.d("different LABEL injected");
            builder.setService(BleConstants.SERVICE_UUID_STD_GAP);
            builder.addValue(BleConstants.CHARACTERISTIC_STD_LABEL, networkNode.getLabel());
        }
        builder.setService(BleConstants.SERVICE_UUID_NETWORK_NODE);
        boolean needsOperationMode = nodeTypeChanged || diffNode.isUwbModeChanged() || diffNode.isOperatingFirmwareChanged()
                || diffNode.isFirmwareUpdateEnableChanged() || diffNode.isLedIndicationEnableChanged();
        // other properties are passed just conditionally
        if (diffNode.isNetworkIdChanged()) {
            appLog.d("different NETWORK injected");
            builder.addValue(BleConstants.CHARACTERISTIC_NETWORK_ID, networkNode.getNetworkId());
        }
        if (diffNode.isPasswordChanged()) {
            appLog.d("different PASSWORD injected");
            builder.addValue(BleConstants.CHARACTERISTIC_PASSWORD, networkNode.getPassword());
        }
        if (diffNode.isLocationDataModeChanged()) {
            appLog.d("different LOCATION_DATA_MODE injected");
            builder.addValue(BleConstants.CHARACTERISTIC_LOCATION_DATA_MODE, GattEncoder.encodeLocationDataMode(networkNode.getLocationDataMode()));
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // anchor-specific modifiable properties
        if (networkNode.isAnchor()) {
            AnchorNodeDiffingWrapper anchor = (AnchorNodeDiffingWrapper) diffNode;
            // initiator
            if (!needsOperationMode) {
                needsOperationMode = anchor.isInitiatorChanged();
            }
            // position
            if (anchor.isPositionChanged()) {
                appLog.d("different POSITION injected");
                // now we will encode position (specific write-only characteristic)
                builder.addValue(BleConstants.CHARACTERISTIC_PERSISTED_POSITION, anchor.getPosition());
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // tag-specific modifiable properties
        if (diffNode.isTag()) {
            //noinspection ConstantConditions
            TagNodeDiffingWrapper tag = (TagNodeDiffingWrapper) diffNode;
            if (tag.isUpdateRateChanged() || tag.isStationaryUpdateRateChanged()) {
                appLog.d("setting up (STATIONARY) UPDATE RATE");
                builder.addValue(BleConstants.CHARACTERISTIC_TAG_UPDATE_RATE, GattEncoder.encodeUpdateRate(tag.getUpdateRate(), tag.getStationaryUpdateRate()));
            }
            if (tag.isLowPowerModeChanged() || tag.isLocationEngineEnableChanged() ||  tag.isAccelerometerEnableChanged()) {
                needsOperationMode = true;
            }
        }
        // operation mode change
        if (needsOperationMode) {
            // we are always passing operation mode - the mode needs to be in sync with everything else
            NetworkNode finalNetworkNode = NodeFactory.newNodeCopy(networkNode);
            // we are using lazy value provider, because there might be missing operation mode in decode context now (NPE, bug #152)
            // but definitely will be there later
            builder.prependValue(BleConstants.CHARACTERISTIC_OPERATION_MODE, () -> GattEncoder.encodeOperationMode(finalNetworkNode, getGattDecoder().getContext()));
        }
        return builder.build();
    }

    /**
     * All the asynchronous notifications are passed to GATT operation queue as 'success'.
     */
    private void onOperationComplete(SynchronousBleGatt gatt) {
        if (Constants.DEBUG) {
            Preconditions.checkState(gattOperationQueue.hasPendingAsyncOperation());
        }
        // delegate to HEAD operation (remove the operation at head), keep the queue active
        gattOperationQueue.onGattSuccess(gatt);
    }

    /**
     * All the asynchronous notifications are passed to GATT operation queue as 'fail'.
     */
    private void onOperationFailed(SynchronousBleGatt gatt, int errorCode, String errorMessage) {
        if (Constants.DEBUG) {
            Preconditions.checkState(gattOperationQueue.hasPendingAsyncOperation());
        }
        // we will rather enable debug logs - to have as much info as possible
        logBlockStatus.unblockDeviceLog(bleAddress);
        fsm.setDebugLoggingEnabled(true);
        // delegate to HEAD operation (remove the operation at head)
        gattOperationQueue.onGattFail(gatt, errorCode, errorMessage);
    }

    private static Set<ReadCharacteristicRequest> TAG_REQUESTS, ANCHOR_REQUESTS;
    private static final ReadCharacteristicRequest OPERATION_MODE_REQUEST =
            new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_OPERATION_MODE);

    private static Set<ReadCharacteristicRequest> getRequests(NodeType nodeType, boolean keepOperationMode) {
        switch (nodeType) {
            case TAG:
                if (TAG_REQUESTS == null) {
                    TAG_REQUESTS = cloneAndDecorate(PropertySet.TAG_REQUESTS, keepOperationMode);
                }
                return TAG_REQUESTS;
            case ANCHOR:
                if (ANCHOR_REQUESTS == null) {
                    ANCHOR_REQUESTS = cloneAndDecorate(PropertySet.ANCHOR_REQUESTS, keepOperationMode);
                }
                return ANCHOR_REQUESTS;
            default:
                throw new IllegalStateException("unexpected node type: " + nodeType);
        }
    }

    private static HashSet<ReadCharacteristicRequest> cloneAndDecorate(Set<ReadCharacteristicRequest> requests,
                                                                       boolean keepOperationMode) {
        HashSet<ReadCharacteristicRequest> r = new HashSet<>(requests);
        if (!keepOperationMode) r.remove(OPERATION_MODE_REQUEST);
        // add proxy characteristic - just make sure that we are connecting to a compatible node
        r.add(new ReadCharacteristicRequest(BleConstants.SERVICE_UUID_NETWORK_NODE, BleConstants.CHARACTERISTIC_PROXY_POSITIONS));
        return r;
    }

    @Override
    public String toString() {
        return "NetworkNodeBleConnectionImpl{" + "bleAddress='" + bleAddress + '\'' +
                ", state=" + state +
                '}';
    }
}
