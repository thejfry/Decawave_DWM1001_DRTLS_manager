/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import com.google.common.base.Preconditions;

/**
 * Matrix wrapper printing debug messages when write operations are being performed.
 */
@SuppressWarnings("ImplicitArrayToString")
class MatrixWrapper extends Matrix {
    private static final String TAG = "ARGO.MATRIX";

    private Float scale;


    @Override
    public void setTranslate(float dx, float dy) {
        Log.d(TAG, "setTranslate() called with: dx = [" + dx + "], dy = [" + dy + "]");
        super.setTranslate(dx, dy);
    }

    @Override
    public void setScale(float sx, float sy, float px, float py) {
        Log.d(TAG, "setScale() called with: sx = [" + sx + "], sy = [" + sy + "], px = [" + px + "], py = [" + py + "]");
        super.setScale(sx, sy, px, py);
    }

    @Override
    public void setScale(float sx, float sy) {
        Log.d(TAG, "setScale() called with: sx = [" + sx + "], sy = [" + sy + "]");
        super.setScale(sx, sy);
    }

    @Override
    public void setRotate(float degrees, float px, float py) {
        Log.d(TAG, "setRotate() called with: degrees = [" + degrees + "], px = [" + px + "], py = [" + py + "]");
        super.setRotate(degrees, px, py);
    }

    @Override
    public void setRotate(float degrees) {
        Log.d(TAG, "setRotate() called with: degrees = [" + degrees + "]");
        super.setRotate(degrees);
    }

    @Override
    public void setSinCos(float sinValue, float cosValue, float px, float py) {
        Log.d(TAG, "setSinCos() called with: sinValue = [" + sinValue + "], cosValue = [" + cosValue + "], px = [" + px + "], py = [" + py + "]");
        super.setSinCos(sinValue, cosValue, px, py);
    }

    @Override
    public void setSinCos(float sinValue, float cosValue) {
        Log.d(TAG, "setSinCos() called with: sinValue = [" + sinValue + "], cosValue = [" + cosValue + "]");
        super.setSinCos(sinValue, cosValue);
    }

    @Override
    public void setSkew(float kx, float ky, float px, float py) {
        Log.d(TAG, "setSkew() called with: kx = [" + kx + "], ky = [" + ky + "], px = [" + px + "], py = [" + py + "]");
        super.setSkew(kx, ky, px, py);
    }

    @Override
    public void setSkew(float kx, float ky) {
        Log.d(TAG, "setSkew() called with: kx = [" + kx + "], ky = [" + ky + "]");
        super.setSkew(kx, ky);
    }

    @Override
    public boolean setConcat(Matrix a, Matrix b) {
        Log.d(TAG, "setConcat() called with: a = [" + a + "], b = [" + b + "]");
        return super.setConcat(a, b);
    }

    @Override
    public boolean preTranslate(float dx, float dy) {
        Log.d(TAG, "preTranslate() called with: dx = [" + dx + "], dy = [" + dy + "]");
        return super.preTranslate(dx, dy);
    }

    @Override
    public boolean preScale(float sx, float sy, float px, float py) {
        Log.d(TAG, "preScale() called with: sx = [" + sx + "], sy = [" + sy + "], px = [" + px + "], py = [" + py + "]");
        boolean b = super.preScale(sx, sy, px, py);
        scale = getValues()[0];
        return b;
    }

    @Override
    public boolean preScale(float sx, float sy) {
        Log.d(TAG, "preScale() called with: sx = [" + sx + "], sy = [" + sy + "]");
        boolean b = super.preScale(sx, sy);
        scale = getValues()[0];
        return b;
    }

    @Override
    public boolean preRotate(float degrees, float px, float py) {
        Log.d(TAG, "preRotate() called with: degrees = [" + degrees + "], px = [" + px + "], py = [" + py + "]");
        return super.preRotate(degrees, px, py);
    }

    @Override
    public boolean preRotate(float degrees) {
        Log.d(TAG, "preRotate() called with: degrees = [" + degrees + "]");
        return super.preRotate(degrees);
    }

