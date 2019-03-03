/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.RangingAnchor;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.ui.DisplayMetrics;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import butterknife.BindColor;
import butterknife.ButterKnife;
import eu.kryl.android.common.Animator;
import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action1;

import static com.decawave.argomanager.ArgoApp.daApp;
import static com.decawave.argomanager.ArgoApp.uiHandler;
import static com.decawave.argomanager.ui.DisplayMetrics.LCD_DENSITY_SCALING_FACTOR;
import static com.decawave.argomanager.ui.DisplayMetrics.LCD_DIP_SCALING_FACTOR;

/**
 * Network view.
 *
 * TODO list:
 * - margins
 * - draw reusable graphics into bitmap and reuse
 */
public class GridView extends View {

    private static final boolean DEBUG = false;
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_DRAW_NODE = DEBUG && false;
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_MATRIX = DEBUG && false;

    private static final ComponentLog log = new ComponentLog(GridView.class);
    public static final int FLOORPLAN_MIN_PIXELS_IN_TEN_METERS = 200;
    public static final int FLOORPLAN_MAX_PIXELS_IN_TEN_METERS = 10000;

    static {
        if (!DEBUG) log.disable();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // overall configuration
    private static final int ZOOM_IN_ON_DOUBLE_TAP_DURATION = 500;
    private static final int ZOOM_IN_ANIMATION_DURATION = 2500;
    private static final int NORMALIZATION_DISTANCE_UNIT_FACTOR = 10;
    private static final int CLICK_TOLERANCE_DIP = 30;

    // 0.5 m is tolerable
    private static final float INCONSISTENT_DISTANCE_TOLERANCE = 500;

    private static final int GRID_LINES_MAX_LEVELS = 3;

    private static final int INITIAL_ZOOM_RATIO = 200;     // 1 cm on screen is 200 cm / 2m in reality

    private static final int MAX_ZOOM_RATIO = 20;          // 1cm on screen is 20cm in reality
    private static final int MIN_ZOOM_RATIO = 5000;        // 1cm on screen is 5000 cm (50m) in reality
    private static final float ZOOM_FACTOR_ON_DOUBLE_TAP = 2;
    //
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0f);
    private static final float INCH_TO_CM = 2.54f;
    public static final int CM_IN_METER = 100;
    public static final int CM_IN_KILOMETER = 100000;

    public static final float CM_IN_FEET = 30.48f;
    public static final float CM_IN_YARD = 91.44f;
    public static final int CM_IN_MILE = 160934;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // grid scaling configuration
    private static final int SHORTEST_GRID_SQUARE_MM = 5;
    private static final int SHOW_GRID_MARKS_FROM_SCREEN_DISTANCE_CM_MAX = 4;

    // line widths (relative)
    private static final float[] GRID_LINE_WIDTH = {
            0,        // hairline
            1,
            1
    };

    //
    private static final int[] GRID_LINE_STEP_CM = new int[] {
            10,     // 10cm
            20,
            50,
            100,    // 1m
            200,
            500,
            1000,   // 10m
            2000,
            5000,
            10000,  // 100m
            20000,
            50000,
            100000, // 1km
            200000,
            500000,
            1000000,// 10km
    };

    private static final int[] GRID_LINE_STEP_INCH = new int[] {
            5,          // 5 inches (15 cm)
            12,         // 1 foot - 12 inches
            36,         // 1 yard - 3 feet
            72,         // 2 yards
            180,        // 5 yards
            360,        // 10 yards
            720,        // 20 yards
            1800,       // 50 yards
            3600,       // 100 yards
            7200,
            18000,
            36000,      // 1000 yards
            63360,      // 1 mile (1760 yard)
            126720,     // 2 miles
            316800,     // 5 miles
    };

    // anchor triangle
    private static final int GROW_ANCHOR_TRIANGLE_FROM_ON_SCREEN_4CM_IS_X_M = 15;
    private static final int GROW_ANCHOR_TRIANGLE_TO_ON_SCREEN_4CM_IS_X_M = 5;

    private static final int RANGING_ANCHOR_SHOW_FROM_ON_SCREEN_4CM_IS_X_M = 40;
    private static final int RANGING_ANCHOR_SHOW_TO_ON_SCREEN_4CM_IS_X_M = 25;

    private static final int ANCHOR_TRIANGLE_MIN_ALPHA = 64;
    private static final int ANCHOR_TRIANGLE_START_FULL_COLOR_FROM_ON_SCREEN_4CM_IS_X_M = 40;
    private static final int ANCHOR_TRIANGLE_FULL_COLOR_FROM_ON_SCREEN_4CM_IS_X_M = 10;

    private static final int CONSIDER_NODES_OUT_OF_SCREEN_DIP = 30;

    private static final int GROW_TAG_MARK_FROM_ON_SCREEN_4CM_IS_X_M = 50;
    private static final int GROW_TAG_MARK_TO_ON_SCREEN_4CM_IS_X_M = 15;

    public static final int LINE_WIDTH_NODE = 2;
    private static final int RANGING_ANCHOR_LINE_WIDTH = 1;
    private static final int ANCHOR_LABEL_TEXT_SIZE = 12;
    private static final int NODE_SUBINFO_TEXT_SIZE = 10;
    private static final int RANGING_ANCHOR_DISTANCE_LABEL_TEXT_SIZE = 10;
    private static final int TAG_LABEL_TEXT_SIZE = 13;
    private static final int GRID_LABEL_TEXT_SIZE = 10;
    private static final int ANCHOR_MIN_TRIANGLE_SIZE = 2;
    private static final int ANCHOR_MAX_TRIANGLE_SIZE = 7;


    private static final int TAG_MIN_CIRCLE_RADIUS = 3;
    private static final int TAG_MAX_CIRCLE_RADIUS = 5;

    private static final int FADE_OUT_MISSING_TAGS_BEFORE = 7500;
    private static final int FADE_OUT_MISSING_TAGS_AFTER = 5000;
    private static final int FADE_OUT_MISSING_TAGS_DURATION = FADE_OUT_MISSING_TAGS_BEFORE - FADE_OUT_MISSING_TAGS_AFTER;

    // graphics
    private float pixelsPerCm;
    private float maxRangingAnchorLineScaleFactorToPx;
    private float minRangingAnchorLineScaleFactorToPx;
    private float maxAnchorSignGrowToScaleFactorToPx;
    private float minAnchorSignGrowFromScaleFactorToPx;
    private float minAnchorStartFullColorScaleFactorToPx;
    private float maxAnchorFullColorScaleFactorToPx;
    private float maxTagSignGrowToScaleFactorToPx;
    private float minTagSignGrowFromScaleFactorToPx;
    private float clickTolerance;
    private int considerNodesOutOfScreenPx;
    private boolean visibleNodesIndexingScheduled;
    private FloorPlan floorPlan;
    private boolean floorPlanBound = true;
    private Action1<FloorPlan> floorPlanChangedCallback;
    private long firstShowSysTime;


    private class GridLine {
        int idx;
        int stepInNaturalUnits;     // this is either cm or in
        float stepInPx;

        @Override
        public String toString() {
            return "GridLine{" +
                    "idx=" + idx +
                    ", stepInNaturalUnits=" + stepInNaturalUnits +
                    ", stepInPx=" + stepInPx +
                    '}';
        }
    }

    // dependencies
    private Function<String, Boolean> anchorPresenceResolver;
    private Function<String, Boolean> tagPresenceResolver;
    private LengthUnit lengthUnit;
    // gesture detectors
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private Action1<NetworkNode> nodeClickListener;

    //
    private Function<Short, NetworkNode> networkNodeByShortIdResolver;
    private Function<Long, TrackMode> trackModeResolver;
    private Supplier<FloorPlan> floorPlanProvider;
    private Supplier<Boolean> showDebugInfoSupplier;
    private Supplier<Boolean> showGridSupplier;
    private Supplier<Boolean> showAverageSupplier;
    private Supplier<Boolean> highlightIconsistentRangingDistances;

    // grid
    private Paint[] gridPaint = new Paint[GRID_LINES_MAX_LEVELS];
    private ArrayList<GridLine> shownGridLines = new ArrayList<>(GRID_LINES_MAX_LEVELS);
    private Paint gridLabelPaint;
    // GRID_LINE_STEP_CM transformed to scale factor
    private float[] gridLinesMinimalScaleFactor;

    // anchor + tag
    private Paint anchorStrokePaint;
    private Paint anchorFillPaint;
    private Paint anchorLabelPaint;
    private Paint nodeSubInfoPaint;
    private Paint rangingAnchorLabelPaint;
    private int anchorPresentColor;
    private int anchorMissingColor;
    private int anchorLabelAlpha;
    private int rangingAnchorLineAlpha;
    private int anchorSignAlpha; // 0 - 255
    private float anchorTriangleSize;
    private LoadingCache<String, Integer> tagColorCache;

    private Paint tagPaint;
    private Paint tagLabelPaint;
    private Paint rangingAnchorLinePaint;
    private int tagLabelAlpha;
    private float tagCircleRadius;
    //
    @BindColor(R.color.tag_ranging_anchor_ok_color)
    int rangingAnchorOkColor;
    @BindColor(R.color.tag_ranging_anchor_fail_color)
    int rangingAnchorFailColor;
    @BindColor(R.color.color_passive_node)
    int passiveColor;

    // floorplan
    private Matrix baseFloorPlanMatrix = new Matrix();
    private Matrix drawFloorPlanMatrix = new Matrix();
    private Matrix drawFloorPlanVirtualMatrix = new Matrix();

    // matrix
    private Matrix drawMatrix;
    private Matrix flingStartMatrix = new Matrix();
    private Matrix zoomStartMatrix = new Matrix();
    private PointF zoomFocalPoint = new PointF();

    // restore state
    private PointF injectedFocalPoint = null;
    private Float injectedScale;
    private Float extraAnimatedZoom = 1f;

    // scaling, offsets, fling, scrolling
    private int pxWidth;
    private int pxHeight;
    private float scaleFactorCmToPx;
    private float maxScaleFactorCmToPx;
    private float minScaleFactorCmToPx;
    // minimal step in pixel for which we start drawing marks
    private float minStepInPxGridMark;
    private float xPxOffset;
    private float yPxOffset;
    private Scroller mScroller;
    protected Animator mZoomer;
    protected Animator mFpRotator;

