/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Network nodes enhanced with local-only properties.
 */

public interface NetworkNodeEnhanced {

    // these properties are specific to network placement
    // one is not notified about changes on these properties
    @NotNull TrackMode getTrackMode();

    // rest of properties
    Long getId();

    String getBleAddress();

    boolean isTag();

    boolean isAnchor();

    @NotNull NetworkNode asPlainNode();

    /**
     * Updates internal last seen timestamp.
     * This is updated whenever the node is discovered, connected and disconnected.
     */
    void touchLastSeen();

    Long getLastSeen();

    Object getProperty(NetworkNodeProperty property);

    @NotNull
    List<NodeWarning> getWarnings();

}
