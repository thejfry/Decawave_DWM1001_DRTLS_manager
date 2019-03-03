/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Argo project.
 */
public class RangingAnchor {
    public short nodeId;
    public Distance distance;

    public RangingAnchor() {
    }

    public RangingAnchor(short nodeId, int distance, byte qualityFactor) {
        this.nodeId = nodeId;
        this.distance = new Distance(distance, qualityFactor);
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    public RangingAnchor(@NotNull RangingAnchor other) {
        copyFrom(other);
    }

    private void copyFrom(@NotNull RangingAnchor other) {
        this.nodeId = other.nodeId;
        this.distance = other.distance;
    }

    public static List<RangingAnchor> deepCopy(List<RangingAnchor> source) {
        List<RangingAnchor> r = new ArrayList<>(source.size());
        for (RangingAnchor sra : source) {
            r.add(new RangingAnchor(sra));
        }
        return r;
    }

    @Override
    public String toString() {
        return "RangingAnchor{" +
                "nodeId=" + String.format("%04X", nodeId) +
                ", distance=" + distance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RangingAnchor that = (RangingAnchor) o;

        //noinspection SimplifiableIfStatement
        if (nodeId != that.nodeId) return false;
        return distance != null ? distance.equals(that.distance) : that.distance == null;
    }

    @Override
    public int hashCode() {
        int result = (int) nodeId;
        result = 31 * result + (distance != null ? distance.hashCode() : 0);
        return result;
    }
}
