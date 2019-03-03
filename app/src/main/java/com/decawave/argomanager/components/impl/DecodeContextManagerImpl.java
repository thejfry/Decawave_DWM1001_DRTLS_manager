/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.components.DecodeContextManager;
import com.decawave.argomanager.util.gatt.GattDecodeContext;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Argo project.
 */
public class DecodeContextManagerImpl implements DecodeContextManager {
    private Map<String, GattDecodeContext> map;

    @Inject
    DecodeContextManagerImpl() {
        this.map = new HashMap<>();
    }

    @Override
    @NotNull public GattDecodeContext getOrCreateContext(String bleDeviceAddress) {
        GattDecodeContext r = map.get(bleDeviceAddress);
        if (r == null) {
            map.put(bleDeviceAddress, r = new GattDecodeContext());
        }
        return r;
    }

}
