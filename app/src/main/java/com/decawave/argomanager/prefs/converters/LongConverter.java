/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 *
 */
public class LongConverter extends StringValueConverterAbstract<Long> {

    public static final LongConverter INSTANCE = new LongConverter();

    // use singleton INSTANCE member
    private LongConverter() {
        super(Long.class);
    }

    @Override
    public Long _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return Long.valueOf(str);
    }

    @Override
    public String _asString(Long val) throws UnsupportedOperationException {
        return String.valueOf(val);
    }
}
