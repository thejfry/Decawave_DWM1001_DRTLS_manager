/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.interaction;

/**
 *
 */
public class Fail {
    public final int errorCode;
    public final String message;

    public Fail(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Fail{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                '}';
    }
}
