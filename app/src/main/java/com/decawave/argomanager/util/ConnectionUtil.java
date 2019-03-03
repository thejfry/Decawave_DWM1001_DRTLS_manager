/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.ConnectPriority;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;

import static com.decawave.argomanager.ArgoApp.uiHandler;

/**
 * Argo project.
 */

public class ConnectionUtil {

    private static final int RETRY_INTERVAL = 1000;

    public static void connectAndUpdate(BleConnectionApi bleConnectionApi,
                                        String bleAddress, int attemptCount,
                                        Supplier<NetworkNode> updateNodeSupplier,
                                        Action1<NetworkNodeConnection> connectionListener,
                                        Action2<NetworkNodeConnection.WriteEffect,NetworkNode> onSuccess,
                                        Action1<Fail> onFail, Action1<Integer> onFinished) {
        // just set up the initial counter value
        _connectAndUpdate(bleConnectionApi, bleAddress, 1, attemptCount, new NetworkNode[] { null }, updateNodeSupplier, connectionListener, onSuccess, onFail, onFinished);
    }

    private static void _connectAndUpdate(BleConnectionApi bleConnectionApi,
                                          String bleAddress,
                                          int counter,
                                          int attemptCount,
                                          NetworkNode[] node,
                                          Supplier<NetworkNode> updateNodeSupplier,
                                          Action1<NetworkNodeConnection> connectionListener,
                                          Action2<NetworkNodeConnection.WriteEffect, NetworkNode> onSuccess,
                                          Action1<Fail> onFail, Action1<Integer> onFinished) {
        boolean[] failure = { false };
        NetworkNodeConnection aConnection = bleConnectionApi.connect(bleAddress, ConnectPriority.HIGH, connection -> {
            // on connected
            if (node[0] == null) {
                node[0] = updateNodeSupplier.get();
            }
            connection.updateOtherSideEntity(node[0], false, writeEffect -> {
                // on success
                onSuccess.call(writeEffect, node[0]);
                // disconnect
                connection.disconnect();
            }, fail -> {
                // on fail
                handleFail(fail, counter, attemptCount, onFail, failure);
                // we will get disconnected automatically
            });
        }, (networkNodeConnection, fail) -> {
            // on fail
            handleFail(fail, counter, attemptCount, onFail, failure);
            // we will get disconnected automatically
        }, (networkNodeConnection,errCode) -> {
            // on disconnected
            if (failure[0] && counter < attemptCount) {
                // try again
                uiHandler.postDelayed(() -> _connectAndUpdate(bleConnectionApi, bleAddress, counter + 1, attemptCount,
                        node, updateNodeSupplier, connectionListener, onSuccess, onFail, onFinished), RETRY_INTERVAL);
            } else {
                onFinished.call(errCode);
            }
        });
        // let the connection listener know (each time a new connection attempt is made)
        connectionListener.call(aConnection);
    }

    private static void handleFail(Fail fail, int counter, int attemptCount, Action1<Fail> onFail, boolean[] failure) {
        if (counter == attemptCount) {
            onFail.call(fail);
        } else {
            failure[0] = true;
        }
    }

}
