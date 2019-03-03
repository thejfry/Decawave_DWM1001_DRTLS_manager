/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

/**
 * Argo project.
 */
public class Distance {
    public final int length;
    public final byte quality;

    public Distance(int length, byte quality) {
        this.length = length;
        this.quality = quality;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Distance distance = (Distance) o;

        //noinspection SimplifiableIfStatement
        if (length != distance.length) return false;
        return quality == distance.quality;

    }

    @Override
    public int hashCode() {
        int result = length;
        result = 31 * result + (int) quality;
        return result;
    }

    @Override
    public String toString() {
        return "Distance{" +
                "length=" + length +
                ", quality=" + quality +
                '}';
    }
}
