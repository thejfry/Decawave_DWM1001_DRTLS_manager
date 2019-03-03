/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.common.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import eu.kryl.common.R;

/**
 * To start/stop the animation, set this View's VISIBILITY.
 * 
 * The COLOR and STROKE_WIDTH can be set as attributes from XML,
 * or through a call to {@link SbRotatingProgressView#setLook(android.content.Context, int, int)}.
 * 
 * Example XML entry:
 *      <SbRotatingProgressView
 *          xmlns:custom="http://schemas.android.com/apk/res-auto"
 *          android:layout_width="48dp"
 *          android:layout_height="48dp"
 *          custom:stroke_color="#ff008cbb"
 *          custom:stroke_width="4dp"
 *          />
 *      
 * For this to work, you need this in attrs.xml:
 *      <declare-styleable name="SbRotatingProgress">
 *          <attr name="stroke_color" format="reference|color" />
 *          <attr name="stroke_width" format="reference|dimension" />
 *      </declare-styleable>
 */
public class SbRotatingProgressView extends View {
    /** stylable PARAMS */
    private int color;
    private int strokeWidthPx;
    
    /** */
    private static final int DEF_COLOR = 0xff008cbb;
    private static final int DEF_STROKE_WIDTH_DP = 4;
    
    /** arc length growth speed */
    private static final int CYCLE_TIME = 650; // [ms]
    
    /** arc reference point movement speed */
    private static final float REF_POINT_ROTATION_TIME = 2000.0f; // [ms]
    
    /** */
    private static final int ARC_MIN_LEN_DEG = 20;
    private static final int ARC_MAX_LEN_DEG = 270;
    
    private enum ArcLengthState {
        GROWING,
        SHRINKING
    }
    
    private ArcLengthState arcLengthState;
    
    /** */
    private Paint paint;
    private RectF boundingBox;
    private int paddingPx;
    
    /**
     * The reference point of the arc to be drawn, progressing at a slow but constant angular velocity.
     * This point is either the arc's START, or its END point, flipped when {@link #arcLengthState} changes
     * from {@link ArcLengthState#GROWING} to {@link ArcLengthState#SHRINKING}, halfway through a single
     * animation cycle.
     */
    private float arcRefDeg;
    private long arcRefLastUsedTime;
    
    /** */
    private boolean isVisible;
    
    /** */
    private ArcLengthAnimator animator = new ArcLengthAnimator();
    
    /** */
    public SbRotatingProgressView(Context context) {
        this(context, null);
    }

