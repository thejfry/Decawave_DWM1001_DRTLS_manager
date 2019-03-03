/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeStatistics;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.UwbMode;
import com.google.common.base.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Diffing network node wrapper.
 */
public abstract class NetworkNodeDiffingWrapper<T extends NetworkNode> implements NetworkNode, NetworkNodePropertySetter {
    protected final T delegate;
    @SuppressWarnings("WeakerAccess")
    protected final T original;

    NetworkNodeDiffingWrapper(T node) {
        this.original = node;
        // create a copy into delegate
        this.delegate = NodeFactory.newNodeCopy(node);
    }

    @Override
    public void copyFrom(NetworkNode node) {
        // pass to delegate
        delegate.copyFrom(node);
    }

    @Override
    public Long getId() {
        return delegate.getId();
    }

    @Override
    public Integer getHwVersion() {
        return delegate.getHwVersion();
    }

    @Override
    public boolean isCompatible(NetworkNode node) {
        return delegate.isCompatible(node);
    }

    @Override
    public Integer getFw1Version() {
        return delegate.getFw1Version();
    }

    @Override
    public Integer getFw1Checksum() {
        return delegate.getFw1Checksum();
    }

    @Override
    public Integer getFw2Version() {
        return delegate.getFw2Version();
    }

    @Override
    public Integer getFw2Checksum() {
        return delegate.getFw2Checksum();
    }

    @Override
    public OperatingFirmware getOperatingFirmware() {
        return delegate.getOperatingFirmware();
    }

    @Override
    public void setOperatingFirmware(OperatingFirmware operatingFirmware) {
        delegate.setOperatingFirmware(operatingFirmware);
    }

    @Override
    public NodeType getType() {
        return delegate.getType();
    }

    @Override
    public void setUwbMode(UwbMode uwbMode) {
        delegate.setUwbMode(uwbMode);
    }

    @Override
    public UwbMode getUwbMode() {
        return delegate.getUwbMode();
    }

    @Override
    public void setLedIndicationEnable(Boolean enable) {
        delegate.setLedIndicationEnable(enable);
    }

    @Override
    public Boolean isLedIndicationEnable() {
        return delegate.isLedIndicationEnable();
    }

    @Override
    public void setFirmwareUpdateEnable(Boolean enable) {
        delegate.setFirmwareUpdateEnable(enable);
    }

    @Override
    public Boolean isFirmwareUpdateEnable() {
        return delegate.isFirmwareUpdateEnable();
    }

    @Override
    public LocationDataMode getLocationDataMode() {
        return delegate.getLocationDataMode();
    }

    @Override
    public boolean isAnchor() {
        return delegate.isAnchor();
    }

    @Override
    public boolean isTag() {
        return delegate.isTag();
    }

    @Override
    public void setLocationDataMode(LocationDataMode locationDataMode) {
        delegate.setLocationDataMode(locationDataMode);
    }

    @Override
    public String getLabel() {
        return delegate.getLabel();
    }

    @Override
    public void setLabel(String label) {
        delegate.setLabel(label);
    }

    @Override
    public String getBleAddress() {
        return delegate.getBleAddress();
    }

    @Override
    public Short getNetworkId() {
        return delegate.getNetworkId();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public void setPassword(String password) {
        delegate.setPassword(password);
    }

    @Override
    public void setNetworkId(Short networkId) {
        delegate.setNetworkId(networkId);
    }

    @Override
    public NodeStatistics getNodeStatistics() {
        return delegate.getNodeStatistics();
    }

    @Override
    public boolean isPropertyRecognized(NetworkNodeProperty property) {
        return delegate.isPropertyRecognized(property);
    }

    @Override
    public <U> U getProperty(NetworkNodeProperty property) {
        return delegate.getProperty(property);
    }

    @Override
    public boolean isPropertyInitialized(NetworkNodeProperty networkNodeProperty) {
        return delegate.isPropertyInitialized(networkNodeProperty);
    }

    @Override
    public final void setProperty(NetworkNodeProperty property, Object value) {
        ((NetworkNodePropertySetter) delegate).setProperty(property, value);
    }

    @Override
    public Position extractPositionDirect() {
        return delegate.extractPositionDirect();
    }

    @Nullable
    @Override
    public List<RangingAnchor> extractDistancesDirect() {
        return delegate.extractDistancesDirect();
    }

    public boolean isNetworkIdChanged() {
        return isPropertyChanged(NetworkNodeProperty.NETWORK_ID);
    }

    public boolean isLocationDataModeChanged() {
        return isPropertyChanged(NetworkNodeProperty.LOCATION_DATA_MODE);
    }

    public boolean isLabelChanged() {
        return isPropertyChanged(NetworkNodeProperty.LABEL);
    }

    public boolean isPasswordChanged() {
        return isPropertyChanged(NetworkNodeProperty.PASSWORD);
    }

    public boolean isUwbModeChanged() {
        return isPropertyChanged(NetworkNodeProperty.UWB_MODE);
    }

    public boolean isOperatingFirmwareChanged() {
        return isPropertyChanged(NetworkNodeProperty.OPERATING_FIRMWARE);
    }

    public boolean isFirmwareUpdateEnableChanged() {
        return isPropertyChanged(NetworkNodeProperty.FIRMWARE_UPDATE_ENABLE);
    }

    public boolean isLedIndicationEnableChanged() {
        return isPropertyChanged(NetworkNodeProperty.LED_INDICATION_ENABLE);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isPropertyChanged(NetworkNodeProperty property) {
        return delegate.isPropertyInitialized(property)
                && !Objects.equal(delegate.getProperty(property), original.getProperty(property));
    }

    public void copyWritablePropertiesFrom(T node) {
        setNetworkId(node.getNetworkId());
        setLocationDataMode(node.getLocationDataMode());
        setLabel(node.getLabel());
        setUwbMode(node.getUwbMode());
        setLedIndicationEnable(node.isLedIndicationEnable());
    }

    @Override
    public CompareResult compareTo(@NotNull NetworkNode networkNode) {
        return delegate.compareTo(networkNode);
    }

    @NotNull
    @Override
    public Set<NetworkNodeProperty> compareByProperty(@NotNull NetworkNode networkNode) {
        return delegate.compareByProperty(networkNode);
    }

    @Override
    public Set<NetworkNodeProperty> getInitializedProperties() {
        return delegate.getInitializedProperties();
    }

    @Override
    public String toString() {
        return "NetworkNodeDiffingWrapper:original=" + original.toString();
    }
}
