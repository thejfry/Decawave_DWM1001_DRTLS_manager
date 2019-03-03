/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.exception;

import com.decawave.argomanager.argoapi.ble.BleConstants;

import java.util.UUID;

/**
 * Represents problem while decoding GATT representation.
 */
public class GattCharacteristicDecodeException extends GattRepresentationException {
    private final UUID characteristicUuid;
    private final int expectedSize;
    private final int expectedSize2;
    private final Type type;
    private final boolean serviceDataInAdvertisement;
    private final int actualSize;

    private enum Type {
        EXACT,
        MULTIPLICATION,
        MINIMAL,
        REMAINING_MULTIPLICATION,
    }

    private GattCharacteristicDecodeException(Throwable cause,
                                              String deviceBleAddress,
                                              UUID characteristicUuid,
                                              Type type,
                                              int expectedSize,
                                              int actualSize,
                                              boolean serviceDataInAdvertisement) {
        this(cause, deviceBleAddress, characteristicUuid, type, expectedSize, -1, actualSize, serviceDataInAdvertisement);
    }

    private GattCharacteristicDecodeException(Throwable cause,
                                              String deviceBleAddress,
                                              UUID characteristicUuid,
                                              Type type,
                                              int expectedSize,
                                              int expectedSize2,
                                              int actualSize,
                                              boolean serviceDataInAdvertisement) {
        super(deviceBleAddress, cause);
        this.characteristicUuid = characteristicUuid;
        this.type = type;
        this.expectedSize = expectedSize;
        this.expectedSize2 = expectedSize2;
        this.actualSize = actualSize;
        this.serviceDataInAdvertisement = serviceDataInAdvertisement;
    }

    public static GattCharacteristicDecodeException newMinimalCharacteristicLength(String deviceBleAddress,
                                                                                   UUID characteristicUuid,
                                                                                   int expectedSize,
                                                                                   int actualSize) {
        return new GattCharacteristicDecodeException(null, deviceBleAddress, characteristicUuid, Type.MINIMAL, expectedSize, actualSize, false);
    }

    public static GattCharacteristicDecodeException newRemainingCharacteristicMultiplicationLength(String deviceBleAddress,
                                                                                   UUID characteristicUuid,
                                                                                   int multiplicationOf,
                                                                                   int actualRemaining) {
        return new GattCharacteristicDecodeException(null, deviceBleAddress, characteristicUuid, Type.REMAINING_MULTIPLICATION, multiplicationOf, actualRemaining, false);
    }

    public static GattCharacteristicDecodeException newExactCharacteristicLength(String deviceBleAddress,
                                                                                   UUID characteristicUuid,
                                                                                   int expectedSize,
                                                                                   int actualSize) {
        return new GattCharacteristicDecodeException(null, deviceBleAddress, characteristicUuid, Type.EXACT, expectedSize, actualSize, false);
    }

    public static GattCharacteristicDecodeException newExactCharacteristicLength(String deviceBleAddress,
                                                                                   UUID characteristicUuid,
                                                                                   int expectedSize1,
                                                                                   int expectedSize2,
                                                                                   int actualSize) {
        return new GattCharacteristicDecodeException(null, deviceBleAddress, characteristicUuid, Type.EXACT, expectedSize1, expectedSize2, actualSize, false);
    }

    /**
     * Constructor for GATT decoding exception in advertisement.
     */
    public static GattCharacteristicDecodeException newExactCharacteristicLength(String deviceBleAddress,
                                                                                 int expectedSize,
                                                                                 int actualSize) {
        return new GattCharacteristicDecodeException(null, deviceBleAddress, null, Type.EXACT, expectedSize, actualSize, true);
    }

    @Override
    public String getMessage() {
        String expectedSizeStr;
        if (serviceDataInAdvertisement) {
            return "problem while decoding Service Data in advertisement, expecting " + expectedSize + " bytes, recieved "
                    + actualSize + " instead";
        } // else:
        switch (type) {
            case EXACT:
                expectedSizeStr = "" + this.expectedSize;
                if (this.expectedSize2 != -1) {
                    expectedSizeStr += " or " + this.expectedSize2;
                }
                break;
            case MULTIPLICATION:
            case REMAINING_MULTIPLICATION:
                expectedSizeStr = "multiplication of " + this.expectedSize;
                break;
            case MINIMAL:
                expectedSizeStr = "at least " + this.expectedSize;
                break;
            default:
                throw new IllegalStateException("unexpected type: " + type);
        }
        // build the message
        return "problem while decoding " + BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristicUuid) + " characteristic (" + characteristicUuid + "), "
                + "expecting " + expectedSizeStr + " bytes, " + (type == Type.REMAINING_MULTIPLICATION ? "remaining " : "received ") + actualSize + " instead";
    }

    public UUID getCharacteristicUuid() {
        return characteristicUuid;
    }

    public int getExpectedSize() {
        return expectedSize;
    }

    public int getActualSize() {
        return actualSize;
    }

}
