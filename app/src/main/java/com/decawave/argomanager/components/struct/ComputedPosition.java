/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.util.Util;

import java.util.Arrays;

/**
 * Argo project.
 */
public class ComputedPosition {
    public boolean success;
    public Long[] fromNodes;
    public Position position;

    public ComputedPosition() {
        fromNodes = new Long[] {null, null, null};
        success = true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ComputedPosition{");
        sb.append("success=").append(success);
        sb.append(", fromNodes=[");
        for (Long nodeId : fromNodes) {
            if (nodeId != null) {
                sb.append(Util.shortenNodeId(nodeId, false));
            } else {
                sb.append("null");
            }
            sb.append(",");
        }
        sb.replace(sb.length(), sb.length(), "]");
        sb.append(", position=").append(position);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComputedPosition that = (ComputedPosition) o;

        if (success != that.success) return false;
        //noinspection SimplifiableIfStatement
        if (!Arrays.equals(fromNodes, that.fromNodes)) return false;
        return position != null ? position.equals(that.position) : that.position == null;

    }

    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + Arrays.hashCode(fromNodes);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }
}
