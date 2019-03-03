/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.decawave.argomanager.Constants;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class ListConverter extends StringValueConverterAbstract<List> {

    public static final ListConverter INSTANCE = new ListConverter();

    public static final Gson GSON = new Gson();

    private static final Type stringListType = new TypeToken<List<String>>(){}.getType();

    // use singleton INSTANCE member
    private ListConverter() {
        super(List.class);
    }

    @Override
    public List _fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(str);
        }
        List<String> retVal = newEmptyValue(targetClassHint);
        // convert string to list
        retVal.addAll((List) GSON.fromJson(str, stringListType));
        // return
        return retVal;
    }

    @Override
    public String _asString(List val) throws UnsupportedOperationException {
        // we serialize list to String
        if (val.isEmpty()) {
            // clear
            return null;
        } else {
            // persist the new list as JSON (as string)
            return GSON.toJson(val, stringListType).toString();
        }
    }

    @Override
    public List newEmptyValue(Class<?> targetClassHint) {
        return new LinkedList<>();
    }

    @Override
    public boolean isValueEmpty(List val) {
        return val == null || val.isEmpty();
    }

    @Override
    public List deepCopy(List val) {
        if (val != null) {
            // we presume that this is list of immutable values
            val = Lists.newLinkedList(val);
        }
        return val;
    }

}
