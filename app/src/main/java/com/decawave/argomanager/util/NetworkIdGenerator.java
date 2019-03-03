/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

/**
 * Argo project.
 */
public class NetworkIdGenerator {

    public static short newNetworkId() {
        short s;
        do {
            s = (short) ((Math.random() * 2 - 1) * Short.MAX_VALUE);
        } while (s == -1 || s == 0); // 0xFFFF and 0x0000 are reserved values
        return s;
    }

}
