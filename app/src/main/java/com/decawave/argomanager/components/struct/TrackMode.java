/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

/**
 * Argo project.
 */

public enum TrackMode {
    NOT_TRACKED(false),
    TRACKED_POSITION(true),
    TRACKED_POSITION_AND_RANGING(true);

    public final boolean tracked;

    TrackMode(boolean tracked) {
        this.tracked = tracked;
    }
}
