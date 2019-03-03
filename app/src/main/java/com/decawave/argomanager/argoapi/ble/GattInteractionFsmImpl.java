/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.interaction.ErrorCode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.BleDevice;
import com.decawave.argomanager.ble.BleGatt;
import com.decawave.argomanager.ble.BleGattCallback;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.BleGattDescriptor;
import com.decawave.argomanager.ble.BleGattService;
import com.decawave.argomanager.ble.BleStatus;
import com.decawave.argomanager.ble.ConnectionSpeed;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.util.gatt.GattEncoder;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import eu.kryl.android.common.fsm.impl.FiniteStateMachineImpl;
import eu.kryl.android.common.log.ComponentLog;
import eu.kryl.android.common.log.LogLevel;
import rx.functions.Action0;
import rx.functions.Action3;

/**
 * FSM helping to manage connection to particular BLE device.
 */
public class GattInteractionFsmImpl extends FiniteStateMachineImpl<GattInteractionState> implements GattInteractionFsm {
    private static final int FAKE_DISCONNECT_CALLBACK_DELAY = 4000;  // 4 s
    // dependencies
    private final BleDevice bleDevice;
    private final String bleAddress;
    private final NetworkNodeManager networkNodeManager;
    private final ConnectionListener connectionListener;
    // members
    private final ApplicationComponentLog appLog;
    private GattInteractionInternalWrappedCallback gattInteractionCallbackWrapped;
    // state variables (change in time)
    private BleGatt bleGatt;
    private List<ReadCharacteristicRequest> remainingCharsReadRequests;
    private List<WriteCharacteristicRequest> remainingCharsWriteRequests;
    private List<ReadDescriptorRequest> remainingDescReadRequests;
    private List<WriteDescriptorRequest> remainingDescWriteRequests;
    private SynchronousBleGatt synchronousBleGatt;
    private Integer changeMtuRequest;
    private boolean debugLoggingEnabled;
    private boolean disconnectOnProblem = true;
    // we track existence error code because of connection tracker
    private Integer sessionErrorCode;
    //
    private Runnable currentReadCharacteristicTimeoutRunnable;
    private Runnable currentChangeMtuTimeoutRunnable;
    // helper visitor to set characteristics
    private WriteRequestToCharacteristicVisitor writeVisitor = new WriteRequestToCharacteristicVisitor();

    public interface ConnectionListener {

        void onConnecting(String address);

        void onConnected(String address, GattInteractionFsm fsm, SynchronousBleGatt syncGatt);

        // letting know that connect failed and we are now initiating disconnect (still onDisconnected will be called)
        void onConnectFailed(String address, GattInteractionFsm fsm);

        // this is more important than onConnected (after services are discovered real interaction can begin)
        void onServicesDiscovered(String address, GattInteractionFsm fsm, SynchronousBleGatt bleGatt);

        void onDisconnecting(String address);

        void onDisconnected(String address, Integer sessionErrorCode);

    }

    /**
     * When operation is being initiated on a characteristic (read/write), this tells us about the result.
     */
    private enum RwResult {
        INIT_OK,
        INIT_FAIL(ErrorCode.GATT_OPERATION_INIT),
        MISSING_OK,
        MISSING_FAIL(ErrorCode.GATT_MISSING_CHARACTERISTIC);   // for mandatory characteristics

        public final int errorCode;

        RwResult() {
            errorCode = -1;
        }

        RwResult(int errorCode) {
            this.errorCode = errorCode;
        }
    }

    private BleGattCallback systemGattCallback = new BleGattCallback() {

        @Override
        public void onConnectionStateChange(BleGatt gatt, int status, ConnectionState newConnectionState) {
            //
            logSystemAsync("onConnectionStateChange", status, "newConnectionState = " + newConnectionState, null);
            // check that we are in valid state
            if (getState() == GattInteractionState.DISCONNECTED) {
                appLog.we("ignoring onConnectionStateChange (already CLOSED): newConnectionState=" + newConnectionState + ", status = " + status, ErrorCode.GATT_OBSOLETE_CALLBACK);
                return;
            }
            // process the callback
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore
                if (newConnectionState == ConnectionState.CONNECTED) {
                    // initiate disconnection
                    setState(GattInteractionState.DISCONNECTING);
                } else if (newConnectionState == ConnectionState.CLOSED) {
                    if (getState() == GattInteractionState.CONNECTING) {
                        // let the listener know that the connection attempt failed
                        connectionListener.onConnectFailed(bleAddress, GattInteractionFsmImpl.this);
                    }
                    // confirm so that we can do cleanup
                    setState(GattInteractionState.DISCONNECTED);
                }
            } else if (status == BleStatus.GATT_SUCCESS) {
                if (newConnectionState == ConnectionState.CONNECTED) {
                    GattInteractionState currState = getState();
                    if (currState == GattInteractionState.CONNECTING) {
                        // confirm the state to FSM
                        setState(GattInteractionState.JUST_CONNECTED);
                        return;
                    } // else:
                    if (currState == null) {
                        // ignore this nonsense
                        return;
                    }
                    // it might happen that we initiated DISCONNECT after elapsed connect timeout, simply wait for
                    Preconditions.checkState(getState() == GattInteractionState.DISCONNECTING, "current state is " + getState());
                    // initiate disconnect again (to make sure that Android has heard us)
                    bleGatt.disconnect();
                } else if (newConnectionState == ConnectionState.CLOSED) {
                    // we are disconnected
                    // sometimes we might get disconnected unexpectedly, is this expected or unexpected disconnect?
                    if (getState() != GattInteractionState.DISCONNECTING) {
                        gattInteractionCallbackWrapped.onFail(ErrorCode.BLE_CONNECTION_DROPPED, "sudden disconnect occurred");
                    }
                    setState(GattInteractionState.DISCONNECTED);
                } // else: other newState is not important (now)
            } else {
                // we presume that only CLOSED error is reported
                Preconditions.checkState(newConnectionState == ConnectionState.CLOSED, "unexpected error state: " + newConnectionState);
                // this is not GATT_SUCCESS
                if (sessionErrorCode == null) {
                    // set up the session error code
                    sessionErrorCode = ErrorCode.BLE_CONNECTION_DROPPED;
                    // there was no previous error
                    gattInteractionCallbackWrapped.onFail(ErrorCode.BLE_CONNECTION_DROPPED, "sudden connection change " + newConnectionState + " occurred");
                }
                //
                if (getState() == GattInteractionState.CONNECTING) {
                    // let the listener know, that connect attempt failed
                    connectionListener.onConnectFailed(bleAddress, GattInteractionFsmImpl.this);
                }
                // do direct transition to disconnected
                setState(GattInteractionState.DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BleGatt gatt, int status) {
            logSystemAsync("onServicesDiscovered", status, null, null);
            doOnServicesDiscovered(status);
        }

        @Override
        public void onCharacteristicWritten(BleGatt gatt, BleGattCharacteristic characteristic, int status) {
            if (shouldIgnoreCallback("onCharacteristicWritten()", GattInteractionState.WRITING_CHARACTERISTICS)) return;
            String charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristic.getUuid());
            logSystemAsync("onCharacteristicWritten", status, null, "characteristic '" + charName + "' write failed");
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore, initiate disconnection
                setState(GattInteractionState.DISCONNECTING);
            } else if (status == BleStatus.GATT_SUCCESS) {
                // success, check if all characteristics are loaded now
                if (debugLoggingEnabled) {
                    appLog.d("written CHR '" + charName + "'");
                }
                WriteCharacteristicRequest firstRequest = remainingCharsWriteRequests.remove(0);
                // check that we have written the right characteristic
                Preconditions.checkState(firstRequest.characteristicUuid.equals(characteristic.getUuid()));
                //
                initiateNextCharacteristicWrite();
            } else {
                String msg = "characteristic " + charName + " write failed";
                // fail, disconnect
                handleFail(() -> gattInteractionCallbackWrapped.onCharacteristicWriteFailed(
                        ErrorCode.GATT_ASYNC,
                        msg));
            }
        }

