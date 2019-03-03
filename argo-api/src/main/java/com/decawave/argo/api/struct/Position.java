/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import org.jetbrains.annotations.NotNull;

/**
 * Position in 3D measured relatively from 0,0,0.
 *
 * The shortest distance (delta) is 1mm.
 */
public class Position {
    public static final byte MAX_QUALITY_FACTOR = 100;
    // relative X coordinate
    public int x;
    // relative Y coordinate
    public int y;
    // relative Z coordinate
    public int z;
    // quality factor: 0 - 100
    public Byte qualityFactor;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(int x, int y, int z, Byte qualityFactor) {
        this(x, y, z);
        this.qualityFactor = qualityFactor;
    }

    public Position() {
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    public Position(@NotNull Position position) {
        copyFrom(position);
    }

    public void divide(int factor, Position targetPosition) {
        targetPosition.x = (int) Math.round(x / (double) factor);
        targetPosition.y = (int) Math.round(y / (double) factor);
        targetPosition.z = (int) Math.round(z / (double) factor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (x != position.x) return false;
        if (y != position.y) return false;
        //noinspection SimplifiableIfStatement
        if (z != position.z) return false;
        return qualityFactor != null ? qualityFactor.equals(position.qualityFactor) : position.qualityFactor == null;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        result = 31 * result + (qualityFactor != null ? qualityFactor.hashCode() : 0);
        return result;
    }

    public void copyFrom(@NotNull Position source) {
        this.z = source.z;
        this.y = source.y;
        this.x = source.x;
        this.qualityFactor = source.qualityFactor;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", qualityFactor=" + qualityFactor +
                '}';
    }

    public boolean equalsInCoordinates(Position pos) {
        return pos != null
                && this.x == pos.x
                && this.y == pos.y
                && this.z == pos.z;
    }
}
