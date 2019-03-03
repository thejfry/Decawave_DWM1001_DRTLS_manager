/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;

import java.util.List;

/**
 * Logs changed position and distances of nodes.
 *
 * Managed by {@link NetworkNodeManager}.
 */
public interface LocationDataLogger {

    void setInitialPosition(long nodeId, String nodeBle, Position position);

    void logLocationData(long nodeId,
                         String nodeBle,
                         Position position,
                         List<RangingAnchor> distances,
                         boolean fromProxy);

}
