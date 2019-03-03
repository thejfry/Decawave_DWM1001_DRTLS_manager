/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 *
 */
public class FloatConverter extends StringValueConverterAbstract<Float> {

    public static final FloatConverter INSTANCE = new FloatConverter();

    // use singleton INSTANCE member
    private FloatConverter() {
        super(Float.class);
    }

    @Override
    public Float _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return Float.valueOf(str);
    }

    @Override
    public String _asString(Float val) throws UnsupportedOperationException {
        return String.valueOf(val);
    }
}
