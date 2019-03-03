/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import java.util.UUID;

/**
 *
 */
public class UuidConverter extends StringValueConverterAbstract<UUID> {

    public static final UuidConverter INSTANCE = new UuidConverter();

    // use singleton INSTANCE member
    private UuidConverter() {
        super(UUID.class);
    }

    @Override
    public UUID _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return UUID.fromString(str);
    }

    @Override
    public String _asString(UUID val) throws UnsupportedOperationException {
        return val.toString();
    }
}
