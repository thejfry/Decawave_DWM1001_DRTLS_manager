/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 *
 */
public class DoubleConverter extends StringValueConverterAbstract<Double> {

    public static final DoubleConverter INSTANCE = new DoubleConverter();

    // use singleton INSTANCE member
    private DoubleConverter() {
        super(Double.class);
    }

    @Override
    public Double _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return Double.valueOf(str);
    }

    @Override
    public String _asString(Double val) throws UnsupportedOperationException {
        return String.valueOf(val);
    }
}
