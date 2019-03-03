/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 *
 */
public class IntegerConverter extends StringValueConverterAbstract<Integer> {

    public static final IntegerConverter INSTANCE = new IntegerConverter();

    // use singleton INSTANCE member
    private IntegerConverter() {
        super(Integer.class);
    }

    @Override
    public Integer _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return Integer.valueOf(str);
    }

    @Override
    public String _asString(Integer val) throws UnsupportedOperationException {
        return String.valueOf(val);
    }
}
