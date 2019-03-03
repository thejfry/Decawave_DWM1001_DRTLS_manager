/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.decawave.argomanager.ArgoApp;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.async.FixedAsyncActivityScheduler;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Default implementation of {@link AppPreferenceAccessor}.
 *
 * If you want to create a new app preference just declare a new enum inside {@link AppPreference.Element}
 * with proper value data type.
 *
 */
@Singleton
public class AppPreferenceAccessorImpl implements AppPreferenceAccessor {
    private static final ComponentLog log = new ComponentLog(AppPreferenceAccessorImpl.class).disable();
    private static final String FILENAME = "APP_PREFS";

    // members
    private boolean prefsLoaded = false;
    private final EnumSet<AppPreference.Element> dirtyElements;
    private final FixedAsyncActivityScheduler asyncSavePrefs;
    private final EnumMap<AppPreference.Element, Object> preferenceMap;

    @Inject
    AppPreferenceAccessorImpl() {
        this.prefsLoaded = false;
        this.dirtyElements = EnumSet.noneOf(AppPreference.Element.class);
        this.preferenceMap = new EnumMap<>(AppPreference.Element.class);
        this.asyncSavePrefs = new FixedAsyncActivityScheduler(2000, this::doDump);
    }

    private SharedPreferences getSharedPreferences() {
        return ArgoApp.daApp.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
    }

    @Override
    public void setLastSelectedMainSpinnerItemPos(@Nullable Integer idx) {
        setElementValue(AppPreference.Element.LAST_SELECTED_AB_SPINNER_ITEM_POS, idx);
    }

    @Override
    public void setInstructionsRead() {
        setElementValue(AppPreference.Element.INSTRUCTIONS_READ, true);
    }

    @Override
    public boolean isInstructionsRead() {
        return getElementValue(AppPreference.Element.INSTRUCTIONS_READ);
    }

    @Override
    public int getLastSelectedMainSpinnerItemPos() {
        return getElementValue(AppPreference.Element.LAST_SELECTED_AB_SPINNER_ITEM_POS);
    }

    @Override
    public int getLastSelectedDevelopmentToolsSpinnerItemPos() {
        return getElementValue(AppPreference.Element.LAST_SELECTED_DEVELOPMENT_TOOLS_SPINNER_ITEM_POS);
    }

    @Override
    public void setLastSelectedDevelopmentToolsSpinnerItemPos(@Nullable Integer idx) {
        setElementValue(AppPreference.Element.LAST_SELECTED_DEVELOPMENT_TOOLS_SPINNER_ITEM_POS, idx);
    }

    @Override
    public void setActiveNetworkId(Short networkId) {
        setElementValue(AppPreference.Element.ACTIVE_NETWORK_ID, networkId);
    }

    @Override
    public Short getActiveNetworkId() {
        return getElementValue(AppPreference.Element.ACTIVE_NETWORK_ID);
    }

    @Override
    public void setShowGridDebugInfo(boolean b) {
        setElementValue(AppPreference.Element.SHOW_GRID_DEBUG_INFO, b);
    }

    @Override
    public boolean getShowGridDebugInfo() {
        return com.decawave.argomanager.Constants.DEBUG_UI ? getElementValue(AppPreference.Element.SHOW_GRID_DEBUG_INFO) : false;
    }

    @Override
    public boolean setShowGrid(boolean b) {
        return setElementValue(AppPreference.Element.SHOW_GRID, b);
    }

    @Override
    public boolean getShowGrid() {
        return getElementValue(AppPreference.Element.SHOW_GRID);
    }

    @Override
    public boolean setShowAverage(boolean b) {
        return setElementValue(AppPreference.Element.SHOW_AVERAGE, b);
    }

    @Override
    public boolean getShowAverage() { return getElementValue(AppPreference.Element.SHOW_AVERAGE); }

    @Override
    public LengthUnit getLengthUnit() {
        return getElementValue(AppPreference.Element.LENGTH_UNIT);
    }

