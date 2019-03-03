/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.struct;

import android.support.annotation.NonNull;

import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Argo project.
 */
public class NodeWarning {

    @SuppressWarnings("WeakerAccess")
    public enum Type {
        UWB_OFF,
        TAG_UWB_PASSIVE,
        OTHER_ANCHOR_SAME_POSITION
    }

    @NotNull
    public final Type type;
    public final Set<Long> relatedNodesParam;

    private NodeWarning(@NonNull Type type) {
        this.type = type;
        this.relatedNodesParam = null;
    }

    private NodeWarning(@NonNull Type type, Set<Long> relatedNodesParam) {
        this.type = type;
        this.relatedNodesParam = relatedNodesParam;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeWarning that = (NodeWarning) o;

        //noinspection SimplifiableIfStatement
        if (type != that.type) return false;
        return relatedNodesParam != null ? relatedNodesParam.equals(that.relatedNodesParam) : that.relatedNodesParam == null;

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (relatedNodesParam != null ? relatedNodesParam.hashCode() : 0);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // factory methods
    ///////////////////////////////////////////////////////////////////////////


    public static NodeWarning newUwbOffWarning() {
        return new NodeWarning(Type.UWB_OFF);
    }

    public static NodeWarning newTagUwbPassiveWarning() {
        return new NodeWarning(Type.TAG_UWB_PASSIVE);
    }

    public static NodeWarning newAnchorSamePositionWarning(Set<Long> samePositionNodes) {
        return new NodeWarning(Type.OTHER_ANCHOR_SAME_POSITION, samePositionNodes);
    }

    public static NodeWarning newAnchorSamePositionWarning(Long... samePositionNodes) {
        return new NodeWarning(Type.OTHER_ANCHOR_SAME_POSITION, Sets.newHashSet(samePositionNodes));
    }

}
