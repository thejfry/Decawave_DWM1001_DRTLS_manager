/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components;

import com.decawave.argomanager.components.impl.EnhancedNetworkNodeContainerImpl;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;

import java.util.Collection;

/**
 * Argo project.
 */

public class EnhancedNetworkNodeContainerFactory {

    public static EnhancedNetworkNodeContainer createContainer(Collection<NetworkNodeEnhanced> nodeList) {
        EnhancedNetworkNodeContainerImpl c = new EnhancedNetworkNodeContainerImpl();
        for (NetworkNodeEnhanced networkNodeEnhanced : nodeList) {
            c.doAddNode(networkNodeEnhanced, true);
        }
        return c;
    }

}
