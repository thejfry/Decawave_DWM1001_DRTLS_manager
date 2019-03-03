/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.decawave.argomanager.R;

import butterknife.BindColor;
import butterknife.ButterKnife;
import eu.kryl.android.common.Animator;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */

public class SimpleProgressView extends View {
    private static final int ANIMATION_DURATION = 200;
    private static final int INDETERMINATE_PROGRESS_PERIOD = 1000;
    private int maxValue = 100;
    private int currValue = 30;
    private int w;
    private int h;
    private Paint barPaint;
    private Animator progressAnimator;
    private State state = State.DETERMINATE;
    private long startTime;

    //
    @BindColor(R.color.mtrl_primary)
    int barColor;

    private enum State {
        INDETERMINATE,
        DETERMINATE,
        INACTIVE
    }

    public SimpleProgressView(Context context) {
        super(context);
        construct();
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        construct();
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        construct();
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        construct();
    }

    private void construct() {
        ButterKnife.bind(this);
        //
        barPaint = new Paint();
        barPaint.setColor(barColor);
        //
        progressAnimator = new Animator(daApp);
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public void makeInactive() {
        if (state != State.INACTIVE) {
            progressAnimator.abortAnimation();
            state = State.INACTIVE;
            // initiate onDraw
            invalidate();
        }
    }

    public void makeIndeterminate() {
        if (state != State.INDETERMINATE) {
            progressAnimator.abortAnimation();
            startTime = SystemClock.uptimeMillis();
            state = State.INDETERMINATE;
            // initiate onDraw
            invalidate();
        }
    }

    /**
     * Switches the view to determinate state with the given amount displayed.
     */
    public void setCurrValue(int currValue) {
        if (state != State.DETERMINATE) {
            progressAnimator.abortAnimation();
        }
        this.state = State.DETERMINATE;
        // now set up the animation
        float startValue;
        if (progressAnimator.isFinished()) {
            startValue = this.currValue;
        } else {
            progressAnimator.computeValue();
            startValue = progressAnimator.getCurrValue();
            progressAnimator.abortAnimation();
        }
        progressAnimator.startAnimation(startValue, currValue - startValue, ANIMATION_DURATION);
        // adjust internal state
        this.currValue = currValue;
        // initiate onDraw
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.w = w;
        this.h = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (state) {
            case INDETERMINATE:
                drawIndeterminateBar(canvas);
                break;
            case DETERMINATE:
                drawDeterminateBar(canvas);
                break;
            case INACTIVE:
                // do nothing
                break;
        }
    }

    private void drawIndeterminateBar(Canvas canvas) {
        int timeDelta = (int) ((SystemClock.uptimeMillis() - startTime) % INDETERMINATE_PROGRESS_PERIOD);
        //
        float prc = 1f * timeDelta / INDETERMINATE_PROGRESS_PERIOD;
        float mid = prc * w;
        //
        float start, end;
        if (prc < 0.5) {
            float delta = w * prc * 0.3f;
            start = mid - delta;
            end = mid + delta;
        } else {
            float delta = w * (1 - prc) * 0.3f;
            start = mid - delta;
            end = mid + delta;
        }
        canvas.drawRect(start, 0, end, h, barPaint);
        // and draw again
        postInvalidate();
    }

    private void drawDeterminateBar(Canvas canvas) {
        // draw the progress
        progressAnimator.computeValue();
        float currValue = progressAnimator.getCurrValue();
        float ratio = 1f * currValue / maxValue;
        float xLength = w * ratio;
        canvas.drawRect(0, 0, xLength, h, barPaint);
        if (!progressAnimator.isFinished()) {
            // redraw again
            postInvalidate();
        }
    }
}
