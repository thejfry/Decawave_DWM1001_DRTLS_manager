/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import com.decawave.argo.api.interaction.LocationData;

/**
 * Tag network node.
 */
public interface TagNode extends NetworkNode {

    void setAccelerometerEnable(Boolean enable);

    Boolean isAccelerometerEnable();

    Integer getUpdateRate();

    void setUpdateRate(Integer updateRate);

    Integer getStationaryUpdateRate();

    void setStationaryUpdateRate(Integer updateRate);

    Boolean isLowPowerModeEnable();

    void setLowPowerModeEnable(Boolean enable);

    Boolean isLocationEngineEnable();

    void setLocationEngineEnable(Boolean enable);

    LocationData getLocationData();

    boolean anyRangingAnchorInLocationData();
}