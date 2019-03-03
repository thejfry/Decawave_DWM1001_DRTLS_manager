/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.struct.NetworkNodeProperty;

/**
 * Use with care!
 * Only when you know what you are doing.
 */

public interface NetworkNodePropertySetter {

    void setProperty(NetworkNodeProperty property, Object value);

}
