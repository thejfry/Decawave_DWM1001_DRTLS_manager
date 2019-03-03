/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ble.signal.SignalStrength;
import com.decawave.argomanager.util.Fixme;
import com.google.common.base.Objects;

import butterknife.BindColor;
import butterknife.ButterKnife;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Argo project.
 */

public class SignalStrengthView extends View {
    private static final ComponentLog log = new ComponentLog(SignalStrengthView.class).disable();
    private static final int BAR_COUNT = SignalStrength.values().length - 1;
    private static final float HEIGHT_TO_WIDTH_RATIO = 44f/ 33;
    private static final float BAR_FILL_RATIO = 9f / 12;
    private static final float MIN_BAR_HEIGHT_RATIO = 0.5f;
    // width height
    private int w;
    private int h;
    // computed sizes
    private float horizontalShift, verticalShift;
    private float[] barHeights = new float[BAR_COUNT];
    private float barWidth;
    private float segmentWidth;
    //
    @BindColor(R.color.signal_bar_active)
    int colorActive;
    @BindColor(R.color.signal_bar_active_obsolete)
    int colorActiveObsolete;
    @BindColor(R.color.signal_bar_inactive)
    int colorInactive;

    private Paint activePaint, activeObsoletePaint, inactivePaint;
    //
    private SignalStrength signalStrength = SignalStrength.MEDIUM;
    private boolean colorize;  // false == obsolete


    public SignalStrengthView(Context context) {
        super(context);
        construct();
    }

    public SignalStrengthView(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct();
    }

    public SignalStrengthView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        construct();
    }

    public SignalStrengthView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        construct();
    }

    public void setSignalStrength(SignalStrength signalStrength, boolean colorize) {
        SignalStrength oldStrength = this.signalStrength;
        this.signalStrength = signalStrength;
        boolean oldColorize = this.colorize;
        this.colorize = colorize;
        boolean change = !Objects.equal(oldStrength, signalStrength) || !Objects.equal(oldColorize, colorize);
        if (change) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(widthMeasureSpec);

        if (Constants.DEBUG) {
            log.d("onMeasure: " + "widthMeasureSpec.w = [" + w + ", " + asMsMode(widthMeasureSpec) +
                    "], heightMeasureSpec = [" + h + ", " + asMsMode(heightMeasureSpec) + "]");
        }
        //
        if (wMode == MeasureSpec.EXACTLY) {
            // we will derive the dimension from width
            if (hMode == MeasureSpec.EXACTLY) {
                // problem, give up
                log.w("giving up onMeasure, both are exact: width = " + w + ", height = " + h);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            // compute the corresponding height
            int measuredHeight = (int) (w * HEIGHT_TO_WIDTH_RATIO + 0.5);
            // check if the height matches
            if (hMode == MeasureSpec.AT_MOST) {
                // check that we fit in
                if (measuredHeight > h) {
                    // we need to recompute based on max height
                    setMeasuredDimensions(w, h, false);
                    return;
                }
            } // multi-else:
            setMeasuredDimension(w, measuredHeight);
        } else if (hMode == MeasureSpec.EXACTLY) {
            // compute the corresponding width
            int measuredWidth = (int) (h / HEIGHT_TO_WIDTH_RATIO + 0.5);
            // check if the width matches
            if (wMode == MeasureSpec.AT_MOST) {
                // check that we fit it
                if (measuredWidth > w) {
                    // we need to recompute based on max width
                    setMeasuredDimensions(w, h, true);
                    return;
                }
            } // multi-else:
            setMeasuredDimension(measuredWidth, h);
        } else {
            log.w("giving up onMeasure(), none of mode is EXACT");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    static String asMsMode(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case MeasureSpec.AT_MOST:
                return "AT_MOST";
            case MeasureSpec.EXACTLY:
                return "EXACTLY";
            case MeasureSpec.UNSPECIFIED:
                return "UNSPECIFIED";
            default:
                throw new Fixme("unsupported mode! " + mode);
        }
    }

    private void setMeasuredDimensions(int w, int h, boolean baseOnWidth) {
        if (baseOnWidth) {
            // base on width
            setMeasuredDimension(w, (int) (w * HEIGHT_TO_WIDTH_RATIO + 0.5));
        } else {
            // base on height
            setMeasuredDimension((int) (h / HEIGHT_TO_WIDTH_RATIO + 0.5), h);
        }
    }

    private void construct() {
        // resolve colors
        ButterKnife.bind(this);
        // paints
        activePaint = new Paint();
        activePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        activePaint.setColor(colorActive);
        inactivePaint = new Paint(activePaint);
        inactivePaint.setColor(colorInactive);
        activeObsoletePaint = new Paint(activePaint);
        activeObsoletePaint.setColor(colorActiveObsolete);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            // we need to recompute
            float reqH = w * HEIGHT_TO_WIDTH_RATIO;
            float reqW = h / HEIGHT_TO_WIDTH_RATIO;
            if (reqH <= h) {
                // we will take the full width (it is limiting)
                this.w = w;
                horizontalShift = 0;
                // and center vertically
                verticalShift = (h - reqH) / 2;
                this.h = (int) (reqH + 0.5);
            } else if (reqW <= w) {
                // we will take the full height
                this.h = h;
                verticalShift = 0;
                // and center horizontally
                horizontalShift = (w - reqW) / 2;
                this.w = (int) (reqW + 0.5);
            } else {
                throw new IllegalStateException();
            }
        }
        // compute the sizes
        float minBarHeight = this.h * MIN_BAR_HEIGHT_RATIO;
        float heightDelta = (this.h - minBarHeight) / (BAR_COUNT - 1);
        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] = minBarHeight + (heightDelta * i);
        }
        // how many pieces are there in one bar
        float baseWidth = BAR_COUNT - 1 + BAR_FILL_RATIO;
        segmentWidth = this.w / baseWidth;
        barWidth = segmentWidth * BAR_FILL_RATIO;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (signalStrength == null) {
            // simply skip
            return;
        }
        for (int i = 0; i < BAR_COUNT; i++) {
            float baseLeft = horizontalShift + i * segmentWidth;
            float baseBottom = verticalShift + this.h;
            Paint paint = i <= (signalStrength.ordinal() - 1) ? (colorize ? activePaint : activeObsoletePaint) : inactivePaint;
            canvas.drawRect(baseLeft,
                    baseBottom - barHeights[i],
                    baseLeft + barWidth,
                    baseBottom,
                    paint
                    );
        }
    }
}
