/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.decawave.argomanager.Constants;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MapConverter extends StringValueConverterAbstract<Map> {

    public static final MapConverter INSTANCE = new MapConverter();

    public static final Gson GSON = new Gson();

    private static final Type stringToStringMapType = new TypeToken<Map<String, String>>(){}.getType();

    // use singleton INSTANCE member
    private MapConverter() {
        super(Map.class);
    }

    /**
     * We are able to process BiMap in the targetClassHint.
     */
    @Override
    public Map _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(str);
        }
        Map<String, String> retVal = newEmptyValue(targetClassHint);
        // convert string to map
        retVal.putAll((Map<? extends String, ? extends String>) GSON.fromJson(str, stringToStringMapType));
        // return
        return retVal;
    }

    @Override
    public String _asString(Map val) throws UnsupportedOperationException {
        // we serialize map to String
        if (val.isEmpty()) {
            // clear
            return null;
        } else {
            // persist the new mapping as JSON (as string)
            return GSON.toJson(val, stringToStringMapType).toString();
        }
    }

    @Override
    public Map newEmptyValue(Class<?> targetClassHint) {
        boolean isBiMap = BiMap.class.isAssignableFrom(targetClassHint);
        return isBiMap ? HashBiMap.<String, String>create() : new HashMap<String, String>();
    }

    @Override
    public boolean isValueEmpty(Map val) {
        return val == null || val.isEmpty();
    }

    @Override
    public Map deepCopy(Map val) {
        if (val instanceof BiMap) {
            val = HashBiMap.create(val);
        } else if (val != null) {
            //noinspection unchecked
            val = new HashMap<>(val);
        }
        return val;
    }
}
