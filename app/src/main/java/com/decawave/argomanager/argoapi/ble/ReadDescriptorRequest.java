/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import java.util.UUID;

/**
 * Extracted elementary read descriptor request.
 */
class ReadDescriptorRequest {
    final UUID serviceUuid;
    final UUID characteristicUuid;
    final UUID descriptorUuid;
    // for now, all descriptors are mandatory
    final boolean mandatory;

    ReadDescriptorRequest(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.descriptorUuid = descriptorUuid;
        this.mandatory = true;
    }


}
