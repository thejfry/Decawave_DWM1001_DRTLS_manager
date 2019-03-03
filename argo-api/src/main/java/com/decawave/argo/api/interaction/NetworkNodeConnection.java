/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.interaction;

import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.YesNoAsync;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;

/**
 * Abstraction of a network node connection.
 */
public interface NetworkNodeConnection  {

    String getOtherSideAddress();

    /**
     * Whether this connection is still established/CONNECTED.
     * @return true if so, false otherwise
     */
    boolean isConnected();

    /**
     * The connection state is slightly different from GattInteractionFsm:
     * CONNECTED is not after connection is confirmed, but after services
     * are discovered and node type is fetched.
     *
     * @return connection state
     */
    @NotNull ConnectionState getState();

    /**
     * Retrieve the other side entities with the restricted property set.
     * @param properties restricts property set, all properties are fetched if empty
     */
    void getOtherSideEntity(@NotNull Action1<NetworkNode> onSuccess, @NotNull Action1<Fail> onFail,
                            NetworkNodeProperty... properties);

    YesNoAsync isUploadingFirmware();

    /**
     * @return whether the connection is in disconnected state, this is PENDING or DISCONNECTED states
     */
    boolean isDisconnected();

    enum WriteEffect {
        WRITE_SKIPPED,
        WRITE_IMMEDIATE_EFFECT,
        WRITE_DELAYED_EFFECT
    }

    /**
     * Update the entity on the other side according to what's saved in network node.
     * @param networkNode desired state of the other side entity
     * @param operationModeChange whether an operation mode has changed
     *
     * @param onSuccess callback notified in case of successfully written properties
     * @param onFail callback notified in case of fail
     */
    void updateOtherSideEntity(NetworkNode networkNode, boolean operationModeChange, Action1<WriteEffect> onSuccess, Action1<Fail> onFail);

    interface DataChangedCallback<T> {

        void onStarted();

        void onFail(Fail fail);

        void onChange(T newData);

        void onStopped();

    }

    interface LocationDataChangedCallback extends DataChangedCallback<LocationData> {

    }

    /**
     * Starts firmware upload.
     * @param firmwareData binary stream representation firmware data
     * @param onSuccessCallback invoked once the firmware has been uploaded
     * @param progressListener invoked each time a buffer is sent, notified with gonna-to-be sent bytes number (starts with 0)
     * @param onFailCallback invoked if the firmware upload failed
     */
    void uploadFirmware(FirmwareMeta firmwareMeta, InputStream firmwareData,
                        @Nullable Action0 onSuccessCallback,
                        @Nullable Action1<Integer> progressListener,
                        @Nullable Action1<Fail> onFailCallback);

    /**
     * Starts position and distances observation.
     * @param locationDataChangedCallback invoked whenever there is received a position or distance change
     */
    void observeLocationData(LocationDataChangedCallback locationDataChangedCallback);

    /**
     * Stops position and ranging anchor observation.
     * Valid only for connections where observation was previously started.
     */
    void stopObserveLocationData();

    /**
     * True if there is ongoing position observation.
     */
    YesNoAsync isObservingLocationData();

    interface ProxyPositionDataChangedCallback extends DataChangedCallback<List<ProxyPosition>> {

    }


    /**
     * Starts proxy position observation.
     * It is valid to call this routine even if it does not make sense (tag node, UWB active,...).
     *
     * @param positionChangedCallback invoked whenever there is received a position change
     */
    void observeProxyPositionData(ProxyPositionDataChangedCallback positionChangedCallback);

    /**
     * Stops proxy position observation.
     * Valid only for connections where proxy position observation was previously started.
     */
    void stopObserveProxyPositionData();

    /**
     * True if there is an ongoing proxy position observation.
     */
    YesNoAsync isObservingProxyPositionData();

    /**
     * Disconnects (and also cancels any position observation).
     * Optional callback passed to connection request is invoked as well.
     *
     * @see com.decawave.argo.api.GenericConnectionApi#connect(Object, ConnectPriority, Action1, Action2, Action2)
     */
    void disconnect();

    /**
     * Whether to disconnect automatically on reported error (other then service discovery or connection fail).
     * @param disconnect true if disconnect, false otherwise; default is true
     */
    void setDisconnectOnProblem(boolean disconnect);

}
