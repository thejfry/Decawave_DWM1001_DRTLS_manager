/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 *
 */
public class ShortConverter extends StringValueConverterAbstract<Short> {

    public static final ShortConverter INSTANCE = new ShortConverter();

    // use singleton INSTANCE member
    private ShortConverter() {
        super(Short.class);
    }

    @Override
    public Short _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return Short.valueOf(str);
    }

    @Override
    public String _asString(Short val) throws UnsupportedOperationException {
        return String.valueOf(val);
    }
}
