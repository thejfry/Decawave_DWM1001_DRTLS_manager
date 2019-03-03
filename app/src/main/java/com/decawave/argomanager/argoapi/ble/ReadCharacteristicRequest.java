/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Extracted elementary read request.
 */
public class ReadCharacteristicRequest {
    public @NotNull final UUID serviceUuid;
    public @NotNull final UUID characteristicUuid;
    // for now, all characteristics are mandatory
    final boolean mandatory;

    public ReadCharacteristicRequest(@NotNull UUID serviceUuid, @NotNull UUID characteristicUuid) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.mandatory = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReadCharacteristicRequest that = (ReadCharacteristicRequest) o;

        //noinspection SimplifiableIfStatement
        if (!serviceUuid.equals(that.serviceUuid)) return false;
        return characteristicUuid.equals(that.characteristicUuid);

    }

    @Override
    public int hashCode() {
        int result = serviceUuid.hashCode();
        result = 31 * result + characteristicUuid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ReadCharacteristicRequest{" +
                "serviceUuid=" + serviceUuid +
                ", characteristic=" + BleConstants.MAP_CHARACTERISTIC_TITLE.get(characteristicUuid) +
                ", mandatory=" + mandatory +
                '}';
    }
}
