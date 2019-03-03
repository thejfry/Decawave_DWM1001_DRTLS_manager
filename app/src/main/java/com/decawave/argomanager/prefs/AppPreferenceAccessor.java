/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import org.jetbrains.annotations.Nullable;

/**
 * Accessor for application global properties.
 *
 * @see IhAppPreferenceListener for listening to app preference changes
 */
public interface AppPreferenceAccessor {

    void setLastSelectedMainSpinnerItemPos(@Nullable Integer idx);

    void setInstructionsRead();

    boolean isInstructionsRead();

    int getLastSelectedMainSpinnerItemPos();

    void setLastSelectedDevelopmentToolsSpinnerItemPos(@Nullable Integer idx);

    int getLastSelectedDevelopmentToolsSpinnerItemPos();

    void setActiveNetworkId(@Nullable Short networkId);

    Short getActiveNetworkId();

    /**
     * Enforce preference database dump on the current thread.
     */
    void enforcePreferenceDump();

    void setShowGridDebugInfo(boolean b);

    boolean getShowGridDebugInfo();

    @SuppressWarnings("UnusedReturnValue")
    boolean setShowGrid(boolean b);

    boolean getShowGrid();

    @SuppressWarnings("UnusedReturnValue")
    boolean setShowAverage(boolean b);

    boolean getShowAverage();

    LengthUnit getLengthUnit();

    void setLengthUnit(LengthUnit lengthUnit);

    ApplicationMode getApplicationMode();

    void setApplicationMode(ApplicationMode applicationMode);

}
