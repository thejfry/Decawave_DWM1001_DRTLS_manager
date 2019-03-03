/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Argo project.
 */
public class NetworkNodeEnhancedImpl implements NetworkNodeEnhanced {
    // plain delegate
    private final NetworkNode delegate;
    // enriching read-only node properties shown in UI
    private Long lastSeen;
    // these are considered network-specific read/write UI properties
    private TrackMode trackMode;
    private List<NodeWarning> warnings;


    public NetworkNodeEnhancedImpl(@NotNull NetworkNode delegate) {
        this.delegate = delegate;
        this.lastSeen = System.currentTimeMillis();
    }

    public NetworkNodeEnhancedImpl(@NotNull NetworkNode delegate, Long lastSeen) {
        this.delegate = delegate;
        this.lastSeen = lastSeen;
    }

    @NotNull
    @Override
    public TrackMode getTrackMode() {
        return trackMode == null ? TrackMode.TRACKED_POSITION : trackMode;
    }

    public void setTrackMode(TrackMode trackMode) {
        this.trackMode = trackMode;
    }

    public Long getId() {
        return delegate.getId();
    }

    @Override
    public String getBleAddress() {
        return delegate.getBleAddress();
    }

    @Override
    public boolean isTag() {
        return delegate.isTag();
    }

    @Override
    public boolean isAnchor() {
        return delegate.isAnchor();
    }

    public @NotNull NetworkNode asPlainNode() {
        return delegate;
    }

    /**
     * Updates internal last seen timestamp.
     * This is updated whenever the node is discovered, connected and disconnected.
     */
    public void touchLastSeen() {
        lastSeen = System.currentTimeMillis();
    }

    public Long getLastSeen() {
        return lastSeen;
    }

    public Object getProperty(NetworkNodeProperty property) {
        switch (property) {
            case LAST_SEEN:
                return lastSeen;
            default:
                return delegate.getProperty(property);
        }
    }

    @NotNull
    @Override
    public List<NodeWarning> getWarnings() {
        return warnings == null ? Collections.emptyList() : warnings;
    }

    public void setWarnings(List<NodeWarning> warnings) {
        this.warnings = warnings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkNodeEnhancedImpl that = (NetworkNodeEnhancedImpl) o;

        return delegate.equals(that.delegate);
    }


    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return "NetworkNodeEnhancedImpl{" +
                "delegate=" + delegate +
                ", lastSeen=" + lastSeen +
                ", trackMode=" + trackMode +
                '}';
    }

}
