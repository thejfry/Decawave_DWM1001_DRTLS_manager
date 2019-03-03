/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.interaction;

import com.decawave.argo.api.Util;
import com.decawave.argo.api.struct.Position;

/**
 * Argo project.
 */
public class ProxyPosition {
    public final short nodeId;
    public final Position position;


    public ProxyPosition(short nodeId, Position position) {
        this.nodeId = nodeId;
        this.position = position;
    }

    @Override
    public String toString() {
        return "ProxyPosition{" +
                "nodeId=" + Util.shortenNodeId(nodeId, false) +
                ", position=" + position +
                '}';
    }
}
