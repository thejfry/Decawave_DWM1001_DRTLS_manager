/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

/**
 * Discovery helper connection state.
 */
enum GattInteractionState {
    DISCONNECTED(false, false),
    CONNECTING(false, false),
    JUST_CONNECTED(true, true),
    DISCOVERING_SERVICES(true, false),
    SERVICES_DISCOVERED(true, true),
    //
    READING_CHARACTERISTICS(true, false),
    CHARACTERISTICS_READ(true, true),
    WRITING_CHARACTERISTICS(true, false),
    CHARACTERISTICS_WRITTEN(true, true),
    IDLE(true, true),
    //
    READING_DESCRIPTORS(true, false),
    DESCRIPTORS_READ(true, true),
    WRITING_DESCRIPTORS(true, false),
    DESCRIPTORS_WRITTEN(true, true),
    //
    CHANGING_MTU(true, false),
    MTU_CHANGED(true, true),
    //
    LAST_OPERATION_FAILED_CONNECTED(true,true),
    //
    DISCONNECTING(true, false);

    /**
     * whether we are connected to BLE device upon entering the state (this might change right before exiting the state)
     */
    public final boolean connected;
    public final boolean idle;

    GattInteractionState(boolean connected, boolean idle) {
        this.connected = connected;
        this.idle = idle;
    }

    public boolean terminal() {
        return this == DISCONNECTING || this == DISCONNECTED;
    }

}
