/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import com.decawave.argomanager.prefs.converters.BooleanConverter;
import com.decawave.argomanager.prefs.converters.DateConverter;
import com.decawave.argomanager.prefs.converters.DoubleConverter;
import com.decawave.argomanager.prefs.converters.EnumConverter;
import com.decawave.argomanager.prefs.converters.FloatConverter;
import com.decawave.argomanager.prefs.converters.IntegerConverter;
import com.decawave.argomanager.prefs.converters.ListConverter;
import com.decawave.argomanager.prefs.converters.LongConverter;
import com.decawave.argomanager.prefs.converters.MapConverter;
import com.decawave.argomanager.prefs.converters.ShortConverter;
import com.decawave.argomanager.prefs.converters.StringConverter;
import com.decawave.argomanager.prefs.converters.UuidConverter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ConverterRepository {
    // exact type converters
    private static Map<Class, StringValueConverter> exactTypeConverters = new HashMap<>();
    // instanceOf converters
    private static List<StringValueConverter> subTypeConverters = new LinkedList<>();

    private static void addConverter(StringValueConverter converter) {
        addConverter(converter, false);
    }

    private static void addConverter(StringValueConverter converter, boolean shouldMatchSubTypes) {
        exactTypeConverters.put(converter.getConvertedType(), converter);
        if (shouldMatchSubTypes) {
            subTypeConverters.add(converter);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // API
    //
    public static StringValueConverter getConverterForClass(Class cls) {
        StringValueConverter converter = exactTypeConverters.get(cls);
        if (converter == null) {
            // it might be an enum
            if (cls.isEnum()) {
                // create on-request converter
                converter = new EnumConverter(cls);
                // put it to cache, so that it can be looked up later
                exactTypeConverters.put(cls, converter);
            } else {
                // to lookup by sub-type
                for (StringValueConverter instanceOfTypeConverter : subTypeConverters) {
                    if (instanceOfTypeConverter.getConvertedType().isAssignableFrom(cls)) {
                        converter = instanceOfTypeConverter;
                    }
                }
            }
        }
        return converter;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // initialization
    //
    static {
        addConverter(StringConverter.INSTANCE);
        addConverter(BooleanConverter.INSTANCE);
        addConverter(DateConverter.INSTANCE);
        addConverter(IntegerConverter.INSTANCE);
        addConverter(ShortConverter.INSTANCE);
        addConverter(FloatConverter.INSTANCE);
        addConverter(DoubleConverter.INSTANCE);
        addConverter(UuidConverter.INSTANCE);
        addConverter(LongConverter.INSTANCE);
        // should check other types
        addConverter(MapConverter.INSTANCE, true);
        addConverter(ListConverter.INSTANCE, true);
    }

}
