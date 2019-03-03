/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import java.util.Date;

/**
 *
 */
public class DateConverter extends StringValueConverterAbstract<Date> {

    public static final DateConverter INSTANCE = new DateConverter();

    // use singleton INSTANCE member
    private DateConverter() {
        super(Date.class);
    }

    @Override
    public Date _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        return new Date(Long.valueOf(str));
    }

    @Override
    public String _asString(Date val) throws UnsupportedOperationException {
        return String.valueOf(val.getTime());
    }
}
