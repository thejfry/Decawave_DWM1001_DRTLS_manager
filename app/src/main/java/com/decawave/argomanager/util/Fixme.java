/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

/**
 *
 */


public class Fixme extends RuntimeException {

    public Fixme() {
        super("FIXME");
    }

    public Fixme(Object value) {
        super("FIXME: " + value);
    }

    public Fixme(Throwable cause) {
        super(cause);
    }
}
