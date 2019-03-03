/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.support.annotation.NonNull;

import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ext.UpdateRate;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static com.decawave.argomanager.ArgoApp.daApp;
import static com.decawave.argomanager.argoapi.ext.UpdateRate.getUpdateRateForValue;

/**
 * Argo project.
 */
public class NetworkNodePropertyDecoratorImpl implements NetworkNodePropertyDecorator {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private LoadingCache<NetworkNodeProperty, DecoratedProperty> cache;

    @Inject
    NetworkNodePropertyDecoratorImpl(AppPreferenceAccessor appPreferenceAccessor) {
        cache = CacheBuilder.newBuilder().build(new CacheLoader<NetworkNodeProperty, DecoratedProperty>() {
            @Override
            public DecoratedProperty load(@NonNull NetworkNodeProperty property) throws Exception {
                String label = resolvePropertyLabel(property, appPreferenceAccessor);
                NetworkNodePropertyValueFormatter formatter = resolvePropertyValueFormatter(property, appPreferenceAccessor);
                return new DecoratedProperty(property, label, formatter);
            }
        });
    }

    @Override
    public DecoratedProperty decorate(NetworkNodeProperty property) {
        try {
            return cache.get(property);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static Date wrkDate = new Date();

    private static final NetworkNodePropertyValueFormatter<Long> dateFormatter = (aLong) -> {
        wrkDate.setTime(aLong);
        return DATE_FORMAT.format(wrkDate);
    };
    private static final NetworkNodePropertyValueFormatter<String> stringFormatter = (str) -> str;
    private static final NetworkNodePropertyValueFormatter<Object> toStringFormatter = Object::toString;
    private static final NetworkNodePropertyValueFormatter<Long> hexaOxLongFormatter = (number) -> Util.formatAsHexa(number, true);
    private static final NetworkNodePropertyValueFormatter<Integer> hexaOxIntFormatter = (number) -> Util.formatIntAsHexa(number);
    private static final NetworkNodePropertyValueFormatter<Short> hexaOxShortFormatter = (number) -> Util.formatAsHexa(number, true);
    private static final NetworkNodePropertyValueFormatter<NodeType> nodeTypeFormatter = Util::nodeTypeString;
    private static final NetworkNodePropertyValueFormatter<OperatingFirmware> operatingFirmwareFormatter = Util::operatingFirmwareString;
    private static final NetworkNodePropertyValueFormatter<Long> hexaColonFormatter = Util::formatAsColonHexa;
    private static final NetworkNodePropertyValueFormatter<LocationDataMode> locationDataModeFormatter = Util::formatLocationDataMode;
    private static final NetworkNodePropertyValueFormatter<UwbMode> uwbModeFormatter = Util::formatUwbMode;
    private static final NetworkNodePropertyValueFormatter<Boolean> booleanEnabledDisabledFormatter = (bool) -> daApp.getString(bool ? R.string.enabled : R.string.disabled);
    private static final NetworkNodePropertyValueFormatter<Boolean> booleanEnabledDisabledInvertedFormatter = (bool) -> daApp.getString(bool ? R.string.disabled : R.string.enabled);
    private static final NetworkNodePropertyValueFormatter<Integer> updateRateFormatter = (updateRate) -> {
        UpdateRate ur = updateRate == null ? null : getUpdateRateForValue(updateRate);
        return daApp.getString(ur == null ? R.string.update_rate_default : ur.text);
    };

    private static NetworkNodePropertyValueFormatter<Position> positionFormatter = null;

    private static NetworkNodePropertyValueFormatter resolvePropertyValueFormatter(NetworkNodeProperty property, AppPreferenceAccessor appPreferenceAccessor) {
        switch (property) {
            case ID:
                return hexaOxLongFormatter;
            case NODE_TYPE:
                return nodeTypeFormatter;
            case OPERATING_FIRMWARE:
                return operatingFirmwareFormatter;
            case LOCATION_DATA_MODE:
                return locationDataModeFormatter;
            case UWB_MODE:
                return uwbModeFormatter;
            case LABEL:
            case BLE_ADDRESS:
                return stringFormatter;
            case TAG_UPDATE_RATE:
            case TAG_STATIONARY_UPDATE_RATE:
                return updateRateFormatter;
            case ANCHOR_SEAT:
                return toStringFormatter;
            case NETWORK_ID:
            case ANCHOR_CLUSTER_MAP:
            case ANCHOR_CLUSTER_NEIGHBOUR_MAP:
                return hexaOxShortFormatter;
            case ANCHOR_MAC_STATS:
            case HW_VERSION:
            case FW1_VERSION:
            case FW2_VERSION:
            case FW1_CHECKSUM:
            case FW2_CHECKSUM:
                return hexaOxIntFormatter;
            case LAST_SEEN:
                return dateFormatter;
            case FIRMWARE_UPDATE_ENABLE:
            case TAG_ACCELEROMETER_ENABLE:
            case TAG_LOCATION_ENGINE_ENABLE:
            case LED_INDICATION_ENABLE:
            case ANCHOR_INITIATOR:
            case ANCHOR_BRIDGE:
                return booleanEnabledDisabledFormatter;
            case TAG_LOW_POWER_MODE_ENABLE:
                return booleanEnabledDisabledInvertedFormatter;
            case ANCHOR_POSITION:
                if (positionFormatter == null) {
                    // create the instance
                    positionFormatter = (position) -> Util.formatPosition(position, appPreferenceAccessor.getLengthUnit());
                }
                return positionFormatter;
            default:
                throw new Fixme("property " + property + " formatter not supported");
        }
    }

    private String resolvePropertyLabel(NetworkNodeProperty property, AppPreferenceAccessor appPreferenceAccessor) {
        switch (property) {
            case ID:
                return daApp.getString(R.string.node_detail_id);
            case NODE_TYPE:
                return daApp.getString(R.string.node_detail_node_type);
            case OPERATING_FIRMWARE:
                return daApp.getString(R.string.node_detail_operating_firmware);
            case UWB_MODE:
                return daApp.getString(R.string.node_detail_uwb_mode);
            case FIRMWARE_UPDATE_ENABLE:
                return daApp.getString(R.string.node_detail_firmware_update);
            case LED_INDICATION_ENABLE:
                return daApp.getString(R.string.node_detail_led_indication);
            case LABEL:
                return daApp.getString(R.string.node_detail_label);
            case ANCHOR_POSITION:
                return daApp.getString(R.string.node_detail_position, daApp.getString(appPreferenceAccessor.getLengthUnit().unitLabelResource));
            case LOCATION_DATA_MODE:
                return daApp.getString(R.string.node_detail_location_data_mode);
            case TAG_UPDATE_RATE:
                return daApp.getString(R.string.node_detail_update_rate);
            case TAG_ACCELEROMETER_ENABLE:
                return daApp.getString(R.string.node_detail_accelerometer);
            case TAG_STATIONARY_UPDATE_RATE:
                return daApp.getString(R.string.node_detail_stationary_update_rate);
            case NODE_STATISTICS:
                return daApp.getString(R.string.node_detail_statistics);
            case BLE_ADDRESS:
                return daApp.getString(R.string.node_detail_ble_address);
            case NETWORK_ID:
                return daApp.getString(R.string.node_detail_network);
            case PASSWORD:
                return daApp.getString(R.string.node_detail_password);
            case HW_VERSION:
                return daApp.getString(R.string.node_detail_hw_version);
            case FW1_VERSION:
                return daApp.getString(R.string.node_detail_fw1_version);
            case FW1_CHECKSUM:
                return daApp.getString(R.string.node_detail_fw1_checksum);
            case FW2_VERSION:
                return daApp.getString(R.string.node_detail_fw2_version);
            case FW2_CHECKSUM:
                return daApp.getString(R.string.node_detail_fw2_checksum);
            case ANCHOR_INITIATOR:
                return daApp.getString(R.string.node_detail_initiator);
            case ANCHOR_BRIDGE:
                return daApp.getString(R.string.node_detail_bridge);
            case ANCHOR_SEAT:
                return daApp.getString(R.string.node_detail_seat_number);
            case ANCHOR_CLUSTER_MAP:
                return daApp.getString(R.string.node_detail_cluster_map);
            case ANCHOR_CLUSTER_NEIGHBOUR_MAP:
                return daApp.getString(R.string.node_detail_cluster_neighbour_map);
            case ANCHOR_MAC_STATS:
                return daApp.getString(R.string.node_detail_mac_stats);
            case ANCHOR_AN_LIST:
                return daApp.getString(R.string.node_detail_anchor_list);
            case TAG_LOW_POWER_MODE_ENABLE:
                return daApp.getString(R.string.node_detail_responsive_mode);
            case TAG_LOCATION_ENGINE_ENABLE:
                return daApp.getString(R.string.node_detail_location_engine);
            case LAST_SEEN:
                return daApp.getString(R.string.node_detail_last_seen);

            default:
                throw new Fixme("property label not configured for " + property);
        }
    }

}
