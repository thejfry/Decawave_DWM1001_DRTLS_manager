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
 import com.decawave.argo.api.struct.UwbMode;
 import com.decawave.argomanager.Constants;
 import com.decawave.argomanager.util.Util;
 import com.google.common.base.Objects;
 import com.google.common.base.Preconditions;
 import com.google.common.collect.Sets;

 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;

 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.EnumMap;
 import java.util.EnumSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;

 import static com.decawave.argomanager.util.Util.formatAsHexa;

/**
 * Common predecessor for Tags and Anchors.
 */

abstract class NetworkNodeImpl implements NetworkNode, NetworkNodePropertySetter {
    private Map<NetworkNodeProperty, Object> valueMap;

    NetworkNodeImpl(@Nullable Long nodeId, @NotNull NodeType nodeType) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(nodeType);
        }
        this.valueMap = new EnumMap<>(NetworkNodeProperty.class);
        this.valueMap.put(NetworkNodeProperty.NODE_TYPE, nodeType);
        if (nodeId != null) this.valueMap.put(NetworkNodeProperty.ID, nodeId);
    }

    NetworkNodeImpl(NetworkNode other) {
        this.valueMap = new EnumMap<>(NetworkNodeProperty.class);
        doCopyFrom(other, true);
    }

    @Override
    public final boolean isCompatible(NetworkNode node) {
        NodeType t1 = getType();
        NodeType t2 = node.getType();
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(t1, "cannot check compatibility, type of " + this + " is null");
            Preconditions.checkNotNull(t2, "cannot check compatibility, type of " + node + " is null");
        }
        return t1 == t2;
    }

    @Override
    public final void copyFrom(NetworkNode other) {
        if (Constants.DEBUG) {
            Preconditions.checkState(this.getId() == null || other.getId() == null || this.getId().equals(other.getId()));
            Preconditions.checkNotNull(this.getType());
        }
        doCopyFrom(other, false);
    }

    private void doCopyFrom(NetworkNode other, boolean copyAlsoNodeType) {
        for (NetworkNodeProperty property : other.getInitializedProperties()) {
            if ((copyAlsoNodeType || property != NetworkNodeProperty.NODE_TYPE) && isPropertyRecognized(property)) {
                setProperty(property, other.getProperty(property, true));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> T getProperty(NetworkNodeProperty property, boolean deepCopy) {
        // get shallow copy first
        T propValue = getProperty(property);
        if (propValue == null || !deepCopy) {
            return propValue;
        }
        // else: deep copy if non-null value
        return property.deepCopy(propValue);
    }

    public final <T> T getProperty(NetworkNodeProperty property) {
        if (Constants.DEBUG) {
            if (!isPropertyRecognized(property)) {
                //noinspection ConstantConditions
                Preconditions.checkState(false, "cannot return unrecognized property! " + property + " of " + this);
            }
        }
        //noinspection unchecked
        return (T) valueMap.get(property);
    }

    public final void setProperty(NetworkNodeProperty property, Object value) {
        if (Constants.DEBUG) {
            if (!isPropertyRecognized(property)) {
                //noinspection ConstantConditions
                Preconditions.checkState(false, "cannot set unrecognized property! " + property + " of " + this);
            }
            Preconditions.checkState(property != NetworkNodeProperty.NODE_TYPE || !isPropertyInitialized(NetworkNodeProperty.NODE_TYPE),
                    "node type cannot be set once initialized, it's immutable!");
            if (property == NetworkNodeProperty.ID) {
                // we allow to set ID only if it's not set yet
                Long currNodeId = getId();
                Preconditions.checkState(currNodeId == null || Objects.equal(currNodeId, value), "cannot set node ID: " + value + ", old value = " + currNodeId);
            }
        }
        valueMap.put(property, value);
    }

    @Override
    public Long getId() {
        return getProperty(NetworkNodeProperty.ID);
    }

    @Override
    public NodeType getType() {
        return getProperty(NetworkNodeProperty.NODE_TYPE);
    }

    @Override
    public OperatingFirmware getOperatingFirmware() {
        return getProperty(NetworkNodeProperty.OPERATING_FIRMWARE);
    }

    @Override
    public void setOperatingFirmware(OperatingFirmware operatingFirmware) {
        setProperty(NetworkNodeProperty.OPERATING_FIRMWARE, operatingFirmware);
    }

    @Override
    public String getLabel() {
        return getProperty(NetworkNodeProperty.LABEL);
    }

    @Override
    public Short getNetworkId() {
        return getProperty(NetworkNodeProperty.NETWORK_ID);
    }

    @Override
    public UwbMode getUwbMode() {
        return getProperty(NetworkNodeProperty.UWB_MODE);
    }

    @Override
    public void setLedIndicationEnable(Boolean enable) {
        setProperty(NetworkNodeProperty.LED_INDICATION_ENABLE, enable);
    }

    @Override
    public Boolean isLedIndicationEnable() {
        return getProperty(NetworkNodeProperty.LED_INDICATION_ENABLE);
    }

    @Override
    public void setUwbMode(UwbMode uwbMode) {
        setProperty(NetworkNodeProperty.UWB_MODE, uwbMode);
    }

    @Override
    public void setLabel(String label) {
        setProperty(NetworkNodeProperty.LABEL, label);
    }

    @Override
    public void setPassword(String password) {
        setProperty(NetworkNodeProperty.PASSWORD, password);
    }

    @Override
    public String getPassword() {
        return getProperty(NetworkNodeProperty.PASSWORD);
    }

    @Override
    public void setNetworkId(Short networkId) {
        setProperty(NetworkNodeProperty.NETWORK_ID, networkId);
    }

    @Override
    public String getBleAddress() {
        return getProperty(NetworkNodeProperty.BLE_ADDRESS);
    }

    public void setBleAddress(String bleAddress) {
        setProperty(NetworkNodeProperty.BLE_ADDRESS, bleAddress);
    }

    @Override
    public Integer getHwVersion() {
        return getProperty(NetworkNodeProperty.HW_VERSION);
    }

    void setHwVersion(Integer hwVersion) {
        setProperty(NetworkNodeProperty.HW_VERSION, hwVersion);
    }

    @Override
    public Integer getFw1Version() {
        return getProperty(NetworkNodeProperty.FW1_VERSION);
    }

    void setFw1Version(Integer fw1Version) {
        setProperty(NetworkNodeProperty.FW1_VERSION, fw1Version);
    }

    void setFw2Version(Integer fw2Version) {
        setProperty(NetworkNodeProperty.FW2_VERSION, fw2Version);
    }

    @Override
    public Integer getFw1Checksum() {
        return getProperty(NetworkNodeProperty.FW1_CHECKSUM);
    }

    @Override
    public Integer getFw2Version() {
        return getProperty(NetworkNodeProperty.FW2_VERSION);
    }

    @Override
    public Integer getFw2Checksum() {
        return getProperty(NetworkNodeProperty.FW2_CHECKSUM);
    }

    void setFw1Checksum(Integer fw1Checksum) {
        setProperty(NetworkNodeProperty.FW1_CHECKSUM, fw1Checksum);
    }

    void setFw2Checksum(Integer fw2Checksum) {
        setProperty(NetworkNodeProperty.FW2_CHECKSUM, fw2Checksum);
    }

    @Override
    public Boolean isFirmwareUpdateEnable() {
        return getProperty(NetworkNodeProperty.FIRMWARE_UPDATE_ENABLE);
    }

    @Override
    public void setFirmwareUpdateEnable(Boolean enable) {
        setProperty(NetworkNodeProperty.FIRMWARE_UPDATE_ENABLE, enable);
    }

    @Override
    public NodeStatistics getNodeStatistics() {
        NodeStatistics aStatistics = getProperty(NetworkNodeProperty.NODE_STATISTICS);
        return aStatistics == null ? null : new NodeStatistics(aStatistics);
    }

    void setNodeStatistics(NodeStatistics statistics) {
        setProperty(NetworkNodeProperty.NODE_STATISTICS, statistics);
    }

    @Override
    public LocationDataMode getLocationDataMode() {
        return getProperty(NetworkNodeProperty.LOCATION_DATA_MODE);
    }

    @Override
    public void setLocationDataMode(LocationDataMode locationDataMode) {
        setProperty(NetworkNodeProperty.LOCATION_DATA_MODE, locationDataMode);
    }

    @Override
    public boolean isPropertyRecognized(NetworkNodeProperty property) {
        if (property.extended) {
            // extended properties are not recognized (they belong to enhanced network node)
            return false;
        } // else:
        switch (property) {
            case ID:
            case NODE_TYPE:
            case UWB_MODE:
            case LED_INDICATION_ENABLE:
            case OPERATING_FIRMWARE:
            case FIRMWARE_UPDATE_ENABLE:
            case LABEL:
            case LOCATION_DATA_MODE:
            case BLE_ADDRESS:
            case NETWORK_ID:
            case PASSWORD:
            case HW_VERSION:
            case FW1_VERSION:
            case FW2_VERSION:
            case FW1_CHECKSUM:
            case FW2_CHECKSUM:
            case NODE_STATISTICS:
                return true;
            default:
                return false;
        }
    }

    @Override
    public final boolean isAnchor() {
        return getProperty(NetworkNodeProperty.NODE_TYPE) == NodeType.ANCHOR;
    }

    @Override
    public final boolean isTag() {
        return getProperty(NetworkNodeProperty.NODE_TYPE) == NodeType.TAG;
    }

    @Override
    public Set<NetworkNodeProperty> getInitializedProperties() {
        return Collections.unmodifiableSet(valueMap.keySet());
    }

    @Override
    public boolean isPropertyInitialized(NetworkNodeProperty networkNodeProperty) {
        return valueMap.containsKey(networkNodeProperty);
    }

    @Override
    public final CompareResult compareTo(@NotNull NetworkNode networkNode) {
        Set<NetworkNodeProperty> l = compareByProperty(networkNode);
        if (l.isEmpty()) {
            return CompareResult.NO_CHANGE;
        } else {
            return Util.anyStaticProperty(l) ? CompareResult.STATIC_CHANGE : CompareResult.DYNAMIC_CHANGE;
        }
    }

    @NotNull
    @Override
    public final Set<NetworkNodeProperty> compareByProperty(@NotNull NetworkNode other) {
        if (this == other) return Collections.emptySet();
        if (!getType().equals(other.getType())) {
            // stop now, the property set is different
            return Sets.immutableEnumSet(NetworkNodeProperty.NODE_TYPE);
        }
        Set<NetworkNodeProperty> l = EnumSet.noneOf(NetworkNodeProperty.class);
        // aggregate non-equal initialized properties
        for (NetworkNodeProperty property : other.getInitializedProperties()) {
            if (this.isPropertyInitialized(property)) {
                Object myProp = this.getProperty(property);
                Object otherProp = other.getProperty(property);
                if (!Objects.equal(myProp, otherProp)) {
                    l.add(property);
                }
            }
        }
        return l;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkNodeImpl that = (NetworkNodeImpl) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }

    @Override
    public final int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        if (!valueMap.isEmpty()) {
            // order the keys alphabetically
            List<NetworkNodeProperty> keys = new ArrayList<>(valueMap.keySet());
            Collections.sort(keys, (nnp1,nnp2) -> nnp1.name().compareTo(nnp2.name()));
            for (NetworkNodeProperty property : keys) {
                Object value = valueMap.get(property);
                sb.append(property)
                        .append("=");
                if ((property != NetworkNodeProperty.TAG_UPDATE_RATE && property != NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE)
                        && (value instanceof Short || value instanceof Integer || value instanceof Long)) {
                    // format as hexa
                    sb.append(formatAsHexa((Number) value));
                } else if (property == NetworkNodeProperty.ANCHOR_AN_LIST) {
                    sb.append('[');
                    //noinspection unchecked
                    List<Short> lValue = (List<Short>) value;
                    for (Short aShort : lValue) {
                        sb.append(formatAsHexa(aShort));
                        sb.append(" ");
                    }
                    sb.append(']');
                } else {
                    // simply toString()
                    sb.append(value);
                }
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

}
