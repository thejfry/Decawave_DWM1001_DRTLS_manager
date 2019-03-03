/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager;

import com.decawave.argo.api.interaction.Fail;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.argoapi.ble.SynchronousBleGatt;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import java.util.Comparator;

import rx.functions.Action1;
import rx.functions.Action2;

/**
 *
 */

@SuppressWarnings("WeakerAccess")
public class Constants {

    // adjust as necessary
    public static final boolean ENFORCE_CRASH_REPORTS = false;
    public static final boolean ENFORCE_DEBUG_LOGGING_AND_ASSERTS = false;
    public static final boolean ENFORCE_DEBUG_UI = false;

    // std values
    public static final boolean DEBUG = BuildConfig.DEBUG || ENFORCE_DEBUG_LOGGING_AND_ASSERTS;
    public static final boolean DEBUG_UI = BuildConfig.DEBUG || ENFORCE_DEBUG_UI;
    @SuppressWarnings("WeakerAccess")
    public static final boolean CRASH_REPORTS_ENABLED = !BuildConfig.DEBUG || ENFORCE_CRASH_REPORTS;

    ///////////////////////////////////////////////////////////////////////////
    // self-documentation
    ///////////////////////////////////////////////////////////////////////////
    public static final Action1<NetworkNode> VOID_NETWORK_NODE_ACTION = (node) -> {};
    public static final Action1<SynchronousBleGatt> VOID_BLE_GATT_ACTION = (node) -> {};
    public static final Action2<SynchronousBleGatt, Fail> VOID_BLE_GATT_FAIL = (bleGatt, node) -> {};
    public static final Action2<NetworkNodeConnection, Integer> VOID_ON_DISCONNECT = (nnc, err) -> {};
    public static final Action1<Fail> VOID_ON_FAIL = (fail) -> {};
    public static final Action2<NetworkNodeConnection,Fail> VOID_ON_CONNECTION_FAIL = (connection,fail) -> {};

    ///////////////////////////////////////////////////////////////////////////
    // network node comparator for ordering in UI
    public static final Comparator<NetworkNodeEnhanced> NETWORK_NODE_COMPARATOR = (n1, n2) -> {
        // tags first
        int i = n1.asPlainNode().getType().ordinal() - n2.asPlainNode().getType().ordinal();
        if (i != 0) {
            return i;
        }
        // same type sort by label
        i = n1.asPlainNode().getLabel().compareTo(n2.asPlainNode().getLabel());
        if (i != 0) {
            return i;
        }
        // compare by BLE address
        return n1.getBleAddress().compareTo(n2.getBleAddress());
    };
}
