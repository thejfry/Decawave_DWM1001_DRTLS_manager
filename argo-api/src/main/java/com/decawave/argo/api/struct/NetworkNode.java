/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Interfacing methods of a network node.
 */
public interface NetworkNode {

    Long getId();

    Integer getHwVersion();

    Integer getFw1Version();

    Integer getFw1Checksum();

    Integer getFw2Version();

    Integer getFw2Checksum();

    NodeType getType();

    Boolean isFirmwareUpdateEnable();

    void setFirmwareUpdateEnable(Boolean enable);

    OperatingFirmware getOperatingFirmware();

    void setOperatingFirmware(OperatingFirmware operatingFirmware);

    LocationDataMode getLocationDataMode();

    void setLocationDataMode(LocationDataMode locationDataMode);

    UwbMode getUwbMode();

    void setUwbMode(UwbMode uwbMode);

    void setLedIndicationEnable(Boolean enable);

    Boolean isLedIndicationEnable();

    String getLabel();

    void setLabel(String label);

    String getBleAddress();

    Short getNetworkId();

    void setNetworkId(Short networkId);

    String getPassword();

    void setPassword(String password);

    /**
     * @return a deep copy of statistics
     * @see #getProperty(NetworkNodeProperty)
     */
    NodeStatistics getNodeStatistics();

    void copyFrom(NetworkNode node);

    boolean isCompatible(NetworkNode node);

    enum CompareResult {
        NO_CHANGE,
        DYNAMIC_CHANGE,
        STATIC_CHANGE
    }

    /**
     * If there is at least one static change (on properties which are not related to position),
     * STATIC_CHANGE is returned. Else if there is at least one dynamic change (on a property
     * related to position, DYNAMIC_CHANGE is returned. If there is no change, NO_CHANGE is returned.
     *
     * @param networkNode node to be compared to
     */
    CompareResult compareTo(NetworkNode networkNode);

    /**
     * @return set of properties which are initialized on both nodes and are different
     */
    @NotNull Set<NetworkNodeProperty> compareByProperty(NetworkNode networkNode);

    Set<NetworkNodeProperty> getInitializedProperties();

    boolean isPropertyInitialized(NetworkNodeProperty networkNodeProperty);

    boolean isPropertyRecognized(NetworkNodeProperty property);

    /**
     * Returns direct reference (not a deep copy) to a property value.
     *
     * @param property identifies the property/value
     * @return null if the property is explicitly set to null, or not known,
     * non-null value otherwise
     */
    <T> T getProperty(NetworkNodeProperty property);

    <T> T getProperty(NetworkNodeProperty property, boolean deepCopy);

    /**
     * For anchor this returns simply position for tag this returns position extracted out of location data (if any).
     * @return network node position, direct reference
     */
    @Nullable Position extractPositionDirect();

    /**
     * For anchor this returns simply distances for tag this returns distances extracted out of location data (if any).
     * @return network node position
     */
    @Nullable List<RangingAnchor> extractDistancesDirect();

    boolean isAnchor();

    boolean isTag();

}
