/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.util.Fixme;
import com.decawave.argomanager.util.Util;

import butterknife.BindColor;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;

import static com.decawave.argomanager.ui.DisplayMetrics.LCD_DIP_SCALING_FACTOR;
import static com.decawave.argomanager.ui.view.GridView.DEG_TO_RAD;
import static com.decawave.argomanager.ui.view.SignalStrengthView.asMsMode;

/**
 * Argo project.
 */

public class NodeStateView extends View {
    private static final ComponentLog log = new ComponentLog(NodeStateView.class).disable();
    public static final float STROKE_WIDTH = LCD_DIP_SCALING_FACTOR * GridView.LINE_WIDTH_NODE;
    //
    public static final float TEXT_SIZE_DIP = 20;
    public static final float TEXT_SIZE_PX = LCD_DIP_SCALING_FACTOR * TEXT_SIZE_DIP;

    private static final int SEGMENT_COUNT = 8;
    private static final int VISIBLE_SEGMENT_COUNT = (int) (SEGMENT_COUNT * 0.7);
    private static final int SEGMENT_ANGLE = 360 / SEGMENT_COUNT;
    //
    public static final int ONE_ROUND_MS = 800;
    public static final int SEGMENT_MS = ONE_ROUND_MS / SEGMENT_COUNT;
    //
    private static final int TRANSITION_ANIMATION_DURATION = 700;

    // computed sizes
    private float horizontalShift, verticalShift;
    private float size;
    //
    @BindColor(R.color.anchor_color)
    int anchorTriangleColor;
    @BindColor(R.color.exclamation_mark_color)
    int exclamationMarkColor;
    @BindColor(R.color.anchor_color)
    int fixedColor;
    @BindColor(R.color.color_passive_node)
    int passiveColor;

    // this is the effective color, we will initialize the default here for the Android Studio preview to work
    private Paint cachedConnectedPaint, cachedTagPaint, cachedAnchorPaint, cachedExclPaint;
    // we will initialize the default here for the Android Studio preview to work
    private NetworkNode networkNode;
    private long startTime;
    private long transitionAnimationStart;
    private long transitionAnimationStop;
    private State previousState;
    private State state;
    private boolean passive;
    private Action0 onClickAction;


    public NodeStateView(Context context) {
        super(context);
        construct();
    }

