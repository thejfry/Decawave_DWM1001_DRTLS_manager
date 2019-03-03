/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble;

import com.decawave.argo.api.struct.Position;

import java.util.UUID;

/**
 * BLE GATT characteristic representation.
 */
public interface BleGattCharacteristic {

    UUID getUuid();

    byte[] getValue();

    String getStringValue();

    Integer getIntValue();

    BleGattService getService();

    boolean setStringValue(String value);

    boolean setByteValue(byte[] value);

    boolean setIntValue(Integer value);

    boolean setShortValue(Short value);

    boolean setBooleanValue(Boolean value);

    boolean setUuidValue(UUID uuid);

    boolean setPositionValue(Position value);

    boolean valueLoaded();

    boolean emptyValue();

    BleGattDescriptor getDescriptor(UUID descriptor);

    void setWriteType(WriteType writeType);

}