    private ArrayList<NodeAndPosition> nodesByXCoordinate = new ArrayList<>();
    private List<NodeAndPosition> visibleNodes = new ArrayList<>();
    private SortedSet<NetworkNode> nodesByZAxis;
    private Map<Long, NetworkNode> nodesById;
    private Map<Long, TagAvg> avgNodesById;

    private class TagAvg {
        int idx;
        boolean ready = false;
        float x_avg;
        float y_avg;
        float z_avg;
        float x[]  = new float[10];
        float y[]  = new float[10];
        float z[]  = new float[10];
        Position p = new Position();

        float average(float a[]) {
            float a_avg = 0;
            float div;

            if(ready) {
                div = 10 ;
            } else {
                div = idx;
            }

            if(div == 0) return 0;

            for(int i=0; i<div; i++) {
                a_avg += a[i];
            }
            a_avg /= div;

            return a_avg;
        }

        Position averagep() {
            if (!ready && idx == 0) {
                // there is no value to make average from yet
                if (DEBUG) log.d("averagep() returning null");
                return null;
            }
            p.x = (int) (float) x_avg;
            p.y = (int) (float) y_avg;
            p.z = (int) (float) z_avg;
            if (DEBUG) log.d("averagep() returning p: " + p);
            return p;
        }

        void updatexyz(Position position){
            x[idx] = position.x;
            y[idx] = position.y;
            z[idx] = position.z;

            idx++;
            if(idx >= 10) {
                ready = true;
                idx = 0;
            }

            //calculate averages
            x_avg = average(x);
            y_avg = average(y);
            z_avg = average(z);
        }
    }

    public GridView(Context context) {
        super(context);
        construct(context);
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct(context);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        construct(context);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        construct(context);
    }

    public void setDependencies(List<NetworkNode> initialNodeSet,
                                Function<Short, NetworkNode> networkNodeByShortIdResolver,
                                Function<Long, TrackMode> trackModeResolver,
                                Supplier<FloorPlan> floorPlanProvider,
                                Supplier<Boolean> showDebugInfoSupplier,
                                Supplier<Boolean> showGridSupplier,
                                Supplier<Boolean> showAverageSupplier,
                                Supplier<Boolean> highlightInconsistentRangingDistances,
                                Function<String, Boolean> anchorPresenceResolver,
                                Function<String, Boolean> tagPresenceResolver,
                                Action1<FloorPlan> floorPlanChangedCallback,
                                LengthUnit lengthUnit) {
        this.networkNodeByShortIdResolver = networkNodeByShortIdResolver;
        this.trackModeResolver = trackModeResolver;
        this.anchorPresenceResolver = anchorPresenceResolver;
        this.tagPresenceResolver = tagPresenceResolver;
        this.lengthUnit = lengthUnit;
        this.floorPlanProvider = floorPlanProvider;
        this.showDebugInfoSupplier = showDebugInfoSupplier;
        this.showGridSupplier = showGridSupplier;
        this.showAverageSupplier = showAverageSupplier;
        this.highlightIconsistentRangingDistances = highlightInconsistentRangingDistances;
        this.floorPlanChangedCallback = floorPlanChangedCallback;
        // initialize node set
        initNodeSet(initialNodeSet);
    }

    public void setFloorPlanBound(boolean lock) {
        floorPlanBound = lock;
        if (lock) {
            // if we are binding floor plan again, we need to recompute base floorplan matrix
            refreshFloorplanMatrixAndCaches();
        } else {
            // create a copy of floorplan draw matrix - this will be used for virtual scale
            // and propagated to drawFloorPlanMatrix if appropriate
            fpMatrixToVirtualFpMatrix();
        }
    }

    public void fpMatrixToVirtualFpMatrix() {
        drawFloorPlanVirtualMatrix.set(drawFloorPlanMatrix);
    }

