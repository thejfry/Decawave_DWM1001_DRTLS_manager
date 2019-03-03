/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import java.util.UUID;

/**
 * Argo project.
 */
public interface BleGattDescriptor {

    UUID getUuid();

    BleGattCharacteristic getCharacteristic();

    boolean setValue(byte[] value);

    byte[] getValue();

}
