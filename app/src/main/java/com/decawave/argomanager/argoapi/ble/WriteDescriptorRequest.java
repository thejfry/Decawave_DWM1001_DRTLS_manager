/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import java.util.UUID;

/**
 * Extracted elementary write request.
 */
public class WriteDescriptorRequest {
    public final UUID serviceUuid;
    public final UUID characteristicUuid;
    @SuppressWarnings("WeakerAccess")
    public final UUID descriptorUuid;
    public final byte[] value;

    public WriteDescriptorRequest(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] value) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.descriptorUuid = descriptorUuid;
        this.value = value;
    }

}
