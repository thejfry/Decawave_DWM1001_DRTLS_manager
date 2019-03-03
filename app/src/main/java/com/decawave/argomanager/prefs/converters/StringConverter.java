/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

/**
 * Trivial - identity.
 */
public class StringConverter extends StringValueConverterAbstract<String> {
    public static final StringConverter INSTANCE = new StringConverter();

    // use INSTANCE
    private StringConverter() {
        super(String.class, true);
    }

    @Override
    public String _fromString(String str, Class<?> targetClassHint) {
        return str;
    }

    @Override
    public String _asString(String val) {
        return val;
    }

}
