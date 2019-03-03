/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.Callable;

import static com.decawave.argomanager.ArgoApp.daApp;


/**
 * Preference shared among all datasets - global to an application.
 *
 * If you want to create a new app preference just declare a new enum inside {@link AppPreference.Element}
 * with proper value data type.
 *
 * @see AppPreferenceAccessor working with app preferences
 */
public class AppPreference {

    public enum Element {
        // !!!!! DO NOT RENAME !!!!! (reorder permitted)
        LAST_SELECTED_AB_SPINNER_ITEM_POS(Integer.class, 0),
        LAST_SELECTED_DEVELOPMENT_TOOLS_SPINNER_ITEM_POS(Integer.class, 0),
        ACTIVE_NETWORK_ID(Short.class),
        SHOW_GRID_DEBUG_INFO(Boolean.class, false),
        SHOW_GRID(Boolean.class, true),
        SHOW_AVERAGE(Boolean.class, true),
        INSTRUCTIONS_READ(Boolean.class, false),
        LENGTH_UNIT(LengthUnit.class, getDefaultLengthUnit),
        APPLICATION_MODE(ApplicationMode.class, ApplicationMode.SIMPLE),
        ;

        /* package private */ final Class<?> valueCls;
        private final Object defaultValue;
        private final Callable<?> defaultValueProvider;

        <T> Element(@NotNull Class<T> valueCls) {
            this(valueCls, null, null);
        }

        <T> Element(@NotNull Class<T> valueCls, @NotNull T defaultValue) {
            this(valueCls, defaultValue, null);
        }

        <T> Element(@NotNull Class<T> valueCls, @NotNull Callable<T> defaultValueProvider) {
            this(valueCls, null, defaultValueProvider);
        }

        <T> Element(Class<T> valueCls, T defaultValue, Callable<T> defaultValueProvider) {
            this.valueCls = valueCls;
            this.defaultValue = defaultValue;
            this.defaultValueProvider = defaultValueProvider;
            // check that primitives specify non-null default value or
            if (Constants.DEBUG) {
                if (valueCls.isPrimitive()) {
                    Preconditions.checkState(defaultValue != null || defaultValueProvider != null);
                }
                // we cannot supply both default value and default value provider
                Preconditions.checkState(!(defaultValue != null && defaultValueProvider != null));
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getDefaultValue() {
            T retVal;
            if (defaultValue != null) {
                retVal = (T) defaultValue;
            } else if (defaultValueProvider != null) {
                try {
                    retVal = (T) defaultValueProvider.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // if the default value is null, use converter to provide 'empty value' to be returned
                retVal = (T) getValueConverter(null).newEmptyValue(this.valueCls);
            }
            return retVal;
        }

        /* package private */
        @SuppressWarnings("unchecked")
        boolean isEmptyValue(Object value) {
            return getValueConverter(value).isValueEmpty(value);
        }

        /* package private */
        @SuppressWarnings("unchecked")
        String getValueAsString(@NotNull Object value) {
            return getValueConverter(value).asString(value);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        // helper methods
        //
        private StringValueConverter getValueConverter(@Nullable Object valueHint) {
            // prefer class of the contained value
            Class<?> converterValCls = valueHint != null ? valueHint.getClass() : this.valueCls;
            StringValueConverter r = ConverterRepository.getConverterForClass(converterValCls);
            if (r == null) {
                throw new IllegalStateException("no converter found for class: " + converterValCls);
            }
            return r;
        }

    }

    /** Get default value for LengthUnit preference */
    private static Callable<LengthUnit> getDefaultLengthUnit = () -> {
        try {
            // get default by country
            final String currentCountry = getCurrentCountry();
            String[] countriesWithInches = {
                    "US", "UK"
            };
            for (String c : countriesWithInches) {
                if (c.equals(currentCountry)) {
                    // set "inches"
                    return LengthUnit.IMPERIAL;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        // multi-else:
        return LengthUnit.METRIC;
    };

    private static String getCurrentCountry() {
        String currentCountry = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) daApp.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) currentCountry = telephonyManager.getSimCountryIso();
        } catch (Exception e) {
            // silently ignore
        }
        if (currentCountry == null) currentCountry = getDefaultCountry();
        return currentCountry;
    }

    @NonNull
    private static String getDefaultCountry() {
        return Locale.getDefault().getCountry().toUpperCase();
    }

}