    /** */
    public SbRotatingProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    /** */
    public SbRotatingProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }
    
    /** */
    private void init(Context ctx, AttributeSet attrs) {
        boundingBox = new RectF();
        paddingPx = dpToPx(ctx, 2);
        
        // default values for when there are no XML attrs
        color = DEF_COLOR;
        strokeWidthPx = dpToPx(ctx, DEF_STROKE_WIDTH_DP);
        if (attrs != null) {
            // SbRotatingProgress defined in values/attrs.xml
            TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.SbRotatingProgress);
            for (int idx=0; idx < ta.getIndexCount(); idx++) {
                int attr = ta.getIndex(idx);
                if (attr == R.styleable.SbRotatingProgress_stroke_color) {
                    color = ta.getColor(attr, DEF_COLOR);
                } else if (attr == R.styleable.SbRotatingProgress_stroke_width) {
                    strokeWidthPx = (int) (0.5f + ta.getDimension(attr, dpToPx(ctx, DEF_STROKE_WIDTH_DP)));
                }
            }
            ta.recycle();
        }
        setLook(ctx, color, strokeWidthPx);
        handleVisibility(getVisibility());
    }
    
    /** */
    public void setLook(Context ctx, int color, int strokeWidthPx) {
        this.color = color;
        this.strokeWidthPx = strokeWidthPx;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG| Paint.DITHER_FLAG);
        paint.setColor(color);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(strokeWidthPx);
    }

    /**
     * 
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        int minDimen = Math.min(w, h);
        float radius = (minDimen - 2*paddingPx - 2*strokeWidthPx) / 2.0f;
        float cx = w/2.0f;
        float cy = h/2.0f;
        
        boundingBox.left = cx - radius;
        boundingBox.right = cx + radius;
        boundingBox.top = cy - radius;
        boundingBox.bottom = cy + radius;
    }
    
    /**
     * 
     */
    @Override
    protected void onDraw(Canvas canvas) {

        if (!isVisible) return;
        
        long now = getDrawingTime();

        if (animator.isDone(now)) {
            // need to flip the reference point and restart the animation...
            switch (arcLengthState) {
                case GROWING:
                    arcLengthState = ArcLengthState.SHRINKING;
                    animator.start(ARC_MAX_LEN_DEG, ARC_MIN_LEN_DEG, CYCLE_TIME, now);
                    arcRefDeg += ARC_MAX_LEN_DEG;
                    arcRefLastUsedTime = now;
                    break;
                case SHRINKING:
                    arcLengthState = ArcLengthState.GROWING;
                    animator.start(ARC_MIN_LEN_DEG, ARC_MAX_LEN_DEG, CYCLE_TIME, now);
                    arcRefDeg -= ARC_MIN_LEN_DEG;
                    arcRefLastUsedTime = now;
                    break;
            }
        }
        
        // d = v*t
        arcRefDeg += (360/REF_POINT_ROTATION_TIME) * (now - arcRefLastUsedTime);
        float arcLenSweep = (float) animator.getArcSweepDeg(now);

        switch (arcLengthState) {
            case GROWING:
                canvas.drawArc(boundingBox, arcRefDeg, arcLenSweep, false, paint);
                break;
            case SHRINKING:
                canvas.drawArc(boundingBox, arcRefDeg-arcLenSweep, arcLenSweep, false, paint);
                break;
        }
        
        arcRefLastUsedTime = now;
        
        postInvalidate();
    }
    
    /**
     * 
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        handleVisibility(visibility);
    }

    private void handleVisibility(int visibility) {
        boolean wasVisible = isVisible;
        isVisible = (visibility == VISIBLE);
        if (!wasVisible && isVisible) {
            arcRefDeg = ARC_MIN_LEN_DEG;
            arcLengthState = ArcLengthState.SHRINKING;
            animator.reset();
            invalidate();
        }
    }
    
    /** */
    public static int dpToPx(Context ctx, int dp) {
        return (int) (0.5f + TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                    ctx.getResources().getDisplayMetrics()));
    }
    
    /**
     * 
     */
    private class ArcLengthAnimator {
        private float startValue, endValue;
        private long durationTime; // [ms]
        
        private long startTime; // [ms]
        private boolean isDone;
        
        /** */
        public void start(float startValue, float endValue, long durationTime, long now) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.durationTime = durationTime;
            startTime = now;
            isDone = false;
        }
        
        /** */
        public void reset() {
            this.startValue = 0;
            this.endValue = 1;
            this.durationTime = 1;
            startTime = 0;
            isDone = true;
        }
        
        /** */
        public boolean isDone(long now) {
            isDone = (now - startTime > durationTime);
            return isDone;
        }
        
        /**
         * @return the length of the arc in degrees
         */
        public double getArcSweepDeg(long now) {
            long elapsedTime = now - startTime;
            
            if (elapsedTime <= 0) {
                return startValue;
            } else if (elapsedTime > durationTime) {
                isDone = true;
                return endValue;
            } else {
                double change = (endValue - startValue) * getInterpolation(((float)elapsedTime)/durationTime, 3);
                return startValue + change;
            }
        }
        
        /**
         * An interpolator where the rate of change starts and ends slowly,
         * but accelerates through the middle.
         * 
         * Some fancy curves at: http://www.flong.com/texts/code/shapers_poly/
         * 
         * Android's AccelerateDecelerateInterpolator uses:
         *      return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
         * 
         * We use Symmetric Double-Polynomial Sigmoids instead. @param degree
         * is the polynom's degree, defining the sigmoid's steepness.
         */
        private double getInterpolation(float x, int degree) {
            if (degree%2 == 0) {
                return (x <= 0.5f) ?
                        Math.pow(2*x, degree) / 2 :
                            1 - Math.pow(2*(x-1), degree) / 2;
            } else {
                return (x <= 0.5f) ?
                        Math.pow(2*x, degree) / 2 :
                            1 + Math.pow(2*(x-1), degree) / 2;
            }
        }
    }
}
