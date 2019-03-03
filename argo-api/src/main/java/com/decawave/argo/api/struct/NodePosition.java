/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import java.util.UUID;

/**
 * Returned node position as observed by position discoverer.
 */
public class NodePosition {
    // this is constant
    public final long nodeId;
    // this is variable
    public Position position;

    public NodePosition(long nodeId) {
        this.nodeId = nodeId;
    }

    public NodePosition(long nodeId, Position position) {
        this.nodeId = nodeId;
        this.position = position;
    }

    @Override
    public String toString() {
        return "NodePosition{" +
                "nodeId=" + nodeId +
                ", position=" + position +
                '}';
    }
}
