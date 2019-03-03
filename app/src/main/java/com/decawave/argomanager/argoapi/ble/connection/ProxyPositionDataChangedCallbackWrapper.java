/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.interaction.ProxyPosition;

import java.util.List;

/**
 * Argo project.
 */

class ProxyPositionDataChangedCallbackWrapper implements NetworkNodeConnection.ProxyPositionDataChangedCallback {
    // delegate
    private final NetworkNodeConnection.ProxyPositionDataChangedCallback delegate;

    ProxyPositionDataChangedCallbackWrapper(NetworkNodeConnection.ProxyPositionDataChangedCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onStarted() {
        delegate.onStarted();
    }

    @Override
    public void onFail(Fail fail) {
        delegate.onFail(fail);
    }

    @Override
    public void onChange(List<ProxyPosition> newData) {
        delegate.onChange(newData);
    }

    @Override
    public void onStopped() {
        delegate.onStopped();
    }
}
