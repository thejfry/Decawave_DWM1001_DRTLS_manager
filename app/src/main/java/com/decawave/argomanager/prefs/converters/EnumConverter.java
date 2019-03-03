/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic enum value to/from string converter.
 */
public class EnumConverter<T extends Enum<T>> extends StringValueConverterAbstract<T> {
    private Map<String, T> nameToEnumMap;

    public EnumConverter(Class<T> convertedType) {
        super(convertedType);
        nameToEnumMap = createStringToValueMap(convertedType.getEnumConstants());
    }

    @Override
    public T _fromString(String str, Class<?> targetClassHint) {
        return nameToEnumMap.get(str);
    }

    @Override
    public String _asString(T val) {
        return val.name();
    }

    private Map<String, T> createStringToValueMap(T[] enumConstants) {
        HashMap<String, T> m = new HashMap<>();
        for (T enumConstant : enumConstants) {
            m.put(enumConstant.name(), enumConstant);
        }
        return m;
    }
}
