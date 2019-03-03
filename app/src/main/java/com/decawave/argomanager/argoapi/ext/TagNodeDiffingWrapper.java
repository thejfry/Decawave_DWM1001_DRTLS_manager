/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.interaction.LocationData;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.TagNode;
import com.google.common.base.Objects;

/**
 * Tag diffing wrapper - actually there is nothing to diff.
 */
public class TagNodeDiffingWrapper extends NetworkNodeDiffingWrapper<TagNode> implements TagNode {

    TagNodeDiffingWrapper(TagNode node) {
        super(node);
    }

    @Override
    public Integer getUpdateRate() {
        return delegate.getUpdateRate();
    }

    @Override
    public void setUpdateRate(Integer updateRate) {
        delegate.setUpdateRate(updateRate);
    }

    public boolean isUpdateRateChanged() {
        return !Objects.equal(delegate.getUpdateRate(), original.getUpdateRate());
    }

    @Override
    public Integer getStationaryUpdateRate() {
        return delegate.getStationaryUpdateRate();
    }

    @Override
    public void setStationaryUpdateRate(Integer updateRate) {
        delegate.setStationaryUpdateRate(updateRate);
    }

    public boolean isStationaryUpdateRateChanged() {
        return !Objects.equal(delegate.getStationaryUpdateRate(), original.getStationaryUpdateRate());
    }

    @Override
    public void setLocationEngineEnable(Boolean enable) {
        delegate.setLocationEngineEnable(enable);
    }

    @Override
    public LocationData getLocationData() {
        return delegate.getLocationData();
    }

    @Override
    public Boolean isLocationEngineEnable() {
        return delegate.isLocationEngineEnable();
    }

    public boolean isLocationEngineEnableChanged() {
        return !Objects.equal(delegate.isLocationEngineEnable(), original.isLocationEngineEnable());
    }
    @Override
    public void setLowPowerModeEnable(Boolean enable) {
        delegate.setLowPowerModeEnable(enable);
    }

    @Override
    public Boolean isLowPowerModeEnable() {
        return delegate.isLowPowerModeEnable();
    }

    @Override
    public void setAccelerometerEnable(Boolean enable) {
        delegate.setAccelerometerEnable(enable);
    }

    @Override
    public Boolean isAccelerometerEnable() {
        return delegate.isAccelerometerEnable();
    }

    @Override
    public boolean anyRangingAnchorInLocationData() {
        return delegate.anyRangingAnchorInLocationData();
    }

    public boolean isAccelerometerEnableChanged() {
        return isPropertyChanged(NetworkNodeProperty.TAG_ACCELEROMETER_ENABLE);
    }

    public boolean isLowPowerModeChanged() {
        return !Objects.equal(delegate.isLowPowerModeEnable(), original.isLowPowerModeEnable());
    }

    @Override
    public void copyWritablePropertiesFrom(TagNode node) {
        super.copyWritablePropertiesFrom(node);
        setUpdateRate(node.getUpdateRate());
        setAccelerometerEnable(node.isAccelerometerEnable());
        setStationaryUpdateRate(node.getStationaryUpdateRate());
    }

    @Override
    public <T> T getProperty(NetworkNodeProperty property, boolean deepCopy) {
        return delegate.getProperty(property, deepCopy);
    }
}
