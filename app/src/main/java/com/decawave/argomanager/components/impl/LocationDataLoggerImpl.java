/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryTagFactory;
import com.decawave.argomanager.util.Util;

import java.util.List;

import javax.inject.Inject;

import eu.kryl.android.common.log.ComponentLog;


/**
 * Default location data logger implementation.
 */
public class LocationDataLoggerImpl implements LocationDataLogger {
    private static final ComponentLog log = new ComponentLog(LocationDataLogger.class);
    private static final ApplicationComponentLog positionLog = ApplicationComponentLog.newPositionLog(log);

    @Inject
    LocationDataLoggerImpl() {
    }

    @Override
    public void setInitialPosition(long nodeId, String nodeBle, Position position) {
        if (Constants.DEBUG) {
            log.d("setInitialPosition: " + "nodeId = [" + nodeId + "], position = [" + position + "]");
        }
        wrkSb.setLength(0);
        wrkSbAppendPosition(Util.formatAsHexa(nodeId, true), "initial-position", position);
        positionLog.i(wrkSb.toString(), LogEntryTagFactory.getDeviceLogEntryTag(nodeBle));
    }

    private StringBuilder wrkSb = new StringBuilder();

    @Override
    public void logLocationData(long nodeId,
                                String nodeBle,
                                Position position,
                                List<RangingAnchor> distances,
                                boolean fromProxy) {
        if (Constants.DEBUG) {
            log.d("logLocationData: " + "nodeId = [" + nodeId + "], nodeBle = [" + nodeBle + "], position = [" + position + "], distances = [" + distances + "], fromProxy = [" + fromProxy + "]");
        }
        wrkSb.setLength(0);
        boolean first = true;
        wrkSb.append(Util.shortenNodeId(nodeId, true))
                .append(" location data");
        if (fromProxy) wrkSb.append(" (proxy)");
        wrkSb.append(": ");
        if (position != null) {
            wrkSbAppendPosition(null, "position", position);
            wrkSb.append("; ");
        }
        if (distances != null) {
            wrkSb.append("distances: ");
            for (RangingAnchor rangingAnchor : distances) {
                if (!first) {
                    wrkSb.append(", ");
                } else {
                    first = false;
                }
                wrkSb.append(Util.shortenNodeId(rangingAnchor.nodeId, false))
                        .append(" distance=")
                        .append(rangingAnchor.distance);
            }
        }
        positionLog.i(wrkSb.toString(), LogEntryTagFactory.getDeviceLogEntryTag(nodeBle));
    }

    private void wrkSbAppendPosition(String nodeIdFormatted, String positionLabel, Position position) {
        if (nodeIdFormatted != null) {
            wrkSb.append(nodeIdFormatted)
                    .append(" ");
        }
        wrkSb.append(positionLabel).append(": x=")
                .append(position.x)
                .append(" y=")
                .append(position.y)
                .append(" z=")
                .append(position.z)
                .append(" q=")
                .append(position.qualityFactor);
    }

}