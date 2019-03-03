/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.Position;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


/**
 * Argo project.
 */

public class WriteCharacteristicRequestsBuilder {
    private List<WriteCharacteristicRequest> requests;
    //
    private UUID currentServiceId;

    public WriteCharacteristicRequestsBuilder() {
        this.requests = new LinkedList<>();
    }

    public WriteCharacteristicRequestsBuilder setService(UUID serviceId) {
        currentServiceId = serviceId;
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, String value) {
        requests.add(new WriteCharacteristicRequest.Text(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, byte[] value) {
        requests.add(new WriteCharacteristicRequest.ByteArray(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder prependValue(UUID characteristicId, Supplier<byte[]> valueProvider) {
        requests.add(0, new WriteCharacteristicRequest.LazyByteArray(currentServiceId, characteristicId, valueProvider));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, Position value) {
        requests.add(new WriteCharacteristicRequest.Position(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, UUID value) {
        requests.add(new WriteCharacteristicRequest.Uuid(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, Integer value) {
        requests.add(new WriteCharacteristicRequest.Number(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, Short value) {
        requests.add(new WriteCharacteristicRequest.ShortNumber(currentServiceId, characteristicId, value));
        return this;
    }

    public WriteCharacteristicRequestsBuilder addValue(UUID characteristicId, Boolean value) {
        requests.add(new WriteCharacteristicRequest.Boolean(currentServiceId, characteristicId, value));
        return this;
    }

    public List<WriteCharacteristicRequest> build() {
        List<WriteCharacteristicRequest> r = this.requests;
        requests = new LinkedList<>();
        return r;
    }

}
