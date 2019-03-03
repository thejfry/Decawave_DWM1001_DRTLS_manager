/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.TagNode;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tag implementation.
 */
class TagNodeImpl extends NetworkNodeImpl implements TagNode {

    TagNodeImpl(Long nodeId) {
        super(nodeId, NodeType.TAG);
    }

    TagNodeImpl(TagNode other) {
        super(other);
    }

    @Override
    public Integer getUpdateRate() {
        return getProperty(NetworkNodeProperty.TAG_UPDATE_RATE);
    }

    @Override
    public void setUpdateRate(Integer updateRate) {
        setProperty(NetworkNodeProperty.TAG_UPDATE_RATE, updateRate);
    }

    @Override
    public Integer getStationaryUpdateRate() {
        return getProperty(NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE);
    }

    @Override
    public void setStationaryUpdateRate(Integer updateRate) {
        setProperty(NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE, updateRate);
    }

    @Override
    public Boolean isLowPowerModeEnable() {
        return getProperty(NetworkNodeProperty.TAG_LOW_POWER_MODE_ENABLE);
    }

    @Override
    public void setLowPowerModeEnable(Boolean enable) {
        setProperty(NetworkNodeProperty.TAG_LOW_POWER_MODE_ENABLE, enable);
    }

    @Override
    public Boolean isLocationEngineEnable() {
        return getProperty(NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE);
    }

    @Override
    public void setLocationEngineEnable(Boolean enable) {
        setProperty(NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE, enable);
    }

    @Override
    public LocationData getLocationData() {
        return getProperty(NetworkNodeProperty.TAG_LOCATION_DATA);
    }

    @Override
    public boolean anyRangingAnchorInLocationData() {
        List<RangingAnchor> ras = extractDistancesDirect();
        return ras != null && ras.size() > 0;
    }

    @Override
    public void setAccelerometerEnable(Boolean enable) {
        setProperty(NetworkNodeProperty.TAG_ACCELEROMETER_ENABLE, enable);
    }

    @Override
    public Boolean isAccelerometerEnable() {
        return getProperty(NetworkNodeProperty.TAG_ACCELEROMETER_ENABLE);
    }

    // position and location depend on each other, they need to be set together
    public void setLocationData(LocationData locationData) {
        setProperty(NetworkNodeProperty.TAG_LOCATION_DATA, locationData);
    }

    @Override
    public boolean isPropertyRecognized(NetworkNodeProperty property) {
        switch (property) {
            case TAG_UPDATE_RATE:
            case TAG_ACCELEROMETER_ENABLE:
            case TAG_STATIONARY_UPDATE_RATE:
            case TAG_LOCATION_ENGINE_ENABLE:
            case TAG_LOW_POWER_MODE_ENABLE:
            case TAG_LOCATION_DATA:
                return true;
            default:
                return super.isPropertyRecognized(property);
        }
    }

    @Override
    public Position extractPositionDirect() {
        LocationData ld = getLocationData();
        if (ld != null && ld.position != null) {
            return ld.position;
        } // else:
        return null;
    }

    @Nullable
    @Override
    public List<RangingAnchor> extractDistancesDirect() {
        LocationData ld = getLocationData();
        if (ld != null && ld.distances != null) {
            return ld.distances;
        } // else:
        return null;
    }

}
