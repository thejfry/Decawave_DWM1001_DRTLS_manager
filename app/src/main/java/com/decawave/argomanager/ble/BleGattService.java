/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import java.util.List;
import java.util.UUID;

/**
 * BEL GATT service representation.
 */
public interface BleGattService {

    UUID getUuid();

    BleGattCharacteristic getCharacteristic(UUID characteristicUuid);

    List<BleGattService> getIncludedServices();

}
