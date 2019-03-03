/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs.converters;

import com.decawave.argomanager.prefs.StringValueConverter;
import com.decawave.argomanager.util.Fixme;
import com.google.common.primitives.Primitives;

import eu.kryl.android.common.Constants;

/**
 * Abstract to/from String value convertor predecessor.
 */
public abstract class StringValueConverterAbstract<T> implements StringValueConverter<T> {
    private final Class<T> convertedType;
    private final boolean returnsArbitraryString;

    protected StringValueConverterAbstract(Class<T> convertedType) {
        this(convertedType, false);
    }

    public StringValueConverterAbstract(Class<T> convertedType, boolean returnsArbitraryString) {
        this.convertedType = convertedType;
        this.returnsArbitraryString = returnsArbitraryString;
    }

    @Override
    public Class<T> getConvertedType() {
        return convertedType;
    }

    @Override
    public final String asString(T val) throws UnsupportedOperationException {
        if (val == null) return null;
        String retVal = _asString(val);
        if (returnsArbitraryString) {
            // we need to escape the string (encode)
            retVal = encode(retVal);
        }
        return retVal;
    }

    @Override
    public final T fromString(String str) throws UnsupportedOperationException {
        if (returnsArbitraryString) {
            // we need to unescape the string (decode)
            str = decode(str);
        }
        return fromString(str, null);
    }

    private String encode(String str) {
        if (str.length() < 1) {
            return str;
        }
        char c = str.charAt(0);
        if (c != '@' && c != '\\') {
            // optimization
            return str;
        } // else:
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            c = str.charAt(i);
            if (c == '@' || c == '\\') {
                // prefix
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String decode(String str) {
        if (str.length() < 2) {
            return str;
        } // else:
        char c = str.charAt(0);
        if (c != '\\') {
            // optimization
            return str;
        } // else: this must be escaped
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            c = str.charAt(i);
            if (c == '\\') {
                c = str.charAt(++i);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public T fromString(String str, Class<?> targetClassHint) throws UnsupportedOperationException {
        if (str == null) return newEmptyValue(targetClassHint);
        if (Constants.DEBUG) {
            if (targetClassHint != null) convertedType.isAssignableFrom(targetClassHint);
        }
        return _fromString(str, targetClassHint != null ? targetClassHint : convertedType);
    }

    public T newEmptyValue(Class<?> targetClassHint) {
        // default implementation
        return null;
    }

    @Override
    public boolean isValueEmpty(T val) {
        // default implementation
        return val == null;
    }

    @Override
    public T deepCopy(T val) {
        // default implementation (simply return value as-is)
        if (Constants.DEBUG) {
            if (val != null) {
                Class<?> aClass = val.getClass();
                if (!Primitives.isWrapperType(aClass) && !aClass.isEnum() && !aClass.equals(String.class)) {
                    throw new Fixme("you must provide your own implementation of deepCopy() for " + aClass + "! (" + this.getClass() + ")");
                }
            }
        }
        return val;
    }

    protected abstract String _asString(T t);

    protected abstract T _fromString(String str, Class<?> targetClassHint);


}
