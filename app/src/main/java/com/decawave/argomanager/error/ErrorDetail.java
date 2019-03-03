/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.error;

import android.os.SystemClock;

/**
 * Error details.
 */
public class ErrorDetail {
    public final Throwable exception;
    public final String message;
    public final int errorCode;
    public final long time;
    private boolean read;
    //
    private ErrorCodeInterpreter.Properties properties;


    public ErrorDetail(Throwable exception, String message, int errorCode) {
        this.exception = exception;
        this.message = message;
        this.errorCode = errorCode;
        this.time = SystemClock.uptimeMillis();
        this.read = false;
    }

    @SuppressWarnings("WeakerAccess")
    public void markAsRead() {
        this.read = true;
    }

    public boolean isRead() {
        return read;
    }

    public long getTime() {
        return time;
    }

    public ErrorCodeInterpreter.Properties getProperties() {
        if (properties == null) {
            properties = ErrorCodeInterpreter.interpret(errorCode);
        }
        return properties;
    }

    @Override
    public String toString() {
        return "ErrorDetail{" + "exception=" + exception +
                ", message='" + message + '\'' +
                ", errorCode=" + errorCode +
                ", time=" + time +
                ", read=" + read +
                ", properties=" + properties +
                '}';
    }
}
