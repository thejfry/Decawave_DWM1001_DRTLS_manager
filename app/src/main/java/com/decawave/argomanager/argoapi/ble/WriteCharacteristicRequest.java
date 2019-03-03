/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble;

import com.annimon.stream.function.Supplier;
import com.decawave.argomanager.ble.WriteType;

import java.util.UUID;

/**
 * Extracted elementary write request.
 *
 * @param <V> type of written value
 */
public abstract class WriteCharacteristicRequest<V> {
    public final UUID serviceUuid;
    public final UUID characteristicUuid;
    @SuppressWarnings("WeakerAccess")
    public final WriteType writeType;
    // value is not final so that the write request can be reused
    private V value;

    private WriteCharacteristicRequest(UUID serviceUuid, UUID characteristicUuid, WriteType writeType, V value) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.writeType = writeType;
        this.setValue(value);
    }

    public void setValue(V value) {
        this.value = value;
    }

    abstract void accept(WriteCharacteristicRequestVisitor visitor);

    public V getValue() {
        return value;
    }

    public static class LazyByteArray extends WriteCharacteristicRequest<byte[]> {
        Supplier<byte[]> valueProvider;

        LazyByteArray(UUID serviceUuid, UUID characteristicUuid, Supplier<byte[]> valueProvider) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, null);
            this.valueProvider = valueProvider;
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            // the visitor method can be reused
            visitor.visitByte(this);
        }

        @Override
        public byte[] getValue() {
            return valueProvider.get();
        }


    }

    static class Text extends WriteCharacteristicRequest<java.lang.String> {

        Text(UUID serviceUuid, UUID characteristicUuid, String value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitText(this);
        }

    }

    static class Number extends WriteCharacteristicRequest<Integer> {

        Number(UUID serviceUuid, UUID characteristicUuid, Integer value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitInteger(this);
        }
    }

    public static class ByteArray extends WriteCharacteristicRequest<byte[]> {

        @SuppressWarnings("WeakerAccess")
        public ByteArray(UUID serviceUuid, UUID characteristicUuid, byte[] value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        public ByteArray(UUID serviceUuid, UUID characteristicUuid, WriteType writeType, byte[] value) {
            super(serviceUuid, characteristicUuid, writeType, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitByte(this);
        }
    }

    static class ShortNumber extends WriteCharacteristicRequest<Short> {

        ShortNumber(UUID serviceUuid, UUID characteristicUuid, Short value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitShort(this);
        }
    }

    static class Boolean extends WriteCharacteristicRequest<java.lang.Boolean> {

        public Boolean(UUID serviceUuid, UUID characteristicUuid, java.lang.Boolean value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitBoolean(this);
        }
    }

    static class Uuid extends WriteCharacteristicRequest<UUID> {
        Uuid(UUID serviceUuid, UUID characteristicUuid, UUID value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitUuid(this);
        }
    }

    static class Position extends WriteCharacteristicRequest<com.decawave.argo.api.struct.Position> {

        public Position(UUID serviceUuid, UUID characteristicUuid, com.decawave.argo.api.struct.Position value) {
            super(serviceUuid, characteristicUuid, WriteType.WITH_RESPONSE, value);
        }

        @Override
        void accept(WriteCharacteristicRequestVisitor visitor) {
            visitor.visitPosition(this);
        }
    }



}
