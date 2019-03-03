/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

/**
 * Log message severity.
 */
public enum Severity {
    DEBUG, INFO, IMPORTANT, WARNING, ERROR;

    public boolean lessThan(Severity severity) {
        return this.ordinal() < severity.ordinal();
    }
}