    public NodeStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct();
    }

    public NodeStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        construct();
    }

    public NodeStateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        construct();
    }

    public void setNetworkNode(NetworkNode networkNode) {
        if (Constants.DEBUG) {
            log.d("setNetworkNode: " + "networkNode = [" + networkNode + "]");
        }
        this.networkNode = networkNode;
        // reset caches
        this.cachedTagPaint = null;
        this.cachedConnectedPaint = null;
    }

    public enum State {
        CONNECTED,
        ERROR,  // DISCONNECTED
        SHOW_NODE_TYPE,      // DISCONNECTED
        ANCHOR,
        TAG
    }

    public void setOnClickAction(Action0 onClickListener) {
        this.onClickAction = onClickListener;
        setClickable(onClickListener != null);
    }

    public void setState(State state, boolean animateStateChange) {
        if (Constants.DEBUG) {
            log.d("setState: " + "state = [" + state + "] , nodeBle = ["
                    + (networkNode != null ? networkNode.getBleAddress() : null) + "]");
        }
        if (this.state != state) {
            if (animateStateChange && this.state != null) {
                this.previousState = this.state;
                this.transitionAnimationStart = SystemClock.uptimeMillis();
                this.transitionAnimationStop = transitionAnimationStart + TRANSITION_ANIMATION_DURATION;
            } else {
                this.previousState = null;
            }
            this.state = state;
            if (state == State.CONNECTED) {
                this.startTime = SystemClock.uptimeMillis();
            }
            // invalidate the paint
            postInvalidate();
        }
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    private Paint getConnectedPaint() {
        if (cachedConnectedPaint == null) {
            cachedConnectedPaint = newConnectedNodePaint(networkNode.getBleAddress());
        }
        return cachedConnectedPaint;
    }

    private Paint getTagPaint() {
        if (cachedTagPaint == null) {
            cachedTagPaint = newTagPaint();
        }
        return cachedTagPaint;
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
        int size = Math.min(w, h);
        if (wMode == MeasureSpec.EXACTLY) {
            // we will derive the dimension from width
            if (hMode == MeasureSpec.EXACTLY) {
                // problem, give up
                log.w("giving up onMeasure, both are exact: width = " + w + ", height = " + h);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            // compute the corresponding height
            // check if the height matches
            if (hMode == MeasureSpec.AT_MOST) {
                // check that we fit in
                if (w > h) {
                    // we need to recompute based on max height
                    setMeasuredDimension(size, size);
                    return;
                }
            } // multi-else:
            setMeasuredDimension(w, w);
        } else if (hMode == MeasureSpec.EXACTLY) {
            // compute the corresponding width
            // check if the width matches
            if (wMode == MeasureSpec.AT_MOST) {
                // check that we fit it
                if (h > w) {
                    // we need to recompute based on max width
                    setMeasuredDimension(size, size);
                    return;
                }
            } // multi-else:
            setMeasuredDimension(h, h);
        } else {
            log.w("giving up onMeasure(), none of mode is EXACT");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void construct() {
        // resolve colors
        ButterKnife.bind(this);
    }

    private Paint getExclamationMarkPaint() {
        if (cachedExclPaint == null) {
            cachedExclPaint = new Paint();
            cachedExclPaint.setTextAlign(Paint.Align.CENTER);
            cachedExclPaint.setTextSize(TEXT_SIZE_PX);
            cachedExclPaint.setColor(exclamationMarkColor);
            cachedExclPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            cachedExclPaint.setAntiAlias(true);
            cachedExclPaint.setDither(true);
        }
        return cachedExclPaint;
    }

    private Paint getAnchorPaint() {
        if (cachedAnchorPaint == null) {
            cachedAnchorPaint = new Paint();
            cachedAnchorPaint.setStyle(Paint.Style.STROKE);
            cachedAnchorPaint.setStrokeWidth(STROKE_WIDTH);
            cachedAnchorPaint.setAntiAlias(true);
            cachedAnchorPaint.setStrokeJoin(Paint.Join.ROUND);
            cachedAnchorPaint.setStrokeCap(Paint.Cap.ROUND);
        }
        return cachedAnchorPaint;
    }

    private Paint newConnectedNodePaint(String bleAddress) {
        Paint connectedNodePaint = new Paint(getAnchorPaint());
        connectedNodePaint.setStyle(Paint.Style.STROKE);
        connectedNodePaint.setColor(Util.computeColorForAddress(bleAddress));
        return connectedNodePaint;
    }

    private Integer computedTagColor;

    private Paint newTagPaint() {
        Paint tagPaint = new Paint(getAnchorPaint());
        tagPaint.setStyle(Paint.Style.FILL);
        return tagPaint;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            // pretend that we have a smaller space (to let the line stroke fit in)
            w -= STROKE_WIDTH;
            h -= STROKE_WIDTH;
            // we need to recompute
            size = Math.min(w, h);
            if (size == w) {
                // we will take the full width (it is limiting)
                horizontalShift = 0;
                // and center vertically
                verticalShift = (h - size) / 2f;
            } else if (size == h) {
                // we will take the full height
                verticalShift = 0;
                // and center horizontally
                horizontalShift = (w - size) / 2f;
            } else {
                throw new IllegalStateException();
            }
            verticalShift += STROKE_WIDTH / 2;
            horizontalShift += STROKE_WIDTH / 2;
            if (Constants.DEBUG) {
                log.d("onSizeChanged() w = " + (w + STROKE_WIDTH) + ", h = " + (h + STROKE_WIDTH) + ", result: size = " + size + ", verticalShift = " + verticalShift + ", horizontalShift = " + horizontalShift);
            }
            networkElementPath = null;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (state == null || (state == State.SHOW_NODE_TYPE && networkNode == null)) {
            // simply skip
            return;
        }
        long now = 0;
        if (previousState != null) {
            now = SystemClock.uptimeMillis();
            if (now >= transitionAnimationStop) {
                previousState = null;
            }
        }
        if (previousState != null) {
            int alpha = (int) (255f * (now - transitionAnimationStart) / TRANSITION_ANIMATION_DURATION);
            drawState(canvas, previousState, 255 - alpha);
            drawState(canvas, state, alpha);
            // let us redraw the final state
            postInvalidate();
        } else {
            drawState(canvas, state, 255);
        }
    }

    private void drawState(Canvas canvas, State state, int alpha) {
        switch (state) {
            case CONNECTED:
                drawConnectedNode(canvas, alpha);
                break;
            case ERROR:
                drawErrorCross(canvas, alpha);
                break;
            case SHOW_NODE_TYPE:
                if (networkNode.getType() == NodeType.TAG) {
                    drawTagCircle(canvas, alpha, false);
                } else {
                    // draw anchor triangle
                    drawAnchorTriangle(canvas, alpha, false);
                }
                break;
            case ANCHOR:
                drawAnchorTriangle(canvas, alpha, true);
                break;
            case TAG:
                drawTagCircle(canvas, alpha, true);
                break;
            default:
                throw new Fixme("unexpected state: " + state);
        }
    }

    private void drawExclamationMark(Canvas canvas, int alpha) {
        Paint paint = getExclamationMarkPaint();
        paint.setAlpha(alpha);
        float textSize = paint.getTextSize();
        canvas.drawText("!", size / 2f + (TEXT_SIZE_PX / 20), textSize, paint);
    }

    private void drawConnectedNode(Canvas canvas, int alpha) {
        Paint paint = getConnectedPaint();
        float w = paint.getStrokeWidth();
        RectF rect = new RectF(0 + w, 0 + w, canvas.getWidth() - w,canvas.getHeight() - w);
        int ms = (int) ((SystemClock.uptimeMillis() - startTime) % ONE_ROUND_MS);
        float angle = SEGMENT_ANGLE * (ms / SEGMENT_MS);
        //
        int alphaDelta = alpha / VISIBLE_SEGMENT_COUNT;
        paint.setAlpha(alpha);
        for (int i = 0; i < VISIBLE_SEGMENT_COUNT; i++) {
            canvas.drawArc(rect, angle, SEGMENT_ANGLE, true, paint);
            // adjust for the next iteration
            angle -= SEGMENT_ANGLE;
            paint.setAlpha(paint.getAlpha() - alphaDelta);
        }
        postInvalidate();
    }

    private void drawTagCircle(Canvas canvas, int alpha, boolean useFixedColorDrawEmptyCircle) {
        float half = size / 2f;
        float radius = half / 2f;
        Paint paint = getTagPaint();
        paint.setAlpha(alpha);
        if (useFixedColorDrawEmptyCircle) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(fixedColor);
        } else {
            paint.setStyle(Paint.Style.FILL);
            if (computedTagColor == null) {
                computedTagColor = Util.computeColorForAddress(networkNode.getBleAddress());
            }
            paint.setColor(computedTagColor);
        }
        canvas.drawCircle(horizontalShift + half, verticalShift + half, radius, paint);
    }

    private Path networkElementPath;

    private void drawAnchorTriangle(Canvas canvas, int alpha, boolean useFixedColor) {
        if (networkElementPath == null) {
            // create the path
            networkElementPath = new Path();
            float half = size / 2f;
            float x = horizontalShift + half;
            float y = verticalShift + half / 2;
            configureAnchorPath(networkElementPath, x, y, half);
        }
        Paint paint = getAnchorPaint();
        // initiator is filled
        paint.setStyle(!useFixedColor && ((AnchorNode) networkNode).isInitiator() ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
        paint.setAlpha(alpha);
        paint.setColor(useFixedColor ? fixedColor : (passive ? passiveColor : anchorTriangleColor));
        canvas.drawPath(networkElementPath, paint);
    }

    private Path crossPath;

    private void drawErrorCross(Canvas canvas, int alpha) {
        if (crossPath == null) {
            // create the path
            crossPath = new Path();
            configureCrossPath(crossPath, horizontalShift + size / 5, verticalShift + size / 5, size * 3/5);
        }
        Paint paint = getAnchorPaint();
        // initiator is filled
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(alpha);
        paint.setColor(anchorTriangleColor);
        canvas.drawPath(crossPath, paint);
    }

    public static void configureAnchorPath(Path path, float tipX, float tipY, float triangleSize) {
        path.reset();
        // move to upper corner
        path.moveTo(tipX, tipY);
        float deltaY = (float) Math.sin(60 * DEG_TO_RAD) * triangleSize;
        float deltaX = 0.5f * triangleSize;
        path.lineTo(tipX + deltaX, tipY + deltaY);
        path.lineTo(tipX - deltaX, tipY + deltaY);
        path.lineTo(tipX, tipY);
    }

    public static void configureCrossPath(Path path, float upperLeftX, float upperLeftY, float size) {
        path.reset();
        // move to upper left corner
        path.moveTo(upperLeftX, upperLeftY);
        path.lineTo(upperLeftX + size, upperLeftY + size);
        path.moveTo(upperLeftX, upperLeftY + size);
        path.lineTo(upperLeftX + size, upperLeftY);
    }

    @OnClick
    public void onClick() {
        if (onClickAction != null) {
            onClickAction.call();
        }
    }

}