    @Override
    public ApplicationMode getApplicationMode() {
        return com.decawave.argomanager.Constants.DEBUG_UI ?
                getElementValue(AppPreference.Element.APPLICATION_MODE) : ApplicationMode.SIMPLE;
    }

    @Override
    public void setApplicationMode(ApplicationMode applicationMode) {
        if (Constants.DEBUG) {
            Preconditions.checkState(com.decawave.argomanager.Constants.DEBUG_UI, "how can one set application mode if not in DEBUG_UI?");
        }
        setElementValue(AppPreference.Element.APPLICATION_MODE, applicationMode);
    }

    @Override
    public void setLengthUnit(LengthUnit lengthUnit) {
        setElementValue(AppPreference.Element.LENGTH_UNIT, lengthUnit);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // utility routines
    //

    private synchronized <T> T getElementValue(AppPreference.Element element) {
        assurePrefsLoaded();
        //noinspection unchecked
        T retVal = (T) preferenceMap.get(element);
        if (retVal != null) {
            // return a deep copy of the value
            //noinspection unchecked
            retVal = prefValueDeepCopy(element, retVal);
        }
        if (Constants.DEBUG) log.d("getElementValue() called with: " + "element = [" + element + "], value = [" + retVal + "]");
        return retVal;
    }

    private synchronized boolean setElementValue(AppPreference.Element element, Object value) {
        if (Constants.DEBUG) log.d("setElementValue() called on " + this + " with: " + "element = [" + element + "], value = [" + value + "]");
        assurePrefsLoaded();
        Object existingValue = preferenceMap.get(element);
        if (value == null) {
            // user has explicitly signaled that he want to return null or default (if any)
            Object defaultValue = element.getDefaultValue();
            if (defaultValue != null) {
                // there is a default value, use that one
                value = defaultValue;
            }
        }
        if (!Objects.equal(existingValue, value)) {
            value = prefValueDeepCopy(element, value);
            preferenceMap.put(element, value);
            markDirty(element, existingValue, value);
            return true;
        }
        return false;
    }

    private <T> T prefValueDeepCopy(AppPreference.Element element, T value) {
        // save deep copy of the value
        StringValueConverter converter = ConverterRepository.getConverterForClass(element.valueCls);
        if (converter == null) {
            throw new IllegalStateException("no converter found for class: " + element.valueCls);
        }
        //noinspection unchecked
        return (T) converter.deepCopy(value);
    }

    private void assurePrefsLoaded() {
        if (!prefsLoaded) {
            loadPreferences();
            prefsLoaded = true;
        }
    }

    // this happens only once at the very beginning
    private void loadPreferences() {
        if (Constants.DEBUG) log.d("loadPreferences()");
        SharedPreferences sp = getSharedPreferences();

        // load prefs one by one
        for (AppPreference.Element element : AppPreference.Element.values()) {
            loadPrefElement(sp, element);
        }
        // some elements might have been marked as dirty (because their default value has been used)
        synchronized (dirtyElements) {
            // therefore we need to persist them back
            // theoretically one could experience deadlock here
            // but because loadPreference will never collide with asyncSavePrefs execution, deadlock will not occur
            if (!dirtyElements.isEmpty()) {
                asyncSavePrefs.schedule();
            }
        }
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    private <T> T loadPrefElement(SharedPreferences sp, AppPreference.Element element) {
        String prefAsString = sp.getString(element.name(), null);
        // check if there is a value
        Object newValue;
        if (prefAsString == null) {
            newValue = null;
        } else {
            // lookup a converter
            StringValueConverter converter = ConverterRepository.getConverterForClass(element.valueCls);
            if (converter == null) {
                throw new IllegalStateException("no converter found for class: " + element.valueCls);
            }
            newValue = converter.fromString(prefAsString, element.valueCls);
            // the result might be null (?)
            if (newValue == null && Constants.DEBUG) {
                throw new IllegalStateException("deserialization of non-null value produced null result ???: " + prefAsString + ", converter = " + converter + ", value class = " + converter.getConvertedType());
            }
        }
        // check if we need to use the default value
        if (newValue == null) {
            // the preference value is set inside
            newValue = handleEmptyElementValue(element);
        } else {
            // set the preference value
            preferenceMap.put(element, newValue);
        }
        return (T) newValue;
    }

    private Object handleEmptyElementValue(AppPreference.Element element) {
        if (Constants.DEBUG) {
            Preconditions.checkState(preferenceMap.get(element) == null);
        }
        Object defaultValue = element.getDefaultValue();
        preferenceMap.put(element, defaultValue);
        if (defaultValue != null) {
            // if there is non-null default value, we will persist it back
            markDirty(element, null, defaultValue);
        }
        return defaultValue;
    }

    /**
     * This method is for tests only!
     */
    public synchronized void reset() {
        if (Constants.DEBUG) log.d("reset()");
        dirtyElements.clear();
        prefsLoaded = true;
        preferenceMap.clear();
        for (AppPreference.Element e : AppPreference.Element.values()) {
            handleEmptyElementValue(e);
        }
    }

    @Override
    public void enforcePreferenceDump() {
        doDump();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // used asynchronously during dump

    private void doDump() {
        EnumMap<AppPreference.Element, String> dirtyValues;
        synchronized (this) {
            // we are touching shared state
            if (dirtyElements.isEmpty()) {
                return;
            } // else:
            dirtyValues = prepareDirtyValues();
            // reset dirty elements
            dirtyElements.clear();
        }
        // save the serialized preferences without synchronization
        SharedPreferences asp = getSharedPreferences();
        SharedPreferences.Editor editor = asp.edit();
        for (Map.Entry<AppPreference.Element, String> dirtyElement : dirtyValues.entrySet()) {
            saveSerializedPrefElement(editor, dirtyElement.getKey(), dirtyElement.getValue());
        }
        editor.apply();
    }

    @NotNull
    private EnumMap<AppPreference.Element, String> prepareDirtyValues() {
        EnumMap<AppPreference.Element, String> dirtyValues = Maps.newEnumMap(AppPreference.Element.class);
        for (AppPreference.Element e : dirtyElements) {
            // serialize the changed value
            Object value = preferenceMap.get(e);
            if (e.isEmptyValue(value)) {
                // we represent empty values as null string
                dirtyValues.put(e, null);
                // but we might serialize back default non-empty values! (if the Element is marked dirty)
            } else {// else:
                if (Constants.DEBUG) {
                    // check that the declared value class and real value match
                    Preconditions.checkState(e.valueCls.isAssignableFrom(value.getClass()));
                }
                String strVal = e.getValueAsString(value);
                if (strVal == null && Constants.DEBUG) {
                    throw new IllegalStateException("serialization of non-null value produced null result ???: " + e
                            + ", value = " + value + ", value class = " + value.getClass());
                }
                dirtyValues.put(e, strVal);
            }
        }
        return dirtyValues;
    }

    private void saveSerializedPrefElement(SharedPreferences.Editor editor, AppPreference.Element element, String serializedValue) {
        if (serializedValue == null) {
            // simply remove
            editor.remove(element.name());
        } else {
            // save
            editor.putString(element.name(), serializedValue);
        }
    }

    // if one makes change in the map returned by getDataSetMapping() do not forget to call this
    private void markDirty(final AppPreference.Element element, Object oldValue, Object newValue) {
        synchronized (dirtyElements) {
            dirtyElements.add(element);
        }
        // notify listeners about the change (on UI thread - default delivery policy)
        InterfaceHub.getHandlerHub(IhAppPreferenceListener.class).onPreferenceChanged(element, oldValue, newValue);
        asyncSavePrefs.schedule();
    }


}
