/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 * Boolean value to/from String convertor.
 */
public class BooleanConverter extends StringValueConverterAbstract<Boolean> {
    public static final BooleanConverter INSTANCE = new BooleanConverter();

    // use INSTANCE
    private BooleanConverter() {
        super(Boolean.class);
    }

    @Override
    protected Boolean _fromString(String str, Class<?> targetClassHint) {
        return str.equals("1");
    }

    @Override
    protected String _asString(Boolean val) {
        return val ? "1" : "0";
    }

}
