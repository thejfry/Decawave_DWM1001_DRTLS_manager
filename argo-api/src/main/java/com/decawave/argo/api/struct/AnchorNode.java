/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import java.util.List;

/**
 * Anchor network node.
 */
public interface AnchorNode extends NetworkNode {

    Byte getSeatNumber();

    Integer getMacStats();

    Short getClusterMap();

    Short getClusterNeighbourMap();

    List<Short> getAnchorList();

    Boolean isInitiator();

    void setInitiator(Boolean initiator);

    // bridge is read-only
    Boolean isBridge();

    /**
     * Set a position.
     */
    void setPosition(Position position);

    /**
     * @return a deep copy of position
     * @see #getProperty(NetworkNodeProperty)
     */
    Position getPosition();

    /**
     * @return a deep copy of distance list
     * @see #getProperty(NetworkNodeProperty)
     */
    List<RangingAnchor> getDistances();

    boolean anyDistance();


}
