/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.prefs;

import com.decawave.argomanager.R;

/**
 * Argo project.
 */

public enum ApplicationMode {
    /** simple */
    SIMPLE(R.string.mode_simple),
    /** for experts */
    ADVANCED(R.string.mode_advanced);

    public final int labelResource;

    ApplicationMode(int labelResource) {
        this.labelResource = labelResource;
    }
}