        @Override
        public void onDescriptorRead(BleGatt gatt, BleGattDescriptor descriptor, byte[] value, int status) {
            if (shouldIgnoreCallback("onDescriptorRead()", GattInteractionState.READING_DESCRIPTORS)) return;
            String descName = BleConstants.MAP_DESCRIPTOR_TITLE.get(descriptor.getUuid());
            logSystemAsync("onDescriptorRead", status, "value = " + GattEncoder.printByteArray(value), "descriptor '" + descName + "' read failed");
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore, initiate disconnection
                setState(GattInteractionState.DISCONNECTING);
            } else if (status == BleStatus.GATT_SUCCESS) {
                // success, check if all descriptors are loaded now
                if (debugLoggingEnabled) {
                    appLog.d("read DESC '" + descName + "'");
                }
                ReadDescriptorRequest firstRequest = remainingDescReadRequests.remove(0);
                // check that we have read the right descriptor
                if (com.decawave.argomanager.Constants.DEBUG) {
                    Preconditions.checkState(firstRequest.descriptorUuid.equals(descriptor.getUuid()));
                    Preconditions.checkState(firstRequest.characteristicUuid.equals(descriptor.getCharacteristic().getUuid()));
                    Preconditions.checkState(firstRequest.serviceUuid.equals(descriptor.getCharacteristic().getService().getUuid()));
                }
                //
                initiateNextDescriptorRead();
            } else {
                // fail
                String msg = "failed to READ descriptor, status = " + status;
                handleFail(() -> gattInteractionCallbackWrapped.onDescriptorReadFailed(
                        ErrorCode.GATT_ASYNC,
                        msg));
            }
        }

        @Override
        public void onMtuChanged(BleGatt gatt, int mtu, int status) {
            if (shouldIgnoreCallback("onMtuChanged()", GattInteractionState.CHANGING_MTU)) return;
            String errMsg = "MTU change request failed";
            logSystemAsync("onMtuChanged", status, "mtu = " + mtu, errMsg);
            // unschedule the timeout (if any)
            if (currentChangeMtuTimeoutRunnable != null) {
                unscheduleRunnable(currentChangeMtuTimeoutRunnable);
                currentChangeMtuTimeoutRunnable = null;
            }
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore, initiate disconnection
                setState(GattInteractionState.DISCONNECTING);
            } else if (status == BleStatus.GATT_SUCCESS) {
                // we might still receive smaller MTU than we requested
                if (mtu < changeMtuRequest) {
                    final String fErrMsg = "negotiated MTU is smaller than requested, requested = " + changeMtuRequest + ", negotiated = " + mtu;
                    // the negotiated MTU does not correspond to our request
                    // disconnect
                    handleFail(() -> gattInteractionCallbackWrapped.onMtuChangeFailed(
                            ErrorCode.GATT_INSUFFICIENT_MTU,
                            fErrMsg));
                } else {
                    // success, check if all descriptors are loaded now
                    if (debugLoggingEnabled) {
                        appLog.d("successfully changed MTU");
                    }
                    // we are done
                    setState(GattInteractionState.MTU_CHANGED);
                }
            } else {
                // fail
                handleFail(() -> gattInteractionCallbackWrapped.onMtuChangeFailed(
                        ErrorCode.GATT_ASYNC,
                        errMsg));
            }
        }

        @Override
        public void onDescriptorWritten(BleGatt gatt, BleGattDescriptor descriptor, int status) {
            if (shouldIgnoreCallback("onDescriptorWritten()", GattInteractionState.WRITING_DESCRIPTORS)) return;
            String descName = BleConstants.MAP_DESCRIPTOR_TITLE.get(descriptor.getUuid());
            logSystemAsync("onDescriptorWritten", status, null, "descriptor '" + descName + "' write failed");
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore, initiate disconnection
                setState(GattInteractionState.DISCONNECTING);
            } else if (status == BleStatus.GATT_SUCCESS) {
                // success, check if all descriptors are loaded now
                if (debugLoggingEnabled) {
                    appLog.d("written DESC '" + descName + "'");
                }
                WriteDescriptorRequest firstRequest = remainingDescWriteRequests.remove(0);
                // check that we have written the right descriptor
                Preconditions.checkState(firstRequest.descriptorUuid.equals(descriptor.getUuid()));
                //
                if (!remainingDescWriteRequests.isEmpty()) {
                    initiateNextDescriptorWrite();
                } else {
                    // we are done
                    setState(GattInteractionState.DESCRIPTORS_WRITTEN);
                }
            } else {
                // fail
                String msg = "descriptor " + descName + " write failed";
                handleFail(() -> gattInteractionCallbackWrapped.onDescriptorWriteFailed(
                        ErrorCode.GATT_ASYNC,
                        msg));
            }
        }

        @Override
        public void onCharacteristicRead(BleGatt gatt, BleGattCharacteristic characteristic, byte[] value, int status) {
            if (shouldIgnoreCallback("onCharacteristicRead()", GattInteractionState.READING_CHARACTERISTICS)) return;
            String charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristic.getUuid());
            logSystemAsync("onCharacteristicRead", status, "value = " + GattEncoder.printByteArray(value), "characteristic '" + charName + "' read failed");
            // unschedule the timeout (if any)
            if (currentReadCharacteristicTimeoutRunnable != null) {
                unscheduleRunnable(currentReadCharacteristicTimeoutRunnable);
                currentReadCharacteristicTimeoutRunnable = null;
            }
            // check the result
            if (!gattInteractionCallbackWrapped.stillInterested()) {
                // we are not interested anymore, initiate disconnection
                setState(GattInteractionState.DISCONNECTING);
            } else if (status == BleStatus.GATT_SUCCESS) {
                // success, check if all characteristics are loaded now
                if (debugLoggingEnabled) {
                    appLog.d("read CHR '" + charName + "'");
                }
                ReadCharacteristicRequest firstRequest = remainingCharsReadRequests.remove(0);
                // check that we have read the right characteristic
                if (com.decawave.argomanager.Constants.DEBUG) {
                    Preconditions.checkState(firstRequest.characteristicUuid.equals(characteristic.getUuid()));
                    Preconditions.checkState(firstRequest.serviceUuid.equals(characteristic.getService().getUuid()));
                }
                //
                initiateNextCharacteristicRead();
            } else {
                // fail
                int errorCode = status == BleStatus.GATT_TIMEOUT ? ErrorCode.BLE_READ_TIMEOUT : ErrorCode.GATT_ASYNC;
                handleFail(() -> gattInteractionCallbackWrapped.onCharacteristicReadFailed(errorCode
                        ,"failed to READ characteristic " + charName + ", status = " + status));
            }
        }

        @Override
        public void onCharacteristicChanged(BleGatt bleGatt, BleGattCharacteristic characteristic, byte[] value) {
            GattInteractionState s = getState();
            if (s == null || s.terminal()) {
                appLog.we("ignoring onCharacteristicChanged, we are " + s, ErrorCode.GATT_OBSOLETE_CALLBACK);
                return;
            } // else: let the interaction know
            gattInteractionCallbackWrapped.onCharacteristicChanged(characteristic, value);
        }

        private boolean shouldIgnoreCallback(final String callbackMethodName, GattInteractionState expectedState) {
            if (getState() != expectedState) {
                appLog.we("ignoring " + callbackMethodName + ", we are not in " + expectedState + " anymore",
                        ErrorCode.GATT_OBSOLETE_CALLBACK);
                return true;
            }
            return false;
        }


    };

    public void setDisconnectOnProblem(boolean doDisconnect) {
        this.disconnectOnProblem = doDisconnect;
    }

    private void logSystemAsync(String asyncCallbackName, int status, @Nullable String extra, @Nullable String errorDetails) {
        if (status == BleStatus.GATT_SUCCESS) {
            if (debugLoggingEnabled) {
                appLog.d(asyncCallbackName + ": status = " + status + ", current state = " + getState()
                        + (extra == null ? "" : ", " + extra));
            }
        } else {
            appLog.we(asyncCallbackName + ": status = " + status + ", current state = " + getState()
                    + (extra == null ? "" : ", " + extra)
                    + (errorDetails != null ? ", " + errorDetails : ""), ErrorCode.GATT_ASYNC);
        }
    }

    public void setDebugLoggingEnabled(boolean debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled;
        getLog().setLogLevel(debugLoggingEnabled ? LogLevel.DEBUG : LogLevel.INFO);
    }

    private void doOnServicesDiscovered(int status) {
        GattInteractionState state = getState();
        if (Constants.DEBUG) {
            // this is nonsense
            Preconditions.checkState(state != GattInteractionState.CONNECTING && state != GattInteractionState.JUST_CONNECTED,
                    "onServiceDiscovered() occurred in " + state);
        }
        if (state != GattInteractionState.DISCOVERING_SERVICES) {
            appLog.we("ignoring onServicesDiscovered() callback invocation in state " + state, ErrorCode.GATT_OBSOLETE_CALLBACK);
            return;
        }
        if (!gattInteractionCallbackWrapped.stillInterested()) {
            // we are not interested anymore, initiate disconnection
            setState(GattInteractionState.DISCONNECTING);
        } else if (status == BleStatus.GATT_SUCCESS) {
            setState(GattInteractionState.SERVICES_DISCOVERED);
        } else {
            // fail
            gattInteractionCallbackWrapped.onFail(ErrorCode.GATT_FAILED_SERVICE_DISCOVERY, "service discovery failed (status = " + status + ")");
            // we have no other option than disconnect - there are no service, nor characteristics
            setState(GattInteractionState.DISCONNECTING);
        }
    }

    public GattInteractionFsmImpl(BleDevice bleDevice, NetworkNodeManager networkNodeManager, ConnectionListener connectionListener) {
        super("GattInteractionFsm [" + bleDevice + "]", GattInteractionState.class, new ComponentLog("GattInteractionFsm [" + bleDevice + "]"));
        this.bleDevice = bleDevice;
        this.bleAddress = bleDevice.getAddress();
        this.networkNodeManager = networkNodeManager;
        this.connectionListener = connectionListener;
        // logging
        this.appLog = ApplicationComponentLog.newNetworkNodeLog(getLog(), bleAddress);
        this.debugLoggingEnabled = getLog().isEnabled();
    }

    @Override
    public boolean isTerminate() {
        return !isActive() || getState().terminal();
    }

    @Override
    public boolean isDisconnected() {
        GattInteractionState state = getState();
        return state == null || state == GattInteractionState.DISCONNECTED;
    }

    @Override
    public void writeDescriptors(List<WriteDescriptorRequest> writeRequests) {
        // check state validity
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass write descriptors request in non-IDLE state: " + state);
        //
        this.remainingDescWriteRequests = writeRequests;
        // initiate write
        setState(GattInteractionState.WRITING_DESCRIPTORS);
    }

    @Override
    public void writeCharacteristics(List<WriteCharacteristicRequest> writeRequests) {
        // check state validity
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass write characteristics request in non-IDLE state: " + state);
        //
        this.remainingCharsWriteRequests = new ArrayList<>(writeRequests);
        // initiate write
        setState(GattInteractionState.WRITING_CHARACTERISTICS);
    }

    @Override
    public void changeMtu(int newMtu) {
        // check state validity
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass change MTU request in non-IDLE state: " + state);
        //
        this.changeMtuRequest = newMtu;
        // initiate write
        setState(GattInteractionState.CHANGING_MTU);
    }

    @Override
    public boolean changeConnectionSpeed(ConnectionSpeed connectionSpeed) {
        // check state validity
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass change connection priority request in non-IDLE state: " + state);
        //
        if (debugLoggingEnabled) {
            appLog.d("changing connection speed: " + connectionSpeed);
        }
        // this does not have a callback method, Google...
        return bleGatt.requestConnectionSpeed(connectionSpeed);
    }

    @Override
    public @Nullable Integer getLastNegotiatedSystemMtu() {
        return changeMtuRequest;
    }

    @Override
    public boolean doDisconnectOnProblem() {
        return disconnectOnProblem;
    }

    @Override
    public void makeSureIdle() {
        GattInteractionState currState = getState();
        if (!currState.idle) {
            // clear remaining requests if any
            if (remainingCharsReadRequests != null) remainingCharsReadRequests.clear();
            if (remainingCharsWriteRequests != null) remainingCharsWriteRequests.clear();
            if (remainingDescWriteRequests != null) remainingDescWriteRequests.clear();
            if (remainingDescReadRequests != null) remainingDescReadRequests.clear();
            // set IDLE state
            setState(GattInteractionState.IDLE);
        }
    }

    @Override
    public void readDescriptors(List<ReadDescriptorRequest> readRequests) {
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass read descriptors in non-IDLE state: " + state);
        Preconditions.checkState(remainingDescReadRequests == null || remainingDescReadRequests.isEmpty(), "cannot initiate next read request while there is pending read request");
        // initiate descriptor retrieval
        // set up stateful variable representing remaining characteristics to load
        remainingDescReadRequests = new LinkedList<>(readRequests);
        // initiate read
        setState(GattInteractionState.READING_DESCRIPTORS);
    }

    @Override
    public void readCharacteristics(Set<ReadCharacteristicRequest> readRequests) {
        // delegate
        doReadCharacteristics(new LinkedList<>(readRequests));
    }

    private void doReadCharacteristics(List<ReadCharacteristicRequest> readRequests) {
        GattInteractionState state = getState();
        Preconditions.checkState(state.idle, "forbidden to pass read characteristics request in non-IDLE state: " + state);
        Preconditions.checkState(remainingCharsReadRequests == null || remainingCharsReadRequests.isEmpty(), "cannot initiate next read request while there is pending read request");
        // initiate characteristics retrieval
        // set up stateful variable representing remaining characteristics to load
        remainingCharsReadRequests = readRequests;
        // initiate read
        setState(GattInteractionState.READING_CHARACTERISTICS);
    }

    @Override
    public void readCharacteristics(ReadCharacteristicRequest... readRequests) {
        // delegate
        doReadCharacteristics(Lists.newArrayList(readRequests));
    }

    private SynchronousBleGatt getSyncGatt() {
        if (synchronousBleGatt == null) {
            synchronousBleGatt = new SynchronousBleGatt() {
                @Override
                public BleGattServiceRdonly getService(UUID serviceUuid) {
                    if (Constants.DEBUG) {
                        Preconditions.checkState(bleGatt != null);
                    }
                    return wrapServiceAsRdonly(bleGatt.getService(serviceUuid));
                }

                @Override
                public boolean setCharacteristicNotification(BleGattCharacteristic characteristic, boolean enable) {
                    if (Constants.DEBUG) {
                        Preconditions.checkState(bleGatt != null);
                    }
                    return bleGatt.setCharacteristicNotification(characteristic, enable);
                }

                @Override
                public String getDeviceAddress() {
                    return bleAddress;
                }

                @Override
                public List<BleGattServiceRdonly> getServices() {
                    if (Constants.DEBUG) {
                        Preconditions.checkState(bleGatt != null);
                    }
                    List<BleGattService> services = bleGatt.getServices();
                    List<BleGattServiceRdonly> l = new ArrayList<>(services.size());
                    for (BleGattService service : services) {
                        l.add(wrapServiceAsRdonly(service));
                    }
                    return l;
                }
            };
        }
        return synchronousBleGatt;
    }

    private Map<BleGattService, BleGattServiceRdonly> rdonlyServiceMap = new HashMap<>();
    
    private BleGattServiceRdonly wrapServiceAsRdonly(final BleGattService service) {
        if (service == null) {
            return null;
        }
        BleGattServiceRdonly s = rdonlyServiceMap.get(service);
        if (s == null) {
            s = new BleGattServiceRdonly() {
                @Override
                public UUID getUuid() {
                    return service.getUuid();
                }

                @Override
                public BleGattCharacteristic getCharacteristic(UUID characteristicUuid) {
                    return service.getCharacteristic(characteristicUuid);
                }

                @Override
                public List<BleGattService> getIncludedServices() {
                    return service.getIncludedServices();
                }
            };
            rdonlyServiceMap.put(service, s);
        }
        return s;
    }

    private void initiateNextCharacteristicWrite() {
        boolean skip;
        String charName;
        byte[] newValue;
        WriteCharacteristicRequest writeRequest;
        BleGattCharacteristic chrsic;
        do {
            if (remainingCharsWriteRequests.isEmpty()) {
                // move directly to CHAR_WRITTEN
                if (Constants.DEBUG && debugLoggingEnabled) {
                    log.d("remainingCharsWriteRequests.isEmpty");
                }
                setState(GattInteractionState.CHARACTERISTICS_WRITTEN);
                return;
            }
            writeRequest = remainingCharsWriteRequests.get(0);
            charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(writeRequest.characteristicUuid);
            chrsic = bleGatt.getService(writeRequest.serviceUuid).getCharacteristic(writeRequest.characteristicUuid);
            if (chrsic == null) {
                handleCharWriteFail("characteristic " + charName + " cannot be written, it is missing",
                        ErrorCode.GATT_MISSING_CHARACTERISTIC);
                return;
            }
            // set proper write type
            chrsic.setWriteType(writeRequest.writeType);
            byte[] oldValue = chrsic.getValue();
            // check the characteristic value - if it is different
            byte[] oldValueClone = oldValue == null ? null : oldValue.clone();
            // fill the characteristics with value according to the write request
            writeVisitor.setCharacteristic(chrsic);
            writeRequest.accept(writeVisitor);
            // now check that the written value is different
            newValue = chrsic.getValue();
            skip = Objects.equal(oldValueClone, newValue);
            if (skip) {
                if (debugLoggingEnabled) {
                    appLog.d("skipping CHR " + charName + " WRITE - no need to update");
                }
                remainingCharsWriteRequests.remove(0);
            }
        } while (skip);
        if (debugLoggingEnabled) {
            appLog.d("initiating CHR '" + charName + "' write (" + writeRequest.characteristicUuid
                    + "), value = " + GattEncoder.printByteArray(newValue));
        }
        // now initiate the write
        if (!bleGatt.writeCharacteristic(chrsic)) {
            handleCharWriteFail("cannot initiate write of characteristic " + charName, ErrorCode.GATT_OPERATION_INIT);
        } // else: wait for the write result (onCharacteristicWritten)
    }

    private void initiateMtuChange() {
        Preconditions.checkNotNull(changeMtuRequest);

        if (!bleGatt.requestMtu(changeMtuRequest)) {
            // failed to initiate the change request
            // handle the fail
            handleFail(() -> gattInteractionCallbackWrapped.onMtuChangeFailed(ErrorCode.GATT_OPERATION_INIT, "failed to initiate MTU change"));
        } else {
            // MTU change initiated, wait in the CHANGING_MTU state and schedule timeout runnable
            currentChangeMtuTimeoutRunnable = () -> {
                appLog.we("timeout occurred while changing MTU, firing pseudo-fail", ErrorCode.GATT_OPERATION_TIMEOUT);
                currentChangeMtuTimeoutRunnable = null;
                // go through the internal gatt callback
                systemGattCallback.onMtuChanged(bleGatt, changeMtuRequest != null ? changeMtuRequest : 23, BleStatus.GATT_TIMEOUT);
            };
            scheduleRunnable(currentChangeMtuTimeoutRunnable, BleConstants.CHANGE_MTU_TIMEOUT);
            // wait for the asynchronous notification
        }
    }

    private void handleFail(Action0 callbackNotifyAction) {
        if (disconnectOnProblem) {
            callbackNotifyAction.call();
            setState(GattInteractionState.DISCONNECTING);
        } else {
            setState(GattInteractionState.LAST_OPERATION_FAILED_CONNECTED);
            callbackNotifyAction.call();
        }
    }

    private void handleCharWriteFail(String message, int errorCode) {
        // let the callback know, handle the fail
        handleFail(() -> gattInteractionCallbackWrapped.onCharacteristicWriteFailed(errorCode, message));
    }

    private void initiateNextDescriptorWrite() {
        Preconditions.checkState(remainingDescWriteRequests.size() > 0);

        WriteDescriptorRequest writeRequest = remainingDescWriteRequests.get(0);
        BleGattCharacteristic chrsic = bleGatt.getService(writeRequest.serviceUuid).getCharacteristic(writeRequest.characteristicUuid);
        String charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(writeRequest.characteristicUuid);
        String descName = BleConstants.MAP_DESCRIPTOR_TITLE.get(writeRequest.descriptorUuid);
        if (chrsic == null) {
            handleDescriptorWriteFail("characteristic " + charName + " is missing, cannot write descriptor " + descName,
                    ErrorCode.GATT_MISSING_CHARACTERISTIC);
            return;
        }
        // fill the characteristics with value according to the write request
        BleGattDescriptor descriptor = chrsic.getDescriptor(writeRequest.descriptorUuid);
        if (descriptor == null) {
            handleDescriptorWriteFail("descriptor " + descName + " cannot be written, it is missing", ErrorCode.GATT_MISSING_DESCRIPTOR);
            return;
        }
        descriptor.setValue(writeRequest.value);
        if (debugLoggingEnabled) {
            appLog.d("initiating " + charName + " DESC '" + descName + "' write (" + writeRequest.descriptorUuid
                    + "), value = " + GattEncoder.printByteArray(writeRequest.value));
        }
        // now initiate the write
        if (!bleGatt.writeDescriptor(descriptor)) {
            handleDescriptorWriteFail("failed to initiate descriptor " + descName + " write", ErrorCode.GATT_OPERATION_INIT);
        } // else: wait for the write result (onCharacteristicWritten)
    }

    private void handleDescriptorWriteFail(String message, int errorCode) {
        // let the callback know, handle the fail
        handleFail(() -> gattInteractionCallbackWrapped.onDescriptorWriteFailed(errorCode, message));
    }

    private void initiateNextDescriptorRead() {
        if (remainingDescReadRequests.isEmpty()) {
            // we are done
            setState(GattInteractionState.DESCRIPTORS_READ);
            return;
        } // else:
        Preconditions.checkState(!remainingDescReadRequests.isEmpty());
        Iterator<ReadDescriptorRequest> it = remainingDescReadRequests.iterator();
        ReadDescriptorRequest request;
        RwResult r;
        do {
            request = it.next();
            BleGattService service = bleGatt.getService(request.serviceUuid);
            BleGattCharacteristic characteristic = service.getCharacteristic(request.characteristicUuid);

            r = initiateDescriptorRead(characteristic, request.descriptorUuid, request.mandatory);
            if (r == RwResult.MISSING_OK) {
                // descriptor missing (and it is not mandatory), try the next UUID in the row
                it.remove();
            } else {
                break;
            }
        } while (it.hasNext());
        // unable to find any matching descriptor
        if (r == RwResult.MISSING_OK) {
            // no further descriptors found, we are done
            setState(GattInteractionState.DESCRIPTORS_READ);
        } else if (r != RwResult.INIT_OK) {
            // cannot initiate read of a descriptor
            String descName = BleConstants.MAP_DESCRIPTOR_TITLE.get(request.descriptorUuid);
            final RwResult fr = r;
            handleFail(() -> gattInteractionCallbackWrapped.onDescriptorReadFailed(
                    fr.errorCode,
                    "Failed to initiate descriptor " + descName + " read"));
        } // else: descriptor retrieval initiated, wait in the READING_DESCRIPTORS state
    }

    private void initiateNextCharacteristicRead() {
        if (remainingCharsReadRequests.isEmpty()) {
            // we are done
            setState(GattInteractionState.CHARACTERISTICS_READ);
            return;
        } // else:
        Preconditions.checkState(!remainingCharsReadRequests.isEmpty());
        Iterator<ReadCharacteristicRequest> it = remainingCharsReadRequests.iterator();
        ReadCharacteristicRequest svcAndChar;
        RwResult r;
        do {
            ReadCharacteristicRequest fSvcAndChar = it.next();
            svcAndChar = fSvcAndChar;
            BleGattService service = bleGatt.getService(fSvcAndChar.serviceUuid);
            // sometimes, this just happens... Android
            if (service == null) {
                handleFail(() -> gattInteractionCallbackWrapped.onCharacteristicReadFailed(
                        ErrorCode.GATT_BROKEN,
                        "Failed to retrieve service " + fSvcAndChar.serviceUuid));
                return;
            }
            r = initiateCharacteristicRead(service, fSvcAndChar.characteristicUuid, fSvcAndChar.mandatory);
            if (r == RwResult.MISSING_OK) {
                // characteristic missing (and it is not mandatory), try the next UUID in the row
                it.remove();
            } else {
                break;
            }
        } while (it.hasNext());
        // unable to find any matching characteristic
        final ReadCharacteristicRequest sac = svcAndChar;
        String charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(sac.characteristicUuid);
        if (r == RwResult.MISSING_OK) {
            // no further characteristics found, we are done
            setState(GattInteractionState.CHARACTERISTICS_READ);
        } else if (r == RwResult.INIT_OK) {
            // characteristic retrieval initiated, wait in the READING_CHARACTERISTICS state and schedule timeout runnable
            currentReadCharacteristicTimeoutRunnable = () -> {
                appLog.we("timeout occurred while reading characteristic "
                        + charName
                        + ", firing pseudo-fail, initiating disconnect", ErrorCode.GATT_OPERATION_TIMEOUT);
                currentReadCharacteristicTimeoutRunnable = null;
                // go through the internal gatt callback
                systemGattCallback.onCharacteristicRead(bleGatt, bleGatt.getService(sac.serviceUuid).getCharacteristic(sac.characteristicUuid), null, BleStatus.GATT_TIMEOUT);
            };
            scheduleRunnable(currentReadCharacteristicTimeoutRunnable, BleConstants.READ_CHARACTERISTIC_TIMEOUT);
        } else {
            // cannot initiate read of a characteristic or the characteristic is missing
            final RwResult fr = r;
            String message;
            if (r == RwResult.MISSING_FAIL) {
                message = getMissingCharacteristicMessage(sac.characteristicUuid);
            } else {
                message = "Failed to initiate characteristic " + charName + " read";
            }
            handleFail(() -> gattInteractionCallbackWrapped.onCharacteristicReadFailed(
                    fr.errorCode,
                    message));
        }
    }

    private RwResult initiateDescriptorRead(BleGattCharacteristic characteristic, UUID descriptorUuid, boolean mandatory) {
        BleGattDescriptor d = characteristic.getDescriptor(descriptorUuid);
        String descName = BleConstants.MAP_DESCRIPTOR_TITLE.get(descriptorUuid);
        if (d == null) {
            String msg = "missing '" + descName + "' (" + descriptorUuid + ") DESC!";
            if (mandatory) {
                appLog.we(msg, ErrorCode.GATT_MISSING_DESCRIPTOR);
                return RwResult.MISSING_FAIL;
            }
            appLog.we(msg, ErrorCode.GATT_MISSING_DESCRIPTOR_WARN);
            // not mandatory, no problem
            return RwResult.MISSING_OK;
        } // else:
        if (debugLoggingEnabled) {
            appLog.d("initiating DESC '" + descName + "' load (" + descriptorUuid + ")");
        }
        return bleGatt.readDescriptor(d) ? RwResult.INIT_OK : RwResult.INIT_FAIL;
    }

    private RwResult initiateCharacteristicRead(BleGattService service, UUID characteristicUuid, boolean mandatory) {
        BleGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
        String charName = BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristicUuid);
        if (ch == null) {
            if (mandatory) {
                return RwResult.MISSING_FAIL;
            }
            appLog.we(getMissingCharacteristicMessage(characteristicUuid), ErrorCode.GATT_MISSING_CHARACTERISTIC_WARN);
            // not mandatory, no problem
            return RwResult.MISSING_OK;
        } // else:
        if (debugLoggingEnabled) {
            appLog.d("initiating CHR '" + charName + "' load (" + characteristicUuid + ")");
        }
        return bleGatt.readCharacteristic(ch) ? RwResult.INIT_OK : RwResult.INIT_FAIL;
    }

    private String getMissingCharacteristicMessage(UUID characteristicUuid) {
        return "missing '" + BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristicUuid) + "' (" + characteristicUuid + ") CHR!";
    }

    class GattInteractionInternalWrappedCallback {
        private final GattInteractionCallback delegate;

        GattInteractionInternalWrappedCallback(GattInteractionCallback delegate) {
            this.delegate = delegate;
        }

        boolean stillInterested() {
            return delegate.stillInterested();
        }

        void onFail(int errorCode, String failMessage) {
            genericOnFail(errorCode, failMessage, delegate::onFail, false);
        }

        void onCharacteristicReadFailed(int errorCode, String failMessage) {
            remainingCharsReadRequests.clear();
            genericOnFail(errorCode, failMessage, delegate::onCharacteristicReadFailed, true);
        }

        void onCharacteristicWriteFailed(int errorCode, String failMessage) {
            remainingCharsWriteRequests.clear();
            genericOnFail(errorCode, failMessage, delegate::onCharacteristicWriteFailed, true);
        }

        void onDescriptorReadFailed(int errorCode, String failMessage) {
            remainingDescReadRequests.clear();
            genericOnFail(errorCode, failMessage, delegate::onDescriptorReadFailed, true);
        }

        void onDescriptorWriteFailed(int errorCode, String failMessage) {
            remainingDescWriteRequests.clear();
            genericOnFail(errorCode, failMessage, delegate::onDescriptorWriteFailed, true);
        }

        void onMtuChangeFailed(int errorCode, String failMessage) {
            genericOnFail(errorCode, failMessage, delegate::onMtuChangeFailed, true);
        }

        private void genericOnFail(int errorCode,
                                   String failMessage,
                                   Action3<SynchronousBleGatt, Integer, String> failAction, boolean goToIdle) {
            sessionErrorCode = errorCode;
            appLog.we(failMessage, errorCode);
            if (checkTerminalState()) {
                if (goToIdle) {
                    // proceed to idle state so that the callback invocation can do 'something'
                    makeSureIdle();
                }
                // invoke the callback now
                failAction.call(getSyncGatt(), errorCode, failMessage);
            }
        }

        void onCharacteristicReadComplete() {
            if (checkTerminalState()) delegate.onCharacteristicReadComplete(getSyncGatt());
        }

        void onDescriptorReadComplete() {
            if (checkTerminalState()) delegate.onDescriptorReadComplete(getSyncGatt());
        }

        void onDescriptorWriteComplete() {
            if (checkTerminalState()) delegate.onDescriptorWriteComplete(getSyncGatt());
        }

        void onCharacteristicWriteComplete() {
            if (checkTerminalState()) delegate.onCharacteristicWriteComplete(getSyncGatt());
        }

        void onCharacteristicChanged(BleGattCharacteristic characteristic, byte[] value) {
            if (checkTerminalState()) delegate.onCharacteristicChanged(getSyncGatt(), characteristic, value);
        }

        void onMtuChangeComplete() {
            if (checkTerminalState()) delegate.onMtuChangeComplete(getSyncGatt());
        }

        private boolean checkTerminalState() {
            if (isTerminate()) {
                log.d("breaking application callback invocation");
                return false;
            }
            return true;
        }

    }

    @Override
    public void initiateConnect(GattInteractionCallback gattInteractionCallback) {
        // set a wrapped (logging) session callback
        this.gattInteractionCallbackWrapped = new GattInteractionInternalWrappedCallback(gattInteractionCallback);
        // setup lifecycle of the FSM
        init();
        // do transition to CONNECTING
        setState(GattInteractionState.CONNECTING);
    }

    @Override
    public void initiateDisconnect() {
        //
        setState(GattInteractionState.DISCONNECTING);
    }

    private void init() {
        addOnStateEnteredHandler(GattInteractionState.CONNECTING, fromState -> {
            // initiate a connection to the device
            if (Constants.DEBUG && debugLoggingEnabled) {
                appLog.d("initiating connection");
            }
            connectionListener.onConnecting(bleAddress);
            //
            bleGatt = bleDevice.connect(systemGattCallback);
            //
            if (bleGatt == null) {
                // serious GATT problem (e.g. airplane mode)
                // notify callback and go to DISCONNECTED, asap
                scheduleRunnable(() -> {
                    gattInteractionCallbackWrapped.onFail(ErrorCode.GATT_BROKEN, "broken Android GATT API, connect() should never return null");
                    setState(GattInteractionState.DISCONNECTED);
                });
                return;
            }
            // schedule disconnect timeout
            scheduleOnCurrentStateKeptRunnable(() -> {
                gattInteractionCallbackWrapped.onFail(ErrorCode.BLE_CONNECT_TIMEOUT, "connect timeout elapsed, disconnecting");
                // notify internally that connect attempt failed
                connectionListener.onConnectFailed(bleAddress, this);
                setState(GattInteractionState.DISCONNECTING);
            }, BleConstants.CONNECT_TIMEOUT, null);
        });
        addOnStateEnteredHandler(GattInteractionState.JUST_CONNECTED, fromState -> {
            // do service discovery right after we are connected
            if (debugLoggingEnabled) {
                appLog.d("just connected, scheduling service discovery (600ms pause)");
            }
            // notify internally that we are connected
            connectionListener.onConnected(bleAddress, this, getSyncGatt());
            scheduleOnCurrentStateKeptRunnable(() -> {
                if (debugLoggingEnabled) {
                    appLog.d("discovering service (600ms pause)");
                }
                // initiate service discovery
                if (bleGatt.discoverServices()) {
                    // set appropriate state
                    setState(GattInteractionState.DISCOVERING_SERVICES);
                    // wait for the gattInteractionCallback to be invoked
                } else {
                    // we are disconnecting unconditionally - there's nothing to do if services could not be discovered
                    gattInteractionCallbackWrapped.onFail(ErrorCode.GATT_FAILED_SERVICE_DISCOVERY, "failed to start service discovery");
                    setState(GattInteractionState.DISCONNECTING);
                }
            }, 600, null);
        });
        addOnStateEnteredHandler(GattInteractionState.SERVICES_DISCOVERED, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("services discovered, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // update the last-seen
                networkNodeManager.updateLastSeen(bleAddress);
                // notify the internal listener
                connectionListener.onServicesDiscovered(bleAddress, this, getSyncGatt());
            });
        });
        addOnStateEnteredHandler(GattInteractionState.DESCRIPTORS_READ, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("DESCs read, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // notify the callback (and let it decide what to do next)
                gattInteractionCallbackWrapped.onDescriptorReadComplete();
        });
        });
        addOnStateEnteredHandler(GattInteractionState.MTU_CHANGED, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("MTU changed, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // notify the callback (and let it decide what to do next)
                gattInteractionCallbackWrapped.onMtuChangeComplete();
        });
        });
        addOnStateEnteredHandler(GattInteractionState.DESCRIPTORS_WRITTEN, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("DESCs written, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // notify the callback (and let it decide what to do next)
                gattInteractionCallbackWrapped.onDescriptorWriteComplete();
        });
        });
        addOnStateEnteredHandler(GattInteractionState.CHARACTERISTICS_READ, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("CHRs read, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // notify the callback (and let it decide what to do next)
                gattInteractionCallbackWrapped.onCharacteristicReadComplete();
        });
        });
        addOnStateEnteredHandler(GattInteractionState. CHARACTERISTICS_WRITTEN, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("CHRs written, letting know the callback");
            }
            scheduleRunnableForCurrentState(() -> {
                // notify the callback (and let it decide what to do next)
                gattInteractionCallbackWrapped.onCharacteristicWriteComplete();
        });
        });
        addOnStateEnteredHandler(GattInteractionState.WRITING_CHARACTERISTICS, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("initiating CHRs write");
            }
            scheduleRunnableForCurrentState(this::initiateNextCharacteristicWrite);
        });
        addOnStateEnteredHandler(GattInteractionState.CHANGING_MTU, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("initiating MTU change");
            }
            scheduleRunnableForCurrentState(this::initiateMtuChange);
        });
        addOnStateEnteredHandler(GattInteractionState.WRITING_DESCRIPTORS, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("initiating DESCs write");
            }
            scheduleRunnableForCurrentState(() -> {
                if (remainingDescWriteRequests.size() > 0) {
                    initiateNextDescriptorWrite();
                } else {
                    // move directly to DESC_WRITTEN
                    setState(GattInteractionState.DESCRIPTORS_WRITTEN);
                }
            });
        });
        addOnStateEnteredHandler(GattInteractionState.READING_CHARACTERISTICS, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("initiating CHRs read");
            }
            scheduleRunnableForCurrentState(this::initiateNextCharacteristicRead);
        });
        addOnStateEnteredHandler(GattInteractionState.READING_DESCRIPTORS, fromState -> {
            if (debugLoggingEnabled) {
                appLog.d("initiating DESCs read");
            }
            scheduleRunnableForCurrentState(this::initiateNextDescriptorRead);
        });
        addOnStateEnteredHandler(fromState -> {
            // do the work now
            if (debugLoggingEnabled) {
                appLog.d("initiating disconnect");
            }
            // let the connect listener know
            connectionListener.onDisconnecting(bleAddress);
            // we need to reset/unset the characteristics to load/write whenever we are going to disconnect
            remainingCharsReadRequests = null;
            remainingCharsWriteRequests = null;
            // simply initiate disconnect
            bleGatt.disconnect();
            // the onConnectionStateChange callback sometimes doesn't get called (the device does not disconnect?)
            // make sure that at least our disconnection sequence happens properly
            scheduleOnCurrentStateKeptRunnable(() -> {
                if (sessionErrorCode == null) {
                    sessionErrorCode = ErrorCode.GATT_DISCONNECT_TIMEOUT;
                }
                appLog.we("disconnect request timed-out", ErrorCode.GATT_DISCONNECT_TIMEOUT);
                setState(GattInteractionState.DISCONNECTED);
            }, FAKE_DISCONNECT_CALLBACK_DELAY, null);
        }, GattInteractionState.DISCONNECTING);
        // schedule timeout in DISCOVERING_SERVICES
        addOnStateEnteredHandler(GattInteractionState.DISCOVERING_SERVICES, fromState -> scheduleOnCurrentStateKeptRunnable(() -> {
            if (bleGatt.getServices().isEmpty()) {
                if (debugLoggingEnabled) {
                    appLog.d("DISCOVERING_SERVICES: first timeout, scheduling next timeout");
                }
                scheduleOnCurrentStateKeptRunnable(() -> {
                    if (bleGatt.getServices().isEmpty()) {
                        // we are not going to idle - we going to DISCONNECTING
                        gattInteractionCallbackWrapped.onFail(ErrorCode.GATT_FAILED_SERVICE_DISCOVERY, "service discovery timed-out");
                        setState(GattInteractionState.DISCONNECTING);
                    } else {
                        if (debugLoggingEnabled) {
                            appLog.d("DISCOVERING_SERVICES: services are there finally, doing fake callback notification");
                        }
                        doOnServicesDiscovered(BleStatus.GATT_SUCCESS);
                    }
                }, BleConstants.SECOND_SERVICE_DISCOVERY_TIMEOUT, null);
            } else {
                if (debugLoggingEnabled) {
                    appLog.d("DISCOVERING_SERVICES: services are already there, doing fake callback notification");
                }
                doOnServicesDiscovered(BleStatus.GATT_SUCCESS);
            }
        }, BleConstants.FIRST_SERVICE_DISCOVERY_TIMEOUT, null));
        //
        addOnStateEnteredHandler(fromState -> {
            // let the device know (so that it can manage it's internal state properly)
            String address = bleAddress;
            if (sessionErrorCode == null) {
                // this was proper/acknowledged disconnect - let the network node manager know about the last seen time
                networkNodeManager.updateLastSeen(address);
            }
            // resource cleanup, throw away the reference to GATT server and close the BT-GATT
            if (debugLoggingEnabled) {
                appLog.d("resource cleanup");
            }
            // it might happen that bleGatt is null - which means a serious BLE stack problem
            if (bleGatt != null) bleGatt.close();
            // stop scheduled tasks - do not execute them
            stop();
            // let the connect listener know
            connectionListener.onDisconnected(address, sessionErrorCode);
        }, GattInteractionState.DISCONNECTED);
    }

}
