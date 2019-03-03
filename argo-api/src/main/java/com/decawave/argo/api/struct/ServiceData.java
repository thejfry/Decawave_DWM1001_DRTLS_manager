/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

/**
 * Broadcasted service data decomposed into java object.
 */
@SuppressWarnings("WeakerAccess")
public class ServiceData {
    public NodeType operationMode;
    public boolean initiator;
    public boolean bridge;
    public boolean online;
    public byte changeCounter;

    public ServiceData() {
    }

    public ServiceData(ServiceData other) {
        this.operationMode = other.operationMode;
        this.initiator = other.initiator;
        this.bridge = other.bridge;
        this.online = other.online;
        this.changeCounter = other.changeCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceData that = (ServiceData) o;

        if (initiator != that.initiator) return false;
        if (bridge != that.bridge) return false;
        if (online != that.online) return false;
        //noinspection SimplifiableIfStatement
        if (changeCounter != that.changeCounter) return false;
        return operationMode == that.operationMode;

    }

    @Override
    public int hashCode() {
        int result = operationMode != null ? operationMode.hashCode() : 0;
        result = 31 * result + (initiator ? 1 : 0);
        result = 31 * result + (bridge ? 1 : 0);
        result = 31 * result + (online ? 1 : 0);
        result = 31 * result + (int) changeCounter;
        return result;
    }

    @Override
    public String toString() {
        return "ServiceData{" +
                "operationMode=" + operationMode +
                ", initiator=" + initiator +
                ", bridge=" + bridge +
                ", online=" + online +
                ", changeCounter=" + changeCounter +
                '}';
    }
}
