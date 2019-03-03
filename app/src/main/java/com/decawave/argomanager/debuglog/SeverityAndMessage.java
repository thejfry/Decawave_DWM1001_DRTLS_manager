/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

/**
 * Composed severity and message - parametrized LogEntry content.
 */
public class SeverityAndMessage {

    private SeverityAndMessage(Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public static SeverityAndMessage from(Severity severity, String message) {
        return new SeverityAndMessage(severity, message);
    }

    public final Severity severity;
    public final String message;

}
