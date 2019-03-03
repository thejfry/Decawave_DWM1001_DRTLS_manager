/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.signal;

/**
 * Argo project.
 */

public interface SignalStrengthInterpreter {

    SignalStrength asSignalStrength(Integer rssi);

}
