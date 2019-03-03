/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api;

/**
 * Argo project.
 */

public class Util {

    public static String hexaFormat(long number) {
        return "0x" + String.format("%04X", number);
    }

    public static String shortenNodeId(long number, boolean prepend0x) {
        return (prepend0x ? "0x…" : "") + String.format("%04X", (short) number);
    }

}
