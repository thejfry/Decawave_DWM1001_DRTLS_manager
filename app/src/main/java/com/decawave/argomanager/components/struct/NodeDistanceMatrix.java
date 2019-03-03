/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

import com.decawave.argo.api.struct.Distance;
import com.decawave.argomanager.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains matrix of retrieved distances.
 */
public class NodeDistanceMatrix {

    private Map<Long, Map<Long, Distance>> distancesFromTo = new HashMap<>();

    public void putDistance(long fromNode, long toNode, Distance distance) {
        Map<Long, Distance> nodeDistances = distancesFromTo.get(fromNode);
        if (nodeDistances == null) {
            nodeDistances = new HashMap<>();
            distancesFromTo.put(fromNode, nodeDistances);
        }
        nodeDistances.put(toNode, distance);
    }

    public Set<Long> getSourceNodes() {
        return Collections.unmodifiableSet(distancesFromTo.keySet());
    }

    public Set<Long> getTargetNodes(long sourceNodeId) {
        Map<Long, Distance> targetNodes = distancesFromTo.get(sourceNodeId);
        if (targetNodes != null) {
            return Collections.unmodifiableSet(targetNodes.keySet());
        } // else:
        return null;
    }

    public Distance evaluateDistance(long fromId, long toId) {
        Distance d1 = getLowLevelDistance(fromId, toId);
        Distance d2 = getLowLevelDistance(toId, fromId);
        if (d1 == null || d2 == null) {
            // this might still return null
            return d1 != null ? d1 : d2;
        } // else:
        if (d1.quality == 0 && d2.quality == 0) {
            // zero-quality distance
            return new Distance((int) ((d1.length + d2.length) / 2 + 0.5), (byte) 0);
        } else if (d1.quality == 0 && d2.quality > 0) {
            return d2;
        } else if (d2.quality == 0 && d1.quality > 0) {
            return d1;
        }
        // else: return weighted distance
        return new Distance(
                (int) ((d1.length * d1.quality + d2.length * d2.quality) / (d1.quality + d2.quality) + 0.5),
                (byte) ((d1.quality + d2.quality) / 2 + 0.5));
    }

    private Distance getLowLevelDistance(long fromNode, long toNode) {
        Map<Long, Distance> nodeDistances = distancesFromTo.get(fromNode);
        if (nodeDistances != null) {
            return nodeDistances.get(toNode);
        }
        return null;
    }

    public void clear() {
        distancesFromTo.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NodeDistanceMatrix{\n");
        for (Map.Entry<Long, Map<Long, Distance>> longMapEntry : distancesFromTo.entrySet()) {
            sb.append(Util.shortenNodeId(longMapEntry.getKey(), false)).append(": ");
            Map<Long, Distance> distanceMap = longMapEntry.getValue();
            boolean comma = false;
            for (Map.Entry<Long, Distance> longDistanceEntry : distanceMap.entrySet()) {
                if (!comma) {
                    comma = true;
                } else {
                    sb.append(",");
                }
                Distance d = longDistanceEntry.getValue();
                sb.append("--").append(d.length).append("--[q=").append(d.quality).append("]-->").append(Util.shortenNodeId(longDistanceEntry.getKey(), false));
            }
            sb.append('\n');
        }
        sb.append('}');
        return sb.toString();
    }

    public boolean hasDistanceInDirection(Long targetId, Long sourceId) {
        Map<Long, Distance> dm = distancesFromTo.get(targetId);
        return dm != null && dm.containsKey(sourceId);
    }

    // return distance matrix where present short node ids are mapped to long node ids
    public NodeDistanceMatrix toLongDistanceMatrix(ArrayList<Long> nodeIdMapping) {
        // create a helper map first
        Map<Short, Long> shortToLongMap = new HashMap<>();
        for (Long nodeId : nodeIdMapping) {
            shortToLongMap.put(nodeId.shortValue(), nodeId);
        }
        // create a new distance matrix
        NodeDistanceMatrix r = new NodeDistanceMatrix();
        for (Map.Entry<Long, Map<Long, Distance>> nodeIdToDistanceMap : distancesFromTo.entrySet()) {
            Map<Long, Distance> distanceMap = nodeIdToDistanceMap.getValue();
            Map<Long, Distance> distanceMapCopy = new HashMap<>();
            for (Map.Entry<Long, Distance> longDistanceEntry : distanceMap.entrySet()) {
                distanceMapCopy.put(shortToLongMap.get(longDistanceEntry.getKey().shortValue()), longDistanceEntry.getValue());
            }
            r.distancesFromTo.put(shortToLongMap.get(nodeIdToDistanceMap.getKey().shortValue()), distanceMapCopy);
        }
        return r;
    }
}