    private void initNodeSet(List<NetworkNode> initialNodeSet) {
        nodesById = new HashMap<>();
        avgNodesById = new HashMap<>();
        // nodes sorted by z-axis
        nodesByZAxis = new TreeSet<>((o1, o2) -> {
            // anchors are always first
            if (o1.getType() != o2.getType()) {
                return o1.isAnchor() ? -1 : 1;
            } // compare same kind by z-axis
            //noinspection ConstantConditions
            int zDiff = o1.extractPositionDirect().z - o2.extractPositionDirect().z;
            if (zDiff != 0) {
                return zDiff;
            } // else: by id (we need total ordering)
            long lDiff = o1.getId() - o2.getId();
            return lDiff > 0 ? 1 : (lDiff == 0 ? 0 : -1);
        });
        Stream.of(initialNodeSet).forEach(networkNode -> {
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(networkNode.extractPositionDirect());
            }
            // create a copy of the node
            networkNode = NodeFactory.newNodeCopy(networkNode);
            // now organize
            nodesByZAxis.add(networkNode);
            nodesById.put(networkNode.getId(), networkNode);
            // initial fill with position
            if (networkNode.isTag()) {
                TagAvg tagAvg = new TagAvg();
                tagAvg.updatexyz(networkNode.extractPositionDirect());
                avgNodesById.put(networkNode.getId(), tagAvg);
            }
        });
        // clear helper datastructures
        nodesByXCoordinate.clear();
        visibleNodes.clear();
    }

    private void construct(Context ctx) {
        if (DEBUG) log.d("construct()");
        mZoomer = new Animator(ctx, new DecelerateInterpolator());
        mFpRotator = new Animator(ctx, new AccelerateDecelerateInterpolator());
        mGestureDetector = new GestureDetector(ctx, new GestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(ctx, new OnScaleGestureListener());
        mScroller = new Scroller(ctx);

        // configure grid paints
        //noinspection ConstantConditions
        Preconditions.checkState(GRID_LINES_MAX_LEVELS == 3, "FIXME: configure grid paint here!");
        Preconditions.checkState(gridPaint.length == GRID_LINES_MAX_LEVELS);
        configureGridPaint(ctx, 0, R.color.grid_line_color_primary);
        configureGridPaint(ctx, 1, R.color.grid_line_color_secondary);
        configureGridPaint(ctx, 2, R.color.grid_line_color_tertiary);

        clickTolerance = CLICK_TOLERANCE_DIP * LCD_DENSITY_SCALING_FACTOR;
        considerNodesOutOfScreenPx = (int) (CONSIDER_NODES_OUT_OF_SCREEN_DIP * LCD_DENSITY_SCALING_FACTOR);
        //
        anchorStrokePaint = new Paint();
        anchorStrokePaint.setStyle(Paint.Style.STROKE);
        anchorStrokePaint.setStrokeWidth(LCD_DIP_SCALING_FACTOR * LINE_WIDTH_NODE);
        anchorStrokePaint.setAntiAlias(true);
        anchorStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        anchorStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        anchorPresentColor = ContextCompat.getColor(ctx, R.color.anchor_color);
        anchorMissingColor = ContextCompat.getColor(ctx, R.color.anchor_missing_color);

        anchorFillPaint = new Paint(anchorStrokePaint);
        anchorFillPaint.setStyle(Paint.Style.FILL);
        anchorFillPaint.setColor(ContextCompat.getColor(ctx, R.color.anchor_fill_color));
        //
        tagPaint = new Paint(anchorStrokePaint);
        tagPaint.setStyle(Paint.Style.FILL);
        //
        rangingAnchorLinePaint = new Paint(tagPaint);
        rangingAnchorLinePaint.setStyle(Paint.Style.STROKE);
        rangingAnchorLinePaint.setStrokeWidth(LCD_DIP_SCALING_FACTOR * RANGING_ANCHOR_LINE_WIDTH);
        rangingAnchorLinePaint.setTextSize(NODE_SUBINFO_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);
        rangingAnchorLinePaint.setTextAlign(Paint.Align.CENTER);

        // resolve colors
        ButterKnife.bind(this);
        //
        anchorLabelPaint = new Paint();
        anchorLabelPaint.setTextSize(ANCHOR_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);
        anchorLabelPaint.setColor(ContextCompat.getColor(ctx, R.color.anchor_label_color));
        anchorLabelPaint.setTextAlign(Paint.Align.CENTER);
        anchorLabelPaint.setAntiAlias(true);

        nodeSubInfoPaint = new Paint(anchorLabelPaint);
        nodeSubInfoPaint.setTextSize(NODE_SUBINFO_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);

        rangingAnchorLabelPaint = new Paint(nodeSubInfoPaint);
        rangingAnchorLabelPaint.setTextSize(RANGING_ANCHOR_DISTANCE_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);

        //
        tagLabelPaint = new Paint(anchorLabelPaint);
        tagLabelPaint.setColor(ContextCompat.getColor(ctx, R.color.tag_label_color));
        tagLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        tagLabelPaint.setTextSize(TAG_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);

        gridLabelPaint = new Paint(tagLabelPaint);
        gridLabelPaint.setColor(ContextCompat.getColor(ctx, R.color.grid_label_color));
        gridLabelPaint.setTypeface(Typeface.DEFAULT);
        gridLabelPaint.setTextSize(GRID_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR);

        tagColorCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Integer>() {
            @Override
            public Integer load(@NonNull String key) throws Exception {
                return Util.computeColorForAddress(key);
            }
        });

        computeLimits();
    }

    private void configureGridPaint(Context ctx, int idx, int colorResId) {
        gridPaint[idx] = new Paint();
        gridPaint[idx].setColor(ContextCompat.getColor(ctx, colorResId));
        gridPaint[idx].setStrokeWidth(LCD_DIP_SCALING_FACTOR * GRID_LINE_WIDTH[idx]);
    }


    float[] fIn = new float[2];
    float[] fOut = new float[2];

    @Override
    protected void onDraw(Canvas canvas) {
        boolean redraw = computeAnimations();
        //
        if (floorPlan != null) {
            drawFloorPlan(canvas);
        }
        // draw the grid
        if (showGridSupplier.get()) drawGrid(canvas);
        // draw the nodes, check if they are sorted
        drawNodes(canvas);
        if (firstShowSysTime + FADE_OUT_MISSING_TAGS_BEFORE > SystemClock.uptimeMillis()) {
            redraw = true;
        }
        if (redraw) {
            postInvalidate();
        }
    }

    Matrix floorPlanWrkMatrix = new Matrix();

    private void drawFloorPlan(Canvas canvas) {
        floorPlanWrkMatrix.reset();
        if (!mFpRotator.isFinished()) {
            // there is ongoing rotate animation
            floorPlanWrkMatrix.setRotate(mFpRotator.getCurrValue(), floorPlan.getBitmapWidth() / 2, floorPlan.getBitmapHeight() / 2);
        } else if (floorPlan.rotation != 0) {
            floorPlanWrkMatrix.setRotate(floorPlan.rotation, floorPlan.getBitmapWidth() / 2, floorPlan.getBitmapHeight() / 2);
        }
        floorPlanWrkMatrix.postConcat(drawFloorPlanMatrix);
        canvas.drawBitmap(floorPlan.getBitmap(), floorPlanWrkMatrix, null);
    }

    private boolean computeAnimations() {
        boolean redraw = false;
        if (!mScroller.isFinished()) {
            // compute the next scroll offset
            mScroller.computeScrollOffset();
            // set proper drawmatrix
            drawMatrix.set(flingStartMatrix);
            // get the offset from scroller
            drawMatrix.postTranslate(mScroller.getCurrX(), mScroller.getCurrY());
            onDrawMatrixTranslationChanged();
            redraw = true;
        }
        if (!mZoomer.isFinished()) {
            drawMatrix.set(zoomStartMatrix);
            mZoomer.computeValue();
            float currValue = mZoomer.getCurrValue();
            if (DEBUG) {
                log.d("AUTO-ZOOM: currValue = " + currValue);
            }
            drawMatrix.postScale(currValue, currValue, zoomFocalPoint.x, zoomFocalPoint.y);
            onDrawMatrixChanged();
            redraw = true;
        }
        if (!mFpRotator.isFinished()) {
            mFpRotator.computeValue();
            redraw = true;
        }
        return redraw;
    }

    private void drawNodes(Canvas canvas) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(nodesByZAxis, "must set dependencies first!");
        }
        // draw them
        visibleNodes.clear();
        // draw ranging anchors first (if they are visible at the current zoom level)
        if (rangingAnchorLineAlpha > 0) {
            for (NetworkNode node : nodesByZAxis) {
                if (node.getType() == NodeType.TAG && tagPresenceResolver.apply(node.getBleAddress())) {
                    if (trackModeResolver.apply(node.getId()) == TrackMode.TRACKED_POSITION_AND_RANGING) {
                        // we are allowed to draw ranging anchors
                        TagNode tagNode = (TagNode) node;
                        if (tagNode.anyRangingAnchorInLocationData()) {
                            drawTagRangingAnchors(canvas, tagNode, rangingAnchorLineAlpha, anchorLabelAlpha);
                        }
                    }
                }
            }
        }
        // now draw nodes
        int missingTagAlpha = 0;
        long now = SystemClock.uptimeMillis();
        if (firstShowSysTime + FADE_OUT_MISSING_TAGS_BEFORE > now) {
            // less than 10s
            long sevenSecsAfterFirstShow = firstShowSysTime + FADE_OUT_MISSING_TAGS_AFTER;
            if (sevenSecsAfterFirstShow > now) {
                // less than 7s
                missingTagAlpha = 255;
            } else {
                // more than 7s
                missingTagAlpha = 255 - (int) ((now - (sevenSecsAfterFirstShow)) * 1f / FADE_OUT_MISSING_TAGS_DURATION * 255);
            }
        }
        int tagAlpha = 0;
        for (NetworkNode node : nodesByZAxis) {
            if (node.isTag()) {
                if (tagPresenceResolver.apply(node.getBleAddress())) {
                    tagAlpha = 255;
                } else {
                    if (missingTagAlpha == 0) {
                        // skip this one
                        continue;
                    }
                    tagAlpha = missingTagAlpha;
                }
            }
            drawNode(canvas, node, visibleNodes, tagAlpha);
        }
        // sorted nodes - scheduling
        if (!visibleNodesIndexingScheduled) {
            if (DEBUG) log.d("scheduling visible nodes indexing");
            visibleNodesIndexingScheduled = true;
            uiHandler.postDelayed(doIndexVisibleNodes, 300);
        }
    }

    private final Runnable doIndexVisibleNodes = () -> {
        visibleNodesIndexingScheduled = false;
        if (DEBUG) log.d("indexVisibleNodes(): count = " + visibleNodes.size());
        nodesByXCoordinate.clear();
        nodesByXCoordinate.addAll(visibleNodes);
        // sort
        Collections.sort(nodesByXCoordinate, (n1, n2) -> (int) Math.signum(n1.x - n2.x));
    };


    private enum DrawLabels {
        NO, YES, EVERY_OTHER
    }

    private void drawGrid(Canvas canvas) {
        int i = 0;
        for (GridLine gridLine : shownGridLines) {
            // determine whether we should draw labels
            DrawLabels drawLabels = DrawLabels.NO;
            // check if it is time to draw labels
            if (gridLine.stepInPx >= minStepInPxGridMark) {
                drawLabels = DrawLabels.YES;
            } else if (gridLine.stepInPx * 2 >= minStepInPxGridMark){
                // check the next gridline, if the marks are close to each other enough
                drawLabels = DrawLabels.EVERY_OTHER;
            }
            drawSingleGrid(canvas,
                    gridPaint[i++],
                    gridLine.stepInPx,
                    gridLine.stepInNaturalUnits,
                    drawLabels);
            if (drawLabels != DrawLabels.NO) {
                // once we have drawn the labels, we will not draw any other grid
                break;
            }
        }
    }

    private void drawSingleGrid(Canvas canvas, Paint paint, float gridLineStepInPx, int gridLineStepInNaturalUnits, DrawLabels drawLabels) {
        // how far we must go to not miss a single line
        int maxHorizontalLineIdx = Math.round(yPxOffset / gridLineStepInPx);
        int minVerticalLineIdx = -Math.round(xPxOffset / gridLineStepInPx);
        float gridLineStepInCm = gridLineStepInNaturalUnits;
        if (lengthUnit == LengthUnit.IMPERIAL) {
            gridLineStepInCm *= Util.CM_IN_INCH;
        }

        int realStartX = Math.round(minVerticalLineIdx * gridLineStepInCm);
        int realX = realStartX;
        int realY = Math.round(maxHorizontalLineIdx * gridLineStepInCm);

        fIn[0] = realX;
        fIn[1] = realY;
        // transform the x,y coordinates
        drawMatrix.mapPoints(fOut, fIn);
        float pxPosX = fOut[0];
        float pxPosY = fOut[1];

        // draw horizontal lines
        int i = 0;
        float y;
        do {
            y = pxPosY + (i++ * gridLineStepInPx);
            canvas.drawLine(0, y, pxWidth, y, paint);
        } while (y <= pxHeight);

        // draw vertical lines
        i = 0;
        float x;
        do {
            x = pxPosX + (i++ * gridLineStepInPx);
            canvas.drawLine(x, 0, x, pxHeight, paint);
        } while (x <= pxWidth);

        float labelStepInPx = gridLineStepInPx;
        float labelRealStep = gridLineStepInCm;

        if (drawLabels != DrawLabels.NO) {
            // draw labels
            pxPosY = fOut[1];
            float pxStartX = fOut[0];
            if (drawLabels == DrawLabels.EVERY_OTHER) {
                labelRealStep *= 2;
                labelStepInPx *= 2;
                int xFitsIn = Math.round(realStartX / gridLineStepInCm);
                if (xFitsIn % 2 != 0) {
                    realStartX += gridLineStepInCm;
                    pxStartX += gridLineStepInPx;
                }
                int yFitsIn = Math.round(realY / gridLineStepInCm);
                if (yFitsIn % 2 != 0) {
                    realY -= gridLineStepInCm;
                    pxPosY += gridLineStepInPx;
                }
            }
            do {
                pxPosX = pxStartX;
                realX = realStartX;
                do {
                    drawGridLabel(canvas, realX, realY, pxPosX, pxPosY);
                    pxPosX += labelStepInPx;
                    realX += labelRealStep;
                } while (pxPosX <= pxWidth);
                pxPosY += labelStepInPx;
                realY -= labelRealStep;
            } while (pxPosY <= pxHeight);
        }
    }

    private void drawGridLabel(Canvas canvas, int realPosX, int realPosY, float pxPosX, float pxPosY) {
        gridLabelPaint.setTextAlign(Paint.Align.RIGHT);
        //
        String x = getHumanReadableDistance(realPosX);
        String y = getHumanReadableDistance(realPosY);
        // draw label
        canvas.drawText(x, pxPosX - 0.4f * GRID_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, pxPosY - 0.5f * GRID_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, gridLabelPaint);
        gridLabelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(y, pxPosX + 0.4f * GRID_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, pxPosY + 1.2f * GRID_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, gridLabelPaint);
    }

    private String getHumanReadableDistance(int realDistance) {
        if (lengthUnit == LengthUnit.METRIC) {
            return getHumanReadableMetricDistance(realDistance);
        } else {
            return getHumanReadableImperialDistance(realDistance);
        }
    }

    private String getHumanReadableImperialDistance(int realDistance) {
        if (realDistance == 0) {
            return "0 " + daApp.getString(R.string.unit_yd);
        }
        // more 'analysis' needed
        boolean notLessThanYard = Math.abs(realDistance) >= CM_IN_YARD;
        boolean lessThanMile = Math.abs(realDistance) < CM_IN_MILE;
        if (notLessThanYard && lessThanMile) {
            // distance between 1yd and 1mi
             return String.valueOf(Math.round(realDistance / CM_IN_YARD)) + " " + daApp.getString(R.string.unit_yd);
        } else if (!notLessThanYard) {
            // less then 1yd
            return String.valueOf(Math.round(realDistance / CM_IN_FEET)) + " " + daApp.getString(R.string.unit_ft);
        } else {
            // more than a mile
            return String.valueOf(Math.round(realDistance / CM_IN_MILE)) + " " + daApp.getString(R.string.unit_mi);
        }
    }

    @NonNull
    private String getHumanReadableMetricDistance(int realDistance) {
        if (realDistance == 0) {
            return "0 " + daApp.getString(R.string.unit_m);
        }
        // more 'analysis' needed
        boolean notLessThanMeter = Math.abs(realDistance) >= CM_IN_METER;
        boolean lessThanKilometer = Math.abs(realDistance) < CM_IN_KILOMETER;
        if (notLessThanMeter && lessThanKilometer) {
            // distance between 1m and 1km
            return String.valueOf(realDistance / CM_IN_METER) + " " + daApp.getString(R.string.unit_m);
        } else if (!notLessThanMeter) {
            // less then one meter
            return realDistance + " " + daApp.getString(R.string.unit_cm);
        } else {
            // more than kilometer
            return String.valueOf(realDistance / CM_IN_KILOMETER) + " " + daApp.getString(R.string.unit_km);
        }
    }

    private void drawNode(Canvas canvas, NetworkNode node, List<NodeAndPosition> visibleNodes, int tagAlpha) {
        Position p;
        if((node.getType() == NodeType.TAG) && showAverageSupplier.get()){
            p = avgNodesById.get(node.getId()).averagep();
        } else {
            p = node.extractPositionDirect();
        }
        if (p == null) {
            // skip this one, there is position value
            return;
        }
        mapPositionToFout(p);

        float x = fOut[0];
        float y = fOut[1];
        // check if we are on-screen
        float closeToScreenEdgeFactor = closeToScreenEdgeFactor(x, y);
        if (closeToScreenEdgeFactor == 0) {
            // skip this one
            return;
        } // else:
        visibleNodes.add(new NodeAndPosition(node, x, y));
        switch (node.getType()) {
            case ANCHOR:
                drawAnchor(canvas, x, y, closeToScreenEdgeFactor, (AnchorNode) node);
                break;
            case TAG:
                drawTag(canvas, x, y, closeToScreenEdgeFactor, (TagNode) node, tagAlpha);
                break;
            default:
                throw new IllegalStateException("unsupported node type: " + node.getType());
        }
    }

    private float closeToScreenEdgeFactor(float x, float y) {
        if (x >= 0 && y >= 0 && x <= pxWidth && y <= pxHeight) {
            return 1;
        } else {
            // compute distance from the edge
            int distanceX = getDistance(x, pxWidth);
            int distanceY = getDistance(y, pxHeight);
            if (DEBUG) {
                log.d("distanceX = " + distanceX);
                log.d("distanceY = " + distanceY);
            }
            //
            if (distanceX > considerNodesOutOfScreenPx || distanceY > considerNodesOutOfScreenPx) {
                // we are completely out of screen
                return 0;
            } // else:
            float distanceFactorX = 1 - 1f * distanceX / considerNodesOutOfScreenPx;
            float distanceFactorY = 1 - 1f * distanceY / considerNodesOutOfScreenPx;
            return Math.min(distanceFactorX, distanceFactorY);
        }
    }

    private int getDistance(float coord, int upperLimit) {
        int distance;
        if (coord < 0) {
            distance = Math.round(-coord);
        } else if (coord > upperLimit) {
            distance = Math.round(coord - upperLimit);
        } else {
            // we are there
            distance = 0;
        }
        return distance;
    }

    private class NodeAndPosition {
        final float x, y;
        final NetworkNode node;

        NodeAndPosition(NetworkNode node, float x, float y) {
            this.x = x;
            this.y = y;
            this.node = node;
        }

        @Override
        public String toString() {
            return "NodeAndPosition{" +
                    "x=" + x +
                    ", y=" + y +
                    ", node=" + Util.shortenNodeId(node.getId(), false) +
                    '}';
        }
    }

    private Path networkElementPath = new Path();

    private Map<String, String> cachedSeatNumberClusterMap = new HashMap<>();
    private Map<String, String> cachedAnchorList = new HashMap<>();

    private void drawAnchor(Canvas canvas, float x, float y, float closeToScreenEdgeFactor, AnchorNode anchor) {
        String bleAddress = anchor.getBleAddress();
        if (DEBUG_DRAW_NODE) {
            log.d("drawing anchor: " + bleAddress + ", position: " + anchor.extractPositionDirect());
        }
        // anchor is a triangle
        NodeStateView.configureAnchorPath(networkElementPath, x, y - anchorTriangleSize / 2, anchorTriangleSize);
        if (!anchor.isInitiator()) {
            // if this is not initiator we will fill the triangle separately
            canvas.drawPath(networkElementPath, anchorFillPaint);
        }
        int alpha = Math.min((int) (closeToScreenEdgeFactor * 255), anchorSignAlpha);
        boolean isPresent = anchorPresenceResolver.apply(bleAddress);
        anchorStrokePaint.setColor(isPresent ? anchorPresentColor : anchorMissingColor);
        anchorStrokePaint.setAlpha(alpha);
        // if this is initiator we will draw filled triangle
        anchorStrokePaint.setStyle(anchor.isInitiator() ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
        canvas.drawPath(networkElementPath, anchorStrokePaint);
        // draw label
        if (anchorLabelAlpha > 0) {
            alpha = Math.min((int) (closeToScreenEdgeFactor * 255), anchorLabelAlpha);
            anchorLabelPaint.setAlpha(alpha);
            canvas.drawText(anchor.getLabel(), x,
                    y - (0.5f + .3f * alpha / 255) * ANCHOR_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, anchorLabelPaint);
            if (showDebugInfoSupplier.get() && isPresent && anchor.getSeatNumber() != null) {
                // seat number
                nodeSubInfoPaint.setAlpha(alpha);
                String seatNumber = cachedSeatNumberClusterMap.get(bleAddress);
                if (seatNumber == null) {
                    //
                    seatNumber = String.valueOf(anchor.getSeatNumber()) +
                            ", " + Util.formatAsHexa(anchor.getClusterMap(), false) +
                            ", " + Util.formatAsHexa(anchor.getClusterNeighbourMap(), false);
                    cachedSeatNumberClusterMap.put(bleAddress, seatNumber);
                }
                canvas.drawText(seatNumber,
                        x,
                        y + (1.2f + .3f * alpha / 255) * NODE_SUBINFO_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, nodeSubInfoPaint);
                // anchor list
                String sAnchorList = cachedAnchorList.get(bleAddress);
                if (sAnchorList == null) {
                    StringBuilder sb = new StringBuilder();
                    List<Short> anchorList = anchor.getAnchorList();
                    if (anchorList != null && !anchorList.isEmpty()) {
                        for (Short shortAnchorId : anchorList) {
                            NetworkNode node = networkNodeByShortIdResolver.apply(shortAnchorId);
                            if (node != null && node.getLabel() != null) {
                                if (sb.length() > 0) {
                                    sb.append(", ");
                                }
                                sb.append(node.getLabel());
                            }
                        }
                    }
                    sAnchorList = sb.toString();
                    cachedAnchorList.put(bleAddress, sAnchorList);
                }
                if (sAnchorList.length() > 0) {
                    // draw it
                    canvas.drawText(sAnchorList,
                            x,
                            y + (1.2f + .3f * anchorLabelAlpha / 255) * NODE_SUBINFO_TEXT_SIZE * 2 * LCD_DIP_SCALING_FACTOR, nodeSubInfoPaint);
                }
            }
        }
    }

    private void checkNodesMatch(NetworkNode n1, NetworkNode n2, Function<NetworkNode,Object> getterReference) {
        Preconditions.checkState(n1.getType() == n2.getType(),
                "node types do not match: n1 = " + n1 + ", n2 = " + n2);
        Object prop1 = getterReference.apply(n1);
        Object prop2 = getterReference.apply(n2);
        // check equality
        Preconditions.checkState(Objects.equals(prop1, prop2), "properties "
                + prop1 + " and "
                + prop2 + " do not match: n1 = " + n1 + ", n2 = " + n2);
    }

    private Position mappedPosition = new Position();

    private void mapPositionToFout(Position inPosition) {
        normalizePosition(inPosition, mappedPosition);
        fIn[0] = mappedPosition.x;
        fIn[1] = mappedPosition.y;
        drawMatrix.mapPoints(fOut, fIn);
    }

    // convert the position so that it is in centimeters (native units are millimeters)
    private void normalizePosition(Position position, Position mappedPosition) {
        position.divide(NORMALIZATION_DISTANCE_UNIT_FACTOR, mappedPosition);
    }

    private void drawTag(Canvas canvas, float x, float y, float closeToScreenEdgeFactor, TagNode tag, int alpha) {
        Position position = tag.extractPositionDirect();
        if (DEBUG_DRAW_NODE) {
            log.d("drawing tag: " + tag.getBleAddress() + ", position: " + position);
        }
        // tag is a circle
        tagPaint.setColor(getColorForTag(tag));
        tagPaint.setStyle(Paint.Style.FILL);
        tagPaint.setAlpha(alpha);
        canvas.drawCircle(x, y, tagCircleRadius, tagPaint);
        // draw label
        if (tagLabelAlpha > 0) {
            alpha = Math.min(alpha, Math.min(tagLabelAlpha, (int) (closeToScreenEdgeFactor * 255)));
            tagLabelPaint.setAlpha(alpha);
            canvas.drawText(tag.getLabel(), x, y - (.5f + .3f * alpha / 255f) * TAG_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, tagLabelPaint);
            // now the position
            nodeSubInfoPaint.setAlpha(alpha);
            //noinspection ConstantConditions
            String strPosition = Util.formatLength(position.x, lengthUnit)
                    + ", " + Util.formatLength(position.y, lengthUnit)
                    + ", " + Util.formatLength(position.z, lengthUnit);
            canvas.drawText(strPosition,
                    x,
                    y + (1.6f + .3f * alpha / 255) * NODE_SUBINFO_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, nodeSubInfoPaint);
        }
    }

    private void drawTagRangingAnchors(Canvas canvas, TagNode tagNode, int lineAlpha, int textAlpha) {
        // use floating average
        Position p;
        if(showAverageSupplier.get()){
            p = avgNodesById.get(tagNode.getId()).averagep();
        } else {
            p = tagNode.extractPositionDirect();
        }
        if (p == null) {
            // skip this one
            return;
        }
        mapPositionToFout(p);
        float x = fOut[0];
        float y = fOut[1];
        // check if we are on-screen
        float closeToScreenEdgeFactor = closeToScreenEdgeFactor(x, y);
        if (closeToScreenEdgeFactor == 0) {
            // skip this one
            return;
        } // else:
        lineAlpha = Math.min(lineAlpha, (int) (255 * closeToScreenEdgeFactor));
        textAlpha = Math.min(textAlpha, (int) (255 * closeToScreenEdgeFactor));
        // draw ranging anchors
        //noinspection ConstantConditions
        for (RangingAnchor rangingAnchor : tagNode.extractDistancesDirect()) {
            drawRangingAnchor(canvas, lineAlpha, textAlpha, x, y, rangingAnchor, tagNode);
        }
    }

    private void drawRangingAnchor(Canvas canvas, int lineAlpha, int textAlpha, float tagX, float tagY, RangingAnchor rangingAnchor, TagNode tag) {
        if (rangingAnchor == null) {
            // skip this one
            return;
        } // else: try to resolve (by short id)
        NetworkNode anchor = networkNodeByShortIdResolver.apply(rangingAnchor.nodeId);
        // retrieve our anchor (by long id)
        anchor = anchor == null ? null : nodesById.get(anchor.getId());
        Position anchorPosition;
        if (anchor == null || (anchorPosition = anchor.extractPositionDirect()) == null) {
            if (DEBUG) {
                if (anchor == null) {
                    log.d("ranging anchor " + rangingAnchor.nodeId + " cannot be resolved, skipping");
                } else {
                    log.d("position of ranging anchor " + rangingAnchor.nodeId + " cannot be resolved, skipping");
                }
            }
            return;
        }
        if (Constants.DEBUG) {
            checkNodesMatch(anchor, networkNodeByShortIdResolver.apply(anchor.getId().shortValue()), NetworkNode::extractPositionDirect);
        }
        // draw the lines
        drawRangingLine(canvas, lineAlpha, textAlpha, tagX, tagY, rangingAnchor, tag, anchorPosition, highlightIconsistentRangingDistances.get());
    }

    private void drawRangingLine(Canvas canvas,
                                 int lineAlpha,
                                 int textAlpha,
                                 float tagX,
                                 float tagY,
                                 RangingAnchor rangingAnchor, TagNode tag, Position anchorPosition,
                                 boolean highlightInconsistency) {
        //
        int color = rangingAnchorOkColor;
        boolean inconsistentDistance = false;
        int computedDistance = -1;
        if (highlightInconsistency) {
            computedDistance = computeDistance(tag, anchorPosition);
            inconsistentDistance = Math.abs(computedDistance - rangingAnchor.distance.length) > INCONSISTENT_DISTANCE_TOLERANCE;
            if (inconsistentDistance) {
                // this is inconsistent
                color = rangingAnchorFailColor;
            }
        }
        rangingAnchorLinePaint.setColor(color);
        rangingAnchorLinePaint.setAlpha(lineAlpha);
        // compute where the line goes to
        mapPositionToFout(anchorPosition);
        float aX = fOut[0];
        float aY = fOut[1];
        //
        float xDelta = aX - tagX;
        float yDelta = aY - tagY;
        if (inconsistentDistance) {
            float ratio = 1f * rangingAnchor.distance.length / computedDistance;
            aX = tagX + xDelta * ratio;
            aY = tagY + yDelta * ratio;
        }
        canvas.drawLine(tagX, tagY, aX, aY, rangingAnchorLinePaint);
        if (textAlpha > 0) {
            // draw also the number
            rangingAnchorLabelPaint.setColor(color);
            rangingAnchorLabelPaint.setAlpha(textAlpha);
            float angle;
            float toZeroFactor;
            if (xDelta == 0) {
                if (aY - tagY > 0) {
                    angle = (float) (Math.PI / 2);
                } else {
                    angle = -(float) (Math.PI / 2);
                }
                toZeroFactor = 1;
            } else {
                angle = (float) Math.atan((aY - tagY) / xDelta);
                toZeroFactor = (float) Math.abs(angle / Math.PI / 2);
                if (xDelta < 0) {
                    // we have to adjust the angle
                    angle = (float) (angle + Math.PI);
                }
            }
            float maxDistance = (float) (Math.sqrt(xDelta * xDelta + yDelta * yDelta) / 2);
            float offset = Math.min(maxDistance, LCD_DIP_SCALING_FACTOR * 40);
            float dtY = tagY + (float) (Math.sin(angle) * offset);
            float dtX = tagX + (float) (Math.cos(angle) * offset);
            // compute the 'middle'
            canvas.drawText(Util.formatLength(rangingAnchor.distance.length, lengthUnit), dtX,
                    dtY - (0.3f + toZeroFactor) * RANGING_ANCHOR_DISTANCE_LABEL_TEXT_SIZE * LCD_DIP_SCALING_FACTOR, rangingAnchorLabelPaint);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int computeDistance(TagNode tag, Position anchorPosition) {
        Position p1 = tag.extractPositionDirect();
        // deltas
        int xDelta = Math.abs(p1.x - anchorPosition.x);
        int yDelta = Math.abs(p1.y - anchorPosition.y);
        int zDelta = Math.abs(p1.z - anchorPosition.z);
        // compute distance
        return (int) (Math.sqrt(xDelta * xDelta + yDelta * yDelta + zDelta * zDelta) + 0.5);
    }

    private int getColorForTag(NetworkNode tagNode) {
        try {
            return tagColorCache.get(tagNode.getBleAddress());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    float[] matrixValues2 = new float[9];
    float currTenMetersInPixels;

    private class OnScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            //
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            if (floorPlanBound) {
                drawMatrix.getValues(matrixAsFloat);
                float finalScale = matrixAsFloat[0] * scaleFactor;
                if (finalScale >= minScaleFactorCmToPx && finalScale <= maxScaleFactorCmToPx) {
                    // do scale
                    drawMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                    onDrawMatrixChanged();
                    if (DEBUG_MATRIX) {
                        log.d("Zoom:drawMatrix = " + drawMatrix);
                    }
                    // draw again
                    invalidate();
                    return true;
                }
            } else {
                // we are just mapping this to floorplan properties
                currTenMetersInPixels = currTenMetersInPixels / scaleFactor;
                // we must have at least 20 pixels and at most 1000 pixels in one meter
                if (currTenMetersInPixels >= FLOORPLAN_MIN_PIXELS_IN_TEN_METERS && currTenMetersInPixels <= FLOORPLAN_MAX_PIXELS_IN_TEN_METERS) {
                    drawFloorPlanVirtualMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                    // let's interpret the values in the matrix
                    if (virtualFloorPlanMatrixToFloorPlanProperties(true)) {
                        // recompute the drawflooplan matrix according to what's saved in the floorplan properties
                        setupBaseFpMatrixFromFpProperties();
                        setupDrawFpMatrix();
                        // notify callback
                        if (floorPlanChangedCallback != null) floorPlanChangedCallback.call(floorPlan);
                    }
                    invalidate();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // reset caches
            if (floorPlan != null) {
                currTenMetersInPixels = floorPlan.tenMetersInPixels;
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // do nothing
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!floorPlanBound) {
                // we are adjusting floorplan, ignore
                return false;
            }
            // determine what was the intention of the click
            float x = e.getX();
            float y = e.getY();
            if (DEBUG) {
                log.d("onSingleTapConfirmed: " + "x = [" + x + "], y = [" + y + "]");
            }
            //
            NetworkNode node = lookupClosest(x, y);
            if (node != null && nodeClickListener != null) {
                nodeClickListener.call(node);
            }
            return true;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!floorPlanBound) {
                // we are adjusting floorplan, ignore
                return false;
            }
            float x = e.getX();
            float y = e.getY();
            // start zoom in animation
            if (DEBUG) {
                log.d("DblTap at: (" + x + "," + y + ")");
            }
            if (scaleFactorCmToPx < maxScaleFactorCmToPx * 0.9) {
                setupZoomAnimation(x, y, ZOOM_FACTOR_ON_DOUBLE_TAP, ZOOM_IN_ON_DOUBLE_TAP_DURATION);
                ignoreNextAbort = true;
                invalidate();
                return true;
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            if (floorPlanBound) {
                drawMatrix.postTranslate(-dx, -dy);
                if (DEBUG_MATRIX) {
                    log.d("Drag:drawMatrix = " + drawMatrix);
                }
                onDrawMatrixTranslationChanged();
            } else {
                drawFloorPlanVirtualMatrix.postTranslate(-dx, -dy);
                // recompute the shift to floorplan properties
                if (virtualFloorPlanMatrixToFloorPlanProperties(false)) {
                    // recompute the drawfloorplan matrix according to what's saved in the floorplan properties
                    setupBaseFpMatrixFromFpProperties();
                    setupDrawFpMatrix();
                    // notify callback
                    if (floorPlanChangedCallback != null) floorPlanChangedCallback.call(floorPlan);
                }
            }
            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DEBUG) {
                log.d("xVelocity = " + velocityX);
                log.d("yVelocity = " + velocityY);
            }
            if (floorPlanBound) {
                // remember how the matrix looked like
                flingStartMatrix.set(drawMatrix);
                // start the fling gesture
                mScroller.fling(
                        0, 0,
                        (int) velocityX, (int) velocityY,
                        Integer.MIN_VALUE, Integer.MAX_VALUE,
                        Integer.MIN_VALUE, Integer.MAX_VALUE
                );
                // draw again
                invalidate();
            } // else: do not allow fling when adjusting floorplan
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            abortOngoingAnimations();
            return super.onDown(e);
        }
    }

    /**
     * Convert virtual floorplan draw matrix values to floorplan properties.
     * @param alsoScale whether to adjust also tenMetersInPixels value (optimization)
     * @return true if there was any change in floorPlan properties
     */
    private boolean virtualFloorPlanMatrixToFloorPlanProperties(boolean alsoScale) {
        // determine the shift from drawFloorPlanMatrixVirtual
        drawFloorPlanVirtualMatrix.getValues(matrixValues2);
        // check the values
        if (DEBUG) {
            // make precondition checks
            Preconditions.checkState(matrixValues2[Matrix.MSCALE_X] == matrixValues2[Matrix.MSCALE_Y],
                    "scales are different: " + matrixValues2[Matrix.MSCALE_X] + " != " + matrixValues2[Matrix.MSCALE_Y]);
            Preconditions.checkState(matrixValues2[3] == 0);
            Preconditions.checkState(matrixValues2[1] == 0);
        }
        float fpPxToCanvasPxScale = matrixValues2[Matrix.MSCALE_X];
        // compute the pure floorplan screen pixel shift (eliminate the shift caused by grid scroll)
        float floorPlanScreenPixelShiftX = matrixValues2[Matrix.MTRANS_X] - xPxOffset;
        float floorPlanScreenPixelShiftY = matrixValues2[Matrix.MTRANS_Y] - yPxOffset;
        // transform the screen shift to floorplan pixel shift
        float floorPlanCenterX = - floorPlanScreenPixelShiftX / fpPxToCanvasPxScale;
        float floorPlanCenterY = - floorPlanScreenPixelShiftY / fpPxToCanvasPxScale;
        // save the shift
        int oldPxCenterX = floorPlan.pxCenterX;
        int oldPxCenterY = floorPlan.pxCenterY;
        floorPlan.pxCenterX = Math.round(floorPlanCenterX);
        floorPlan.pxCenterY = Math.round(floorPlanCenterY);
        boolean change = oldPxCenterX != floorPlan.pxCenterX || oldPxCenterY != floorPlan.pxCenterY;
        if (alsoScale) {
            // save the scale
            currTenMetersInPixels = 1000 * scaleFactorCmToPx / fpPxToCanvasPxScale;
            // propagate the values to floorplan properties
            int oldTenMetersInPixels = floorPlan.tenMetersInPixels;
            floorPlan.tenMetersInPixels = (int) (currTenMetersInPixels + 0.5);
            change = change || (oldTenMetersInPixels != floorPlan.tenMetersInPixels);
        }
        return change;
    }

    private boolean ignoreNextAbort = false;

    private void abortOngoingAnimations() {
        if (Constants.DEBUG) {
            log.d("abortOngoingAnimations");
        }
        if (ignoreNextAbort) {
            ignoreNextAbort = false;
            return;
        }
        if (!mZoomer.isFinished()) {
            mZoomer.abortAnimation();
        }
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    private void setupZoomAnimation(float x, float y, float zoomFactor, int zoomInDuration) {
        float targetScaleFactorToPx = Math.min(Math.max(scaleFactorCmToPx * zoomFactor, minScaleFactorCmToPx), maxScaleFactorCmToPx);
        float targetZoomFactor = targetScaleFactorToPx / scaleFactorCmToPx;
        zoomFocalPoint.set(x, y);
        zoomStartMatrix.set(drawMatrix);
        if (DEBUG) {
            log.d("starting zoom animation, targetZoomFactor = " + targetZoomFactor + ", targetScaleFactorToPx = " + targetScaleFactorToPx);
        }
        mZoomer.startAnimation(1, targetZoomFactor - 1, zoomInDuration);
    }

    private void setupRotateFloorPlanAnimation(float fromDegrees, float targetDegrees) {
        if (!mFpRotator.isFinished()) {
            mFpRotator.abortAnimation();
        }
        mFpRotator.startAnimation(fromDegrees, targetDegrees - fromDegrees);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean s = mScaleGestureDetector.onTouchEvent(event);
        boolean g = mGestureDetector.onTouchEvent(event);
        return s || g;
    }

    // this is naive implementation, once we have KD tree implemented, we should
    // revisit this topic
    public NetworkNode lookupClosest(float x, float y) {
        float minX = x - clickTolerance;
        float maxX = x + clickTolerance;
        float minY = y - clickTolerance;
        float maxY = y + clickTolerance;
        NodeAndPosition closestNode = null;
        float closestDistancePower = -1;
        // lookup
        for (NodeAndPosition nodeAndPosition : nodesByXCoordinate) {
            if (DEBUG) {
                log.d("lookupClosest: trying " + nodeAndPosition + " minX = " + minX + ", maxX = " + maxX + ", minY = " + minY + ", maxY = " + maxY);
            }
            if (nodeAndPosition.x >= minX) {
                if (nodeAndPosition.x > maxX) {
                    break;
                } // else: x axis matches, now check y axis
                if (nodeAndPosition.y >= minY) {
                    if (nodeAndPosition.y > maxY) {
                        // skip this one
                        continue;
                    } // else: y axis matches as well
                    float dp = distancePow(x, y, nodeAndPosition);
                    if (DEBUG) {
                        log.d("lookupClosest: hit node " + Util.shortenNodeId(nodeAndPosition.node.getId(), false) + ", distance = " + dp);
                    }
                    if (closestNode == null) {
                        closestNode = nodeAndPosition;
                        closestDistancePower = dp;
                    } else if (dp < closestDistancePower) {
                        closestNode = nodeAndPosition;
                        closestDistancePower = dp;
                    }
                }
            }
        }
        return closestNode == null ? null : closestNode.node;
    }

    private static float distancePow(float x, float y, NodeAndPosition nodeAndPosition) {
        float dx = Math.abs(nodeAndPosition.x - x);
        float dy = Math.abs(nodeAndPosition.y - y);
        return dx * dx + dy * dy;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG)
            log.d("onSizeChanged() called with: " + "w = [" + w + "], h = [" + h + "], oldw = [" + oldw + "], oldh = [" + oldh + "]");
        // remember the size
        pxWidth = w;
        pxHeight = h;
        // fixing bug #52
        if (!fullyConfigured()) {
            // nothing to recompute
            return;
        }
//        // determine display width in cms
//        float cmWidth = INCH_TO_CM * pxWidth / DisplayMetrics.LCD_DENSITY_DPI;
//        float cmHeight = INCH_TO_CM * pxHeight / DisplayMetrics.LCD_DENSITY_DPI;
//        // one cm on a screen should be 100 centimeters in reality
//        float virtualWidthCm = cmWidth * INITIAL_ZOOM_RATIO;
//        float virtualHeightCm = cmHeight * INITIAL_ZOOM_RATIO;
        // fixing problem with keyboard resizing the drawn content
        if (drawMatrix == null) {
            // we do not have the drawmatrix yet
            if (injectedScale != null) {
                computeInjectedMatrix();
                if (extraAnimatedZoom != 1) {
                    setupZoomAnimation(w / 2, h / 2, extraAnimatedZoom, ZOOM_IN_ANIMATION_DURATION);
                    extraAnimatedZoom = 1f;
                }
            } else {
                // compute the whole matrix
                computeInitialMatrix();
            }
            firstShowSysTime = SystemClock.uptimeMillis();
        }
        if (DEBUG_MATRIX) {
            log.d("drawMatrix = " + drawMatrix);
        }
        // compute what is the minimal step in px from which we draw grid marks/labels
        int showGridMarksFrom;
        if (h > w) {
            // seems that we are in portrait
            // we do not want to have 3 gridmarks in row (except when in extreme case)
            showGridMarksFrom = (int) ((w / 2) *.9 + 0.5);
        } else {
            showGridMarksFrom = (int) ((h / 2) * 1.05 + 0.5);
        }
        minStepInPxGridMark = Math.min(pixelsPerCm * SHOW_GRID_MARKS_FROM_SCREEN_DISTANCE_CM_MAX, showGridMarksFrom);
        // call super
        super.onSizeChanged(w, h, oldw, oldh);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // state save/restore
    //
    void computeInjectedMatrix() {
        if (DEBUG) log.d("computeInjectedMatrix(), injectedScale = " + injectedScale + ", injectedFocalPoint = " + injectedFocalPoint);
        //noinspection ConstantConditions
        drawMatrix = DEBUG_MATRIX ? new MatrixWrapper() : new Matrix();
        // set scale first
        drawMatrix.setScale(injectedScale, -injectedScale);
        // now when we are in pixels, set transition according to focal point (which is in px)
        drawMatrix.postTranslate(pxWidth / 2 - injectedFocalPoint.x, pxHeight / 2 - injectedFocalPoint.y);
        // notify the listeners
        onDrawMatrixChanged();
    }

    public PointF getFocalPointInPx() {
        float halfWidth = pxWidth / 2;
        float halfHeight = pxHeight / 2;
        //
        return new PointF(halfWidth - xPxOffset, halfHeight - yPxOffset);
    }

    public float getScaleFactorCmToPx() {
        return scaleFactorCmToPx;
    }

    public void setFocalPointAndScale(float scaleFactorToPx, PointF focalPoint, float extraAnimatedZoom) {
        if (Constants.DEBUG)
            log.d("setFocalPointAndScale() called with: " + "scaleFactorToPx = [" + scaleFactorToPx + "], focalPoint = [" + focalPoint + "], extraAnimatedZoom = [" + extraAnimatedZoom + "]");
        this.injectedFocalPoint = focalPoint;
        this.injectedScale = scaleFactorToPx;
        this.extraAnimatedZoom = extraAnimatedZoom;
    }

    private boolean fullyConfigured() {
        return floorPlanProvider != null;
    }

    public void onNodeChanged(NetworkNode newNode) {
        if (Constants.DEBUG) log.d("onNodeChanged() called with: " + "newNode = [" + newNode + "]");
        NetworkNode myNode = nodesById.get(newNode.getId());
        TagAvg avgNode = avgNodesById.get(newNode.getId());

        if (Constants.DEBUG) {
            Preconditions.checkNotNull(myNode, "node " + newNode + " is not known, call onNodeUpdatedAndOrAddedToNetwork first!");
            Preconditions.checkState(newNode.getNetworkId().equals(myNode.getNetworkId()));
        }
        // now check if this change is significant
        if (!visualizedPropertiesChanged(myNode, newNode)) {
            // none of the visualized properties has changed
            // just copy the node properties and leave
            myNode.copyFrom(newNode);
            return;
        }
        Position p1 = newNode.extractPositionDirect();
        Position p2 = myNode.extractPositionDirect();
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(p1);
            Preconditions.checkNotNull(p2);
        }
        //noinspection ConstantConditions
        boolean zAxisChanged = p1.z != p2.z;
        boolean nodeTypeChanged = myNode.getType() != newNode.getType();
        if (zAxisChanged || nodeTypeChanged) {
            // remove the node first
            nodesByZAxis.remove(myNode);
        }
        //
        removeCachedDebugStrings(myNode.getBleAddress());
        // copy properties to 'myNode'
        if (nodeTypeChanged) {
            // we have to create a completely new copy
            myNode = NodeFactory.newNodeCopy(newNode);
            // replace the node in the node map
            nodesById.put(myNode.getId(), myNode);
        } else {
            // it is safe to simply copy the node properties
            myNode.copyFrom(newNode);
        }
        if (zAxisChanged || nodeTypeChanged) {
            // add 'myNode' again - properly sorted according to Z-axis
            nodesByZAxis.add(myNode);
        }

        if(newNode.getType() == NodeType.TAG) {
            // update the node in the node map
            avgNode.updatexyz(p1);
        }

        // and now redraw
        invalidate();
    }

    private void removeCachedDebugStrings(String bleAddress) {
        cachedSeatNumberClusterMap.remove(bleAddress);
        cachedAnchorList.remove(bleAddress);
    }

    public boolean isAwareOfNode(long nodeId) {
        return nodesById.containsKey(nodeId);
    }

    public void onNodeAdded(NetworkNode node) {
        // create a node copy first
        node = NodeFactory.newNodeCopy(node);
        // now organize
        NetworkNode prevNode = nodesById.put(node.getId(), node);
        // add node to average map
        avgNodesById.put(node.getId(), new TagAvg());

        if (Constants.DEBUG) {
            log.d("onNodeUpdatedAndOrAddedToNetwork() called with: " + "node = [" + node + "]");
            Preconditions.checkState(prevNode == null);
        }
        nodesByZAxis.add(node);
        // and now redraw
        invalidate();
    }

    public void onNodeRemoved(Long nodeId) {
        NetworkNode node = nodesById.remove(nodeId);
        // remove node from average map
        avgNodesById.remove(nodeId);

        if (Constants.DEBUG) {
            log.d("onNodeRemoved() called with: " + "nodeId = [" + nodeId + "]");
            Preconditions.checkState(node != null);
        }
        if (node.isAnchor()) {
            removeCachedDebugStrings(node.getBleAddress());
        }
        nodesByZAxis.remove(node);
        // and now redraw
        invalidate();
    }

    private boolean visualizedPropertiesChanged(NetworkNode node1, NetworkNode node2) {
        Set<NetworkNodeProperty> diff = node1.compareByProperty(node2);
        return diff.contains(NetworkNodeProperty.ANCHOR_POSITION)
                || diff.contains(NetworkNodeProperty.LABEL)
                || diff.contains(NetworkNodeProperty.NODE_TYPE)
                || diff.contains(NetworkNodeProperty.ANCHOR_SEAT)
                || diff.contains(NetworkNodeProperty.ANCHOR_INITIATOR)
                || diff.contains(NetworkNodeProperty.ANCHOR_CLUSTER_MAP)
                || diff.contains(NetworkNodeProperty.ANCHOR_CLUSTER_NEIGHBOUR_MAP)
                || diff.contains(NetworkNodeProperty.ANCHOR_AN_LIST)
                || diff.contains(NetworkNodeProperty.TAG_LOCATION_DATA)
                ;
    }

    float matrixAsFloat[] = new float[9];

    private void onDrawMatrixChanged() {
        drawMatrix.getValues(matrixAsFloat);
        // extract the scale
        scaleFactorCmToPx = matrixAsFloat[0];
        // fill shown grid lines
        fillGridLinesForScale(shownGridLines, scaleFactorCmToPx);
        if (DEBUG) {
            log.d("scaleFactorCmToPx = " + scaleFactorCmToPx);
            logGridLinesForCurrentScale();
        }
        /////////////////////////////////////////////////////////////////////////////////////////
        // determine how big are the anchor triangles
        float anchorGrowFactor = getScaleFactorTransformed(maxAnchorSignGrowToScaleFactorToPx, minAnchorSignGrowFromScaleFactorToPx);
        anchorTriangleSize = LCD_DIP_SCALING_FACTOR *
                (ANCHOR_MIN_TRIANGLE_SIZE + (ANCHOR_MAX_TRIANGLE_SIZE - ANCHOR_MIN_TRIANGLE_SIZE) * anchorGrowFactor);
        anchorLabelAlpha = (int) (255 * ((anchorGrowFactor - 0.5) * 2));
        rangingAnchorLineAlpha = (int) (255 * getScaleFactorTransformed(maxRangingAnchorLineScaleFactorToPx, minRangingAnchorLineScaleFactorToPx) + 0.5);
        //
        if (scaleFactorCmToPx >= maxAnchorFullColorScaleFactorToPx) {
            anchorSignAlpha = 255;
        } else if (scaleFactorCmToPx <= minAnchorStartFullColorScaleFactorToPx) {
            anchorSignAlpha = ANCHOR_TRIANGLE_MIN_ALPHA;
        } else {
            float diff = scaleFactorCmToPx - minAnchorStartFullColorScaleFactorToPx;
            anchorSignAlpha = ANCHOR_TRIANGLE_MIN_ALPHA +
                    (int) ((255 - ANCHOR_TRIANGLE_MIN_ALPHA) * diff / (maxAnchorFullColorScaleFactorToPx - minAnchorStartFullColorScaleFactorToPx));
        }
        /////////////////////////////////////////////////////////////////////////////////////////
        // determine how big are tag circles
        float tagGrowFactor;
        if (scaleFactorCmToPx >= maxTagSignGrowToScaleFactorToPx) {
            tagGrowFactor = 1;
        } else if (scaleFactorCmToPx <= minTagSignGrowFromScaleFactorToPx) {
            tagGrowFactor = 0;
        } else {
            float diff = scaleFactorCmToPx - minTagSignGrowFromScaleFactorToPx;
            tagGrowFactor = diff / (maxTagSignGrowToScaleFactorToPx - minTagSignGrowFromScaleFactorToPx);
        }
        tagCircleRadius = LCD_DIP_SCALING_FACTOR *
                (TAG_MIN_CIRCLE_RADIUS + (TAG_MAX_CIRCLE_RADIUS - TAG_MIN_CIRCLE_RADIUS) * tagGrowFactor);
        tagLabelAlpha = (int) (255 * ((tagGrowFactor - 0.5) * 2));
        //
        if (Constants.DEBUG) {
            // have 1% tolerance
            Preconditions.checkState(scaleFactorCmToPx <= maxScaleFactorCmToPx * 1.01,
                    "scaleFactorCmToPx = " + scaleFactorCmToPx + ", max = " + maxScaleFactorCmToPx);
            Preconditions.checkState(scaleFactorCmToPx >= minScaleFactorCmToPx * 0.99,
                    "scaleFacotrToPx = " + scaleFactorCmToPx + ", min = " + minScaleFactorCmToPx);
        }
        /////////////////////////////////////////////////////////////////////////////////////////
        // adjust floor plan matrix accordingly
        // center the floor plan
        refreshFloorplanMatrixAndCaches();
    }

    private float getScaleFactorTransformed(float maxScaleFactor, float minScaleFactor) {
        float anchorGrowFactor;
        if (scaleFactorCmToPx >= maxScaleFactor) {
            anchorGrowFactor = 1;
        } else if (scaleFactorCmToPx <= minScaleFactor) {
            anchorGrowFactor = 0;
        } else {
            float diff = scaleFactorCmToPx - minScaleFactor;
            anchorGrowFactor = diff / (maxScaleFactor - minScaleFactor);
        }
        return anchorGrowFactor;
    }

    private Float lastRotation;

    public void fetchFreshFloorPlanAndRedraw(boolean animateRotation) {
        FloorPlan newFp = floorPlanProvider.get();
        if (animateRotation && lastRotation != null) {
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(newFp, "floorplan must NOT be null");
            }
            float fromDegrees;
            if (!mFpRotator.isFinished()) {
                fromDegrees = mFpRotator.getCurrValue();
            } else {
                fromDegrees = lastRotation;
            }
            // store last rotation
            float targetRotation = (float) newFp.rotation;
            if (fromDegrees != targetRotation) {
                if (fromDegrees < 0 && targetRotation == 0) {
                    // normalize
                    fromDegrees += 360;
                }
                // setup the rotate animation
                setupRotateFloorPlanAnimation(fromDegrees, targetRotation);
            }
        } else {
            if (!mFpRotator.isFinished()) mFpRotator.abortAnimation();
        }
        if (newFp != null) {
            // store the last rotation
            lastRotation = (float) newFp.rotation;
        }
        // now draw
        refreshFloorplanMatrixAndCaches();
        invalidate();
    }

    private void refreshFloorplanMatrixAndCaches() {
        this.floorPlan = floorPlanProvider.get();
        if (this.floorPlan != null) {
            setupBaseFpMatrixFromFpProperties();
        } else {
            baseFloorPlanMatrix.reset();
        }
        onDrawMatrixTranslationChangedArrayReady();
    }

    private void setupBaseFpMatrixFromFpProperties() {
        baseFloorPlanMatrix.reset();
        baseFloorPlanMatrix.postTranslate(-floorPlan.pxCenterX, -floorPlan.pxCenterY);
        // now apply the proper scaling
        // how many pixels is 10 meters in the current grid scale?
        float gridTenMetersInPixels = 1000 * scaleFactorCmToPx;
        // we need to shrink/stretch floorPlan.tenMetersInPixels to gridTenMetersInPixels
        float scale = gridTenMetersInPixels / floorPlan.tenMetersInPixels;
        baseFloorPlanMatrix.postScale(scale, scale);
    }

    private void onDrawMatrixTranslationChanged() {
        drawMatrix.getValues(matrixAsFloat);
        onDrawMatrixTranslationChangedArrayReady();
    }

    private void onDrawMatrixTranslationChangedArrayReady() {
        // extract the translation values
        xPxOffset = matrixAsFloat[2];
        yPxOffset = matrixAsFloat[5];
        // shift the matrix
        if (floorPlan != null) setupDrawFpMatrix();
    }

    private void setupDrawFpMatrix() {
        // floor plan matrix - move the center to the same center as the grid
        drawFloorPlanMatrix.set(baseFloorPlanMatrix);
        drawFloorPlanMatrix.postTranslate(xPxOffset, yPxOffset);
        //
        if (DEBUG_MATRIX) {
            log.d("xPxOffset = " + xPxOffset);
            log.d("yPxOffset = " + yPxOffset);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void computeInitialMatrix() {
        //noinspection ConstantConditions
        drawMatrix = DEBUG_MATRIX ? new MatrixWrapper() : new Matrix();
        // now scale it: presume that we have INITIAL_ZOOM_RATIO = 100
        // when we draw point (0,0) and point (100,0), which is 1m distance in reality
        // we want them to be 1cm from each other on the display
        //
        // for 160dpi displays, where one inch is 160px, this means that the distance on display should be 160/2.54 pixels
        // this is: 100 * scaleFactor = 63 on 160dpi display (more generically it is DPI / 2.54)
        float initialScaleFactor = DisplayMetrics.XDPI / INCH_TO_CM / INITIAL_ZOOM_RATIO;
        // move it by one step (3 meters)
        // compute the initial translate - according to focal point
        int sumX = 0, sumY = 0;
        int i = 0;
        for (NetworkNode node : nodesById.values()) {
            if (node.isAnchor()) {
                Position position = node.extractPositionDirect();
                sumX += position.x;
                sumY += position.y;
                i++;
            }
        }
        if (i == 0) {
            // do nothing
            i = 1;
        }
        int avgX = sumX / i / NORMALIZATION_DISTANCE_UNIT_FACTOR;
        int avgY = sumY / i / NORMALIZATION_DISTANCE_UNIT_FACTOR;
        if (DEBUG) {
            log.d("focal point of network is: x = " + avgX + ", y = " + avgY);
        }
        // the focal point should be in 0,0
        drawMatrix.setTranslate(-avgX, -avgY);
        // set the proper scale after move
        drawMatrix.postScale(initialScaleFactor, -initialScaleFactor);
        // now center the image - so that what is in 0,0 (focal point) is in the center of the screen
        drawMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
        // notify the listeners
        onDrawMatrixChanged();
    }

    private void computeLimits() {
        // compute immutable limits
        pixelsPerCm = DisplayMetrics.XDPI / INCH_TO_CM;
        minScaleFactorCmToPx = pixelsPerCm / MIN_ZOOM_RATIO;
        maxScaleFactorCmToPx = pixelsPerCm / MAX_ZOOM_RATIO;
        if (DEBUG) {
            log.d("minScaleFactorCmToPx = " + minScaleFactorCmToPx);
            log.d("maxScaleFactorCmToPx = " + maxScaleFactorCmToPx);
        }
        // anchor
        minAnchorSignGrowFromScaleFactorToPx = getScaleFactorFor4cmOnScreen(GROW_ANCHOR_TRIANGLE_FROM_ON_SCREEN_4CM_IS_X_M);
        maxAnchorSignGrowToScaleFactorToPx = getScaleFactorFor4cmOnScreen(GROW_ANCHOR_TRIANGLE_TO_ON_SCREEN_4CM_IS_X_M);
        // ranging anchor
        minRangingAnchorLineScaleFactorToPx = getScaleFactorFor4cmOnScreen(RANGING_ANCHOR_SHOW_FROM_ON_SCREEN_4CM_IS_X_M);
        maxRangingAnchorLineScaleFactorToPx = getScaleFactorFor4cmOnScreen(RANGING_ANCHOR_SHOW_TO_ON_SCREEN_4CM_IS_X_M);
        //
        minAnchorStartFullColorScaleFactorToPx = getScaleFactorFor4cmOnScreen(ANCHOR_TRIANGLE_START_FULL_COLOR_FROM_ON_SCREEN_4CM_IS_X_M);
        maxAnchorFullColorScaleFactorToPx = getScaleFactorFor4cmOnScreen(ANCHOR_TRIANGLE_FULL_COLOR_FROM_ON_SCREEN_4CM_IS_X_M);
        // tag
        minTagSignGrowFromScaleFactorToPx = getScaleFactorFor4cmOnScreen(GROW_TAG_MARK_FROM_ON_SCREEN_4CM_IS_X_M);
        //noinspection PointlessArithmeticExpression
        maxTagSignGrowToScaleFactorToPx = getScaleFactorFor4cmOnScreen(GROW_TAG_MARK_TO_ON_SCREEN_4CM_IS_X_M);

        // map grid lines distances to scaleFactor
        if (lengthUnit == LengthUnit.METRIC) {
            gridLinesMinimalScaleFactor = new float[GRID_LINE_STEP_CM.length];
            int shortestGridSquareInPx = (int) (pixelsPerCm * SHORTEST_GRID_SQUARE_MM / 10f + 0.5);
            for (int i = 0; i < GRID_LINE_STEP_CM.length; i++) {
                // store the ratio which will transform the boundary in cm to shortestGridSquareInPx
                float minFactor = 1f * shortestGridSquareInPx / GRID_LINE_STEP_CM[i];
                if (minFactor < minScaleFactorCmToPx) {
                    // we will never compare with something bigger than minScaleFactorCmToPx
                    minFactor = 0;
                }
                gridLinesMinimalScaleFactor[i] = minFactor;
                if (DEBUG) {
                    log.d("scalefactor limit for " + GRID_LINE_STEP_CM[i] + "cm = " + gridLinesMinimalScaleFactor[i]);
                }
            }
        } else {
            gridLinesMinimalScaleFactor = new float[GRID_LINE_STEP_INCH.length];
            int shortestGridSquareInPx = (int) (pixelsPerCm * SHORTEST_GRID_SQUARE_MM / 10f + 0.5);
            for (int i = 0; i < GRID_LINE_STEP_INCH.length; i++) {
                // store the ratio which will transform the boundary in cm to shortestGridSquareInPx
                float minFactor = 1f * shortestGridSquareInPx / (GRID_LINE_STEP_INCH[i] * Util.CM_IN_INCH);
                if (minFactor < minScaleFactorCmToPx) {
                    // we will never compare with something bigger than minScaleFactorCmToPx
                    minFactor = 0;
                }
                gridLinesMinimalScaleFactor[i] = minFactor;
                if (DEBUG) {
                    log.d("scalefactor limit for " + GRID_LINE_STEP_INCH[i] + "in = " + gridLinesMinimalScaleFactor[i]);
                }
            }
        }
    }

    private float getScaleFactorFor4cmOnScreen(int correspondsToRealMeters) {
        return getScaleFactorFor1cmOnScreen(correspondsToRealMeters / 4 * 100);
    }

    private float getScaleFactorFor1cmOnScreen(int correspondsToRealCm) {
        // we have 'pixelsPerCm' pixels in 1cm
        return pixelsPerCm / correspondsToRealCm;
    }

    private void fillGridLinesForScale(List<GridLine> gridLines, float scale) {
        // find the shown grid lines
        int lineIdx = 0;
        int i = 0;
        // optimization: find if the current set of shown lines is OK
        if (gridLines.size() > 0) {
            GridLine firstGridLine = gridLines.get(0);
            int idx = firstGridLine.idx;
            if (gridLinesMinimalScaleFactor[idx] <= scaleFactorCmToPx) {
                // condition fulfilled, check if the previous line fulfills it too
                if (idx == 0 || gridLinesMinimalScaleFactor[idx - 1] > scaleFactorCmToPx) {
                    // either there is no previous line or the previous line does not fulfill the condition
                    i = -1;
                } // else: check the whole line set again - from the beginning
            } else {
                // the algorithm may start from a different start idx
                i = firstGridLine.idx + 1;
            }
        }
        if (i != -1) {
            // compute
            Integer firstGridLineStep = null;
            Integer lastGridLineStep = null;
            for (; i < gridLinesMinimalScaleFactor.length; i++) {
                if (gridLinesMinimalScaleFactor[i] <= scaleFactorCmToPx) {
                    GridLine line;
                    int step;
                    if (lengthUnit == LengthUnit.METRIC) {
                        step = GRID_LINE_STEP_CM[i];
                    } else {
                        step = GRID_LINE_STEP_INCH[i];
                    }
                    if (firstGridLineStep != null) {
                        // check that this gridline step is multiplication of the finest gridline step
                        if ((step % firstGridLineStep) != 0) {
                            // skip to the next gridline, it wouldn't be visually pleasant
                            continue;
                        }
                        // check if this gridline step is 'far enough' from the previous one
                        if (lastGridLineStep * 3 >= step) {
                            // skip this one
                            continue;
                        }
                    } else {
                        firstGridLineStep = step;
                    }
                    if (gridLines.size() == lineIdx) {
                        // add one more line
                        line = new GridLine();
                        gridLines.add(line);
                    } else {
                        line = gridLines.get(lineIdx);
                    }
                    // set up the parameters
                    if (line.idx != i) {
                        line.idx = i;
                        line.stepInNaturalUnits = step;
                    }
                    lastGridLineStep = line.stepInNaturalUnits;
                    // check if we are done
                    if (++lineIdx >= GRID_LINES_MAX_LEVELS) {
                        break;
                    }
                }
            }
        }
        // recompute stepInPx
        for (GridLine line : shownGridLines) {
            float stepInCm = line.stepInNaturalUnits;
            if (lengthUnit == LengthUnit.IMPERIAL) {
                // inch -> cm
                stepInCm *= Util.CM_IN_INCH;
            }
            line.stepInPx = scale * stepInCm;
        }
    }

    private void logGridLinesForCurrentScale() {
        for (GridLine line : shownGridLines) {
            log.d("showing grid line: " + line);
        }
    }

    public void setNodeClickListener(Action1<NetworkNode> nodeClickListener) {
        this.nodeClickListener = nodeClickListener;
    }

}


