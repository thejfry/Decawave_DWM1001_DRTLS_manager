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

public enum LengthUnit {
    /** Centimeter, Meter */
    METRIC(R.string.metric_units, R.string.metric_units_unit),
    /** Inches, Feet, Yards */
    IMPERIAL(R.string.imperial_units, R.string.imperial_units_unit);

    public final int labelResource;
    public final int unitLabelResource;

    LengthUnit(int labelResource, int unitLabelResource) {
        this.labelResource = labelResource;
        this.unitLabelResource = unitLabelResource;
    }
}
