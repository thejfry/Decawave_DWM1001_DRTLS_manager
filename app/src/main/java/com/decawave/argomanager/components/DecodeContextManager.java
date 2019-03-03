/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.util.gatt.GattDecodeContext;

import org.jetbrains.annotations.NotNull;

/**
 * Argo project.
 */

public interface DecodeContextManager {

    @NotNull GattDecodeContext getOrCreateContext(String bleDeviceAddress);

}
