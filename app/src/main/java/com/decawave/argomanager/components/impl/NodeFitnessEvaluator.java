/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.os.SystemClock;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreterImpl;
import com.decawave.argomanager.components.BlePresenceApi;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.kryl.android.common.Pair;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */
class NodeFitnessEvaluator {
    private static final ComponentLog log = new ComponentLog(NodeFitnessEvaluator.class);
    // configuration
    private static final int REMEMBER_LAST_N_CONNECTIONS = 5;
    private static final int CONSIDER_LAST_N_MINUTES = 2;
    private static final int EXP_SCALE = 4;

    // derived constants
    private static final int N_MINUTES_MS = 2 * 60 * 1000;
    private static final int SCALE = (int) Math.round(Math.pow(2, EXP_SCALE));
    private static final int RSSI_DIFF = SignalStrengthInterpreterImpl.DEFAULT_LARGEST_RSSI - SignalStrengthInterpreterImpl.VERY_LOW_RSSI;

    // state
    private final BlePresenceApi presenceApi;
    private Map<String, ConnectionFitness> connectionFitnessMap;

    private class ConnectionFitness {
        private List<Pair<Boolean,Long>> results;

        ConnectionFitness() {
            this.results = new LinkedList<>();
        }

        void onConnectionResult(boolean success) {
            // add to the end
            results.add(new Pair<>(success, SystemClock.uptimeMillis()));
            while (results.size() > REMEMBER_LAST_N_CONNECTIONS) {
                // remove from the beginning
                results.remove(0);
            }
        }

        // return <-1;1> based on previous connections results
        float evaluate() {
            // exponentially assign weight to connection results from the last N minutes only
            long now = SystemClock.uptimeMillis();
            long twoMinutesAgo = now - CONSIDER_LAST_N_MINUTES * 60 * 1000;
            // initial value (we do not know)
            float r = 0;
            for (Pair<Boolean, Long> result : results) {
                if (twoMinutesAgo < result.second) {
                    float age = now - result.second;
                    // transform to 0 - EXP_SCALE scale
                    float scaledAge = EXP_SCALE * age / N_MINUTES_MS;
                    // transform to 1 - 0 scale (invert) (exponentially)
                    float expAgeInverted = 1 - (float) Math.pow(2, scaledAge) / SCALE;
                    // we've got the weight, assign the weight to: fail = -1, or success = 1
                    r += expAgeInverted * (result.first ? 1 : -1);
                }
            }
            return r;
        }

    }

    NodeFitnessEvaluator(BlePresenceApi presenceApi) {
        this.presenceApi = presenceApi;
        this.connectionFitnessMap = new HashMap<>();
    }

    void onConnectionClosed(String bleAddress, boolean connectionSuccessful) {
        ConnectionFitness connectionFitness = connectionFitnessMap.get(bleAddress);
        if (connectionFitness == null) {
            connectionFitness = new ConnectionFitness();
            connectionFitnessMap.put(bleAddress, connectionFitness);
        }
        connectionFitness.onConnectionResult(connectionSuccessful);
    }

    /**
     * Returns the node fitness.
     * Fitness is a number in <-1;+1> interval expressing how fit is the given node.
     * Considers both RSSI and connection quality (if known).
     *
     * @param bleAddress identifies the node
     * @return node fitness expressed as a number
     */
    float getNodeFitness(String bleAddress) {
        // initialize to defaults
        float fRssiFitness = 0;
        float fConnectionFitness = 0;
        // now compute
        Integer rssi = presenceApi.getAgingNodeRssi(bleAddress);
        if (rssi != null) {
            // compute rssi fitness: normalize to 0..RSSI_DIFF
            float normalizedRssi = Math.min(SignalStrengthInterpreterImpl.DEFAULT_LARGEST_RSSI, Math.max(rssi, SignalStrengthInterpreterImpl.VERY_LOW_RSSI)) - SignalStrengthInterpreterImpl.VERY_LOW_RSSI;
            // transform to 0,1 then to -.5;+.5 and then to -1;+1
            fRssiFitness = 2 * ((normalizedRssi / RSSI_DIFF) - 0.5f);
        }
        ConnectionFitness connectionFitness = connectionFitnessMap.get(bleAddress);
        if (connectionFitness != null) {
            // evaluate connection fitness
            fConnectionFitness = connectionFitness.evaluate();
        }
        float f = (fRssiFitness + fConnectionFitness) / 2;
        if (Constants.DEBUG) {
            log.d("getNodeFitness: " + "bleAddress = [" + bleAddress + "] returning: " + f);
        }
        return f;
    }

}
