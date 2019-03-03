/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.interaction;

import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;

import java.util.List;

/**
 * Argo project.
 */
public class LocationData {
    public final Position position;
    public final List<RangingAnchor> distances;

    public LocationData(Position position, List<RangingAnchor> distances) {
        this.position = position;
        this.distances = distances;
    }

    public boolean isEmpty() {
        return position == null && distances == null;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "position=" + position +
                ", distances=" + distances +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationData that = (LocationData) o;

        //noinspection SimplifiableIfStatement
        if (position != null ? !position.equals(that.position) : that.position != null) return false;
        return distances != null ? distances.equals(that.distances) : that.distances == null;
    }

    @Override
    public int hashCode() {
        int result = position != null ? position.hashCode() : 0;
        result = 31 * result + (distances != null ? distances.hashCode() : 0);
        return result;
    }
}