    @Override
    public boolean preSkew(float kx, float ky, float px, float py) {
        Log.d(TAG, "preSkew() called with: kx = [" + kx + "], ky = [" + ky + "], px = [" + px + "], py = [" + py + "]");
        return super.preSkew(kx, ky, px, py);
    }

    @Override
    public boolean preSkew(float kx, float ky) {
        Log.d(TAG, "preSkew() called with: kx = [" + kx + "], ky = [" + ky + "]");
        return super.preSkew(kx, ky);
    }

    @Override
    public boolean preConcat(Matrix other) {
        Log.d(TAG, "preConcat() called with: other = [" + other + "]");
        return super.preConcat(other);
    }

    @Override
    public boolean postTranslate(float dx, float dy) {
        Log.d(TAG, "postTranslate() called with: dx = [" + dx + "], dy = [" + dy + "]");
        return super.postTranslate(dx, dy);
    }

    @Override
    public boolean postScale(float sx, float sy, float px, float py) {
        Log.d(TAG, "postScale() called with: sx = [" + sx + "], sy = [" + sy + "], px = [" + px + "], py = [" + py + "]");
        boolean b = super.postScale(sx, sy, px, py);
        scale = getValues()[0];
        return b;
    }

    @Override
    public boolean postScale(float sx, float sy) {
        Log.d(TAG, "postScale() called with: sx = [" + sx + "], sy = [" + sy + "]");
        boolean b = super.postScale(sx, sy);
        scale = getValues()[0];
        return b;
    }

    @Override
    public boolean postRotate(float degrees, float px, float py) {
        Log.d(TAG, "postRotate() called with: degrees = [" + degrees + "], px = [" + px + "], py = [" + py + "]");
        return super.postRotate(degrees, px, py);
    }

    @Override
    public boolean postRotate(float degrees) {
        Log.d(TAG, "postRotate() called with: degrees = [" + degrees + "]");
        return super.postRotate(degrees);
    }

    @Override
    public boolean postSkew(float kx, float ky, float px, float py) {
        Log.d(TAG, "postSkew() called with: kx = [" + kx + "], ky = [" + ky + "], px = [" + px + "], py = [" + py + "]");
        return super.postSkew(kx, ky, px, py);
    }

    @Override
    public boolean postSkew(float kx, float ky) {
        Log.d(TAG, "postSkew() called with: kx = [" + kx + "], ky = [" + ky + "]");
        return super.postSkew(kx, ky);
    }

    @Override
    public boolean postConcat(Matrix other) {
        Log.d(TAG, "postConcat() called with: other = [" + other + "]");
        return super.postConcat(other);
    }

    @Override
    public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
        Log.d(TAG, "setRectToRect() called with: src = [" + src + "], dst = [" + dst + "], stf = [" + stf + "]");
        return super.setRectToRect(src, dst, stf);
    }

    @Override
    public boolean setPolyToPoly(float[] src, int srcIndex, float[] dst, int dstIndex, int pointCount) {
        Log.d(TAG, "setPolyToPoly() called with: src = [" + src + "], srcIndex = [" + srcIndex + "], dst = [" + dst + "], dstIndex = [" + dstIndex + "], pointCount = [" + pointCount + "]");
        return super.setPolyToPoly(src, srcIndex, dst, dstIndex, pointCount);
    }

    @Override
    public boolean invert(Matrix inverse) {
        Log.d(TAG, "invert() called with: inverse = [" + inverse + "]");
        return super.invert(inverse);
    }

    @Override
    public void setValues(float[] values) {
        Log.d(TAG, "setValues() called with: values = [" + values + "]");
        super.setValues(values);
    }

    @Override
    public void set(Matrix src) {
        Log.d(TAG, "set() called with: src = [" + src + "]");
        super.set(src);
    }

    @Override
    public String toString() {
        return "" + this.hashCode() + "@" + super.toString();
    }

    private float values[] = new float[9];

    void checkScale() {
        getValues(values);
        if (scale == null) {
            scale = values[0];
        }
        Preconditions.checkState(values[0] == scale, "cached scale = " + scale + ", real scale = " + values[0]);
    }

    float[] getValues() {
        getValues(values);
        return values;
    }
}
