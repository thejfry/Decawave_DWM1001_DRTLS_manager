/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

/**
 * This class converts arbitrary Java type to a String.
 * It is intended primarily for [arbitrarily typed] preference values
 * to be able to save/load them to/from String values (DB value).
 *
 * T - type to convert to/from String
 *
 * Implementations might decide to support just a one-way conversion.
 * Methods, might therefore throw UnsupportedOperationException.
 */
public interface StringValueConverter<T> {

    Class<T> getConvertedType();

    T fromString(String str) throws UnsupportedOperationException;

    T fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException;

    /**
     * Converts the given java-centric value to it's string representation.
     * Empty value should be returned as null.
     */
    String asString(T val) throws UnsupportedOperationException;

    T newEmptyValue(Class<?> targetClassHint);

    boolean isValueEmpty(T val);

    // this method does not fit in here, but it's better to have it here than anywhere else
    T deepCopy(T val);

}
