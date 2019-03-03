/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api;

/**
 * Connection state.
 *
 * in progress = not CLOSED
 */
public enum ConnectionState {
    PENDING(true, true),
    CONNECTING(false, true),
    CONNECTED(false, true),
    DISCONNECTING(false, true),
    CLOSED(true, false);

    public final boolean disconnected;

    public final boolean inProgress;

    ConnectionState(boolean disconnected, boolean inProgress) {
        this.disconnected = disconnected;
        this.inProgress = inProgress;
    }
}
