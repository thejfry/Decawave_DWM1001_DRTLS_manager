/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import com.decawave.argo.api.struct.NetworkNodeProperty;

/**
 * Argo project.
 */
public interface NetworkNodePropertyDecorator {

    class DecoratedProperty {
        public final NetworkNodeProperty property;
        public final String label;
        @SuppressWarnings("WeakerAccess")
        public final NetworkNodePropertyValueFormatter formatter;

        DecoratedProperty(NetworkNodeProperty property, String label, NetworkNodePropertyValueFormatter formatter) {
            this.property = property;
            this.label = label;
            this.formatter = formatter;
        }

        @SuppressWarnings("unchecked")
        public String formatValue(Object value) {
            return formatter.format(value);
        }
    }

    DecoratedProperty decorate(NetworkNodeProperty property);

}
