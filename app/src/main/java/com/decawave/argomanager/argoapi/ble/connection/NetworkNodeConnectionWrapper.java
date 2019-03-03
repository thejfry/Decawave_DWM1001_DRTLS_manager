/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import android.support.annotation.NonNull;

import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.argoapi.ble.GattInteractionCallback;
import com.decawave.argomanager.ble.BleGattCharacteristic;
import com.decawave.argomanager.ble.ConnectionSpeed;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Argo project.
 */

class NetworkNodeConnectionWrapper implements NetworkNodeBleConnection {
    private NetworkNodeBleConnectionImpl delegate;
    private GattInteractionCallback gattCallback;
    private final String bleAddress;
    // state
    private Boolean disconnectOnProblem;
    private ConnectionSpeed connectionSpeed;
    private ConnectionState injectedState;
    // caused by disconnect being called in PENDING or CONNECTING state
    private boolean disconnectRequired;

    NetworkNodeConnectionWrapper(String bleAddress) {
        this.bleAddress = bleAddress;
    }

    void setInjectedState(ConnectionState injectedState) {
        this.injectedState = injectedState;
    }

    void setDelegate(@NotNull NetworkNodeBleConnectionImpl establishedConnection) {
        if (Constants.DEBUG) {
            Preconditions.checkState(!disconnectRequired, "we should not get called/connected!");
        }
        this.delegate = establishedConnection;
        // propagate the aggregated flags
        if (disconnectOnProblem != null) this.delegate.setDisconnectOnProblem(disconnectOnProblem);
        if (connectionSpeed != null) this.delegate.setConnectionSpeed(connectionSpeed);
    }

    public NetworkNodeBleConnectionImpl getDelegate() {
        return delegate;
    }

    GattInteractionCallback asGattCallback() {
        // give precedence to delegate
        return delegate == null ? gattCallback : delegate.asGattCallback();
    }

    void onCharacteristicChanged(BleGattCharacteristic characteristic, byte[] value) {
        delegate.onCharacteristicChanged(characteristic, value);
    }

    @Override
    public String getOtherSideAddress() {
        return bleAddress;
    }

    @Override
    public boolean isConnected() {
        if (delegate != null) return delegate.isConnected();
        // else:
        return asConnected(injectedState);
    }

    static boolean asConnected(ConnectionState state) {
        switch (state) {
            case PENDING:
            case CLOSED:
            case CONNECTING:
                return false;
            case CONNECTED:
            case DISCONNECTING:
                return true;
            default:
                throw new IllegalStateException("unexpected state: " + state);
        }
    }

    @NonNull
    @Override
    public ConnectionState getState() {
        if (delegate != null) return delegate.getState();
        // else:
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(injectedState, "injected state is null!");
        }
        return injectedState;
    }

    @Override
    public void getOtherSideEntity(@NotNull Action1<NetworkNode> onSuccess,
                                   @NotNull Action1<Fail> onFail,
                                   NetworkNodeProperty... properties) {
        if (delegate != null) delegate.getOtherSideEntity(onSuccess, onFail, properties);
        else throw new IllegalStateException("cannot get other side entity if just connecting!");
    }

    @Override
    public YesNoAsync isUploadingFirmware() {
        if (delegate != null) return delegate.isUploadingFirmware();
        // else:
        return YesNoAsync.NO;
    }

    @Override
    public boolean isDisconnected() {
        if (delegate != null) return delegate.isDisconnected();
        // else:
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(injectedState, "injected state is null, we cannot tell if we are disconnected");
        }
        return injectedState.disconnected;
    }

    @Override
    public void updateOtherSideEntity(NetworkNode networkNode,
                                      boolean operationModeChange,
                                      Action1<WriteEffect> onSuccess,
                                      Action1<Fail> onFail) {
        if (delegate != null) delegate.updateOtherSideEntity(networkNode, operationModeChange, onSuccess, onFail);
        else throw new IllegalStateException("cannot update other side entity if just connecting!");
    }

    @Override
    public void uploadFirmware(FirmwareMeta firmwareMeta,
                               InputStream firmwareData,
                               @Nullable Action0 onSuccessCallback,
                               @Nullable Action1<Integer> progressListener,
                               @Nullable Action1<Fail> onFailCallback) {
        if (delegate != null) delegate.uploadFirmware(firmwareMeta, firmwareData, onSuccessCallback, progressListener, onFailCallback);
        else throw new IllegalStateException("cannot upload firmware if just connecting!");
    }

    @Override
    public void observeLocationData(LocationDataChangedCallback locationDataChangedCallback) {
        if (delegate != null) delegate.observeLocationData(locationDataChangedCallback);
        else throw new IllegalStateException("cannot observe location data if just connecting!");
    }

    @Override
    public void stopObserveLocationData() {
        if (delegate != null) delegate.stopObserveLocationData();
        else throw new IllegalStateException("cannot stop location data observe if just connecting!");
    }

    @Override
    public YesNoAsync isObservingLocationData() {
        if (delegate != null) return delegate.isObservingLocationData();
        // else:
        return YesNoAsync.NO;
    }

    @Override
    public void observeProxyPositionData(ProxyPositionDataChangedCallback positionChangedCallback) {
        if (delegate != null) delegate.observeProxyPositionData(positionChangedCallback);
        else throw new IllegalStateException("cannot observe proxy position data if just connecting!");
    }

    @Override
    public void stopObserveProxyPositionData() {
        if (delegate != null) delegate.stopObserveProxyPositionData();
        else throw new IllegalStateException("cannot stop proxy position data observe if just connecting!");
    }

    @Override
    public YesNoAsync isObservingProxyPositionData() {
        if (delegate != null) return delegate.isObservingProxyPositionData();
        // else:
        return YesNoAsync.NO;
    }

    @Override
    public void disconnect() {
        if (delegate != null) delegate.disconnect();
        else {
            // request to disconnect a PENDING/CONNECTING connection
            disconnectRequired = true;
        }
    }

    /**
     * Use to query if there was a disconnect request while the connection has not been established yet.
     */
    boolean isDisconnectRequired() {
        return disconnectRequired;
    }

    @Override
    public void setDisconnectOnProblem(boolean disconnect) {
        if (delegate != null) delegate.setDisconnectOnProblem(disconnect);
        else disconnectOnProblem = disconnect;
    }

    @Override
    public void setConnectionSpeed(@NonNull ConnectionSpeed connectionSpeed) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(connectionSpeed);
        }
        if (delegate != null) delegate.setConnectionSpeed(connectionSpeed);
        else this.connectionSpeed = connectionSpeed;
    }

    @Override
    public void changeMtu(int mtu, Action0 onSuccessCallback, Action1<Fail> onFailCallback) {
        if (Constants.DEBUG) {
            Preconditions.checkState(mtu >= 23);
        }
        if (delegate != null) delegate.changeMtu(mtu, onSuccessCallback, onFailCallback);
        else throw new IllegalStateException("cannot change MTU on non-connected connection!");
    }

    @Override
    public String toString() {
        return "NetworkNodeConnectionWrapper{" +
                "delegate=" + delegate +
                ", bleAddress='" + bleAddress + '\'' +
                ", disconnectOnProblem=" + disconnectOnProblem +
                ", injectedState=" + injectedState +
                '}';
    }

    // workaround to be used by an already running but not yet fully initialized connection,
    // this is used when connection is made, services are already discovered, but initial asynchronous operations have
    // not been executed yet, this means - delegate is not set yet
    void setGattCallback(GattInteractionCallback gattInteractionCallback) {
        this.gattCallback = gattInteractionCallback;
    }
}
