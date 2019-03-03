/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.decawave.argo.api.ConnectionState;
import com.decawave.argo.api.interaction.NetworkNodeConnection;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.util.Collection;
import java.util.Iterator;

/**
 * Argo project.
 */

public class ConnectionContainer {
    private Multimap<String, NetworkNodeConnection> connectionMap;

    ConnectionContainer() {
        this.connectionMap = MultimapBuilder.hashKeys().linkedListValues().build();
    }

    public void put(String bleAddress, NetworkNodeConnection connection) {
        // cleanup previous, now CLOSED, connections
        Collection<NetworkNodeConnection> connections = this.connectionMap.get(bleAddress);
        Iterator<NetworkNodeConnection> it = connections.iterator();
        while (it.hasNext()) {
            NetworkNodeConnection nnc = it.next();
            if (nnc.getState() == ConnectionState.CLOSED) {
                it.remove();
            }
        }
        // insert a new connection
        this.connectionMap.put(bleAddress, connection);
    }

    /**
     * @param bleAddress identifies the connection
     * @return a connection with affinity to CONNECTED state
     */
    NetworkNodeConnection affinityGet(String bleAddress) {
        NetworkNodeConnection winner = null;
        // we return a 'live' connection preferentially
        Collection<NetworkNodeConnection> connections = connectionMap.get(bleAddress);
        for (NetworkNodeConnection connection : connections) {
            if (winner == null) {
                winner = connection;
            } else if (compare(winner.getState(), connection.getState()) < 0) {
                // the winner is of more significance
                winner = connection;
                if (mapToSignificance(winner.getState()) == 2) {
                    // we are done (there cannot be two connections in '2' state significance)
                    return winner;
                }
            }
        }
        return winner;
    }

    private int compare(ConnectionState lesser, ConnectionState greater) {
        return mapToSignificance(lesser) - mapToSignificance(greater);
    }

    private int mapToSignificance(ConnectionState connectionState) {
        switch (connectionState) {
            case CLOSED:
                // the least interesting state
                return 0;
            case PENDING:
                // more interesting state
                return 1;
            case CONNECTING:
            case CONNECTED:
            case DISCONNECTING:
                // the most interesting state
                return 2;
            default:
                throw new IllegalStateException("unexpected connection state: " + connectionState);
        }
    }

    public Stream<NetworkNodeConnection> values(Predicate<NetworkNodeConnection> connectionPredicate) {
        return Stream.of(connectionMap.values()).filter(connectionPredicate);
    }

}
