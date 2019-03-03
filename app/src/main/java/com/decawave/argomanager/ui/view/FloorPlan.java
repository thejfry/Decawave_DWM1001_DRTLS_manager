/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */
public class FloorPlan {
    public int pxCenterX;
    public int pxCenterY;
    public int tenMetersInPixels;
    public int rotation;
    //
    private final String floorPlanFileName;
    private Bitmap bitmap;
    // cache
    private Integer bitmapHeight;
    private Integer bitmapWidth;

    public FloorPlan(String floorPlanFileName,
                     int pxCenterX,
                     int pxCenterY,
                     int rotation,
                     int tenMetersInPixels) {
        this.floorPlanFileName = floorPlanFileName;
        this.pxCenterX = pxCenterX;
        this.pxCenterY = pxCenterY;
        this.rotation = rotation;
        this.tenMetersInPixels = tenMetersInPixels;
    }

    public static FloorPlan copyNullSafe(@Nullable FloorPlan other) {
        if (other == null) {
            return null;
        } // else:
        return new FloorPlan(other.getFloorPlanFileName(), other.pxCenterX, other.pxCenterY, other.rotation, other.tenMetersInPixels);
    }

    Bitmap getBitmap() {
        // initialize lazily
        if (bitmap == null) {
            bitmap = newBitmapFromFile(getFloorPlanFileName());
        }
        return bitmap;
    }

    Integer getBitmapHeight() {
        if (bitmapHeight == null) {
            bitmapHeight = getBitmap().getHeight();
        }
        return bitmapHeight;
    }

    Integer getBitmapWidth() {
        if (bitmapWidth == null) {
            bitmapWidth = getBitmap().getWidth();
        }
        return bitmapWidth;
    }

    public static Bitmap newBitmapFromFile(String floorPlanFileName) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        File file = new File(daApp.getFilesDir(), floorPlanFileName);
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    public void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
            bitmapHeight = null;
            bitmapWidth = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FloorPlan floorPlan = (FloorPlan) o;

        if (pxCenterX != floorPlan.pxCenterX) return false;
        if (pxCenterY != floorPlan.pxCenterY) return false;
        if (rotation != floorPlan.rotation) return false;
        //noinspection SimplifiableIfStatement
        if (tenMetersInPixels != floorPlan.tenMetersInPixels) return false;
        return getFloorPlanFileName() != null ? getFloorPlanFileName().equals(floorPlan.getFloorPlanFileName()) : floorPlan.getFloorPlanFileName() == null;

    }

    @Override
    public int hashCode() {
        int result = pxCenterX;
        result = 31 * result + pxCenterY;
        result = 31 * result + rotation;
        result = 31 * result + tenMetersInPixels;
        result = 31 * result + (getFloorPlanFileName() != null ? getFloorPlanFileName().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FloorPlan{" +
                "pxCenterX=" + pxCenterX +
                ", pxCenterY=" + pxCenterY +
                ", tenMetersInPixels=" + tenMetersInPixels +
                ", floorPlanFileName='" + getFloorPlanFileName() + '\'' +
                '}';
    }

    public String getFloorPlanFileName() {
        return floorPlanFileName;
    }

}
