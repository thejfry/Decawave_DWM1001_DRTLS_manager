/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

/**
 * Argo project.
 */
public enum PresenceStatus {
    PRESENT(true, true),
    PROBABLY_PRESENT(true, false),
    MISSING(false, true),
    PROBABLY_MISSING(false, false);

    public final boolean present;
    public final boolean certain;

    PresenceStatus(boolean present, boolean certain) {
        this.present = present;
        this.certain = certain;
    }

}