/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ext.NodeFactory;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.ui.view.GridView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment showing the preview of computed autopositioning positions.
 */
public class ApPreviewFragment extends AbstractArgoFragment {
    private static final String BK_SCALE = "SCALE";
    private static final String BK_FOCAL_POINT_X = "FOCAL_X";
    private static final String BK_FOCAL_POINT_Y = "FOCAL_Y";
    private static final String BK_EXTRA_ANIMATED_ZOOM = "ANIMATED_ZOOM";

    //
    @BindView(R.id.gridView)
    GridView grid;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    @Inject
    AutoPositioningManager autoPositioningManager;

    //
    private Float storedScale, storedFocalPointX, storedFocalPointY;
    private NetworkModel networkModel;
    private float extraAnimatedZoomFactor = 1f;

    public ApPreviewFragment() {
        super(FragmentType.AP_PREVIEW);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ap_preview, container, false);
        ButterKnife.bind(this, view);
        // check if we need to restore the state
        if (storedScale != null) {
            grid.setFocalPointAndScale(storedScale, new PointF(storedFocalPointX, storedFocalPointY), extraAnimatedZoomFactor);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // recycle the bitmap
        safeFloorPlanBitmapRecycle(networkNodeManager.getActiveNetwork());
    }

    private void safeFloorPlanBitmapRecycle(NetworkModel activeNetwork) {
        if (activeNetwork != null) {
            FloorPlan floorPlan = activeNetwork.getFloorPlan();
            if (floorPlan != null) {
                floorPlan.recycleBitmap();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(BK_SCALE, grid.getScaleFactorCmToPx());
        PointF focalPointInPx = grid.getFocalPointInPx();
        outState.putFloat(BK_FOCAL_POINT_X, focalPointInPx.x);
        outState.putFloat(BK_FOCAL_POINT_Y, focalPointInPx.y);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args;
        if (savedInstanceState != null && savedInstanceState.containsKey(BK_SCALE))  {
            storedScale = savedInstanceState.getFloat(BK_SCALE);
            storedFocalPointX = savedInstanceState.getFloat(BK_FOCAL_POINT_X);
            storedFocalPointY = savedInstanceState.getFloat(BK_FOCAL_POINT_Y);
            extraAnimatedZoomFactor = savedInstanceState.getFloat(BK_EXTRA_ANIMATED_ZOOM, 1f);
        } else if ((args = getArguments()) != null) {
            storedScale = args.getFloat(BK_SCALE);
            storedFocalPointX = args.getFloat(BK_FOCAL_POINT_X);
            storedFocalPointY = args.getFloat(BK_FOCAL_POINT_Y);
            extraAnimatedZoomFactor = args.getFloat(BK_EXTRA_ANIMATED_ZOOM, 1f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // set enabled status of floorplan menu item
        // inject fresh node set into the grid view
        configureGridView();
    }

    private void configureGridView() {
        List<NetworkNode> nodeList = Stream.of(autoPositioningManager.getNodes())
                .flatMap(node -> {
                    // this is filter & potential clone in one method
                    ComputedPosition computedPosition = autoPositioningManager.getComputedPosition(node.getId());
                    if (computedPosition != null && computedPosition.success) {
                        AnchorNode nodeCopy = NodeFactory.newNodeCopy(node);
                        nodeCopy.setPosition(computedPosition.position);
                        return Stream.of(nodeCopy);
                    } else {
                        // filter out
                        return Stream.of();
                    }
                })
                .collect(Collectors.toList());
        //noinspection ConstantConditions
        grid.setDependencies(nodeList,
                null,
                (nodeId) -> TrackMode.NOT_TRACKED,
                () -> networkNodeManager.getActiveNetwork().getFloorPlan(),
                () -> false,
                () -> true,
                () -> false,
                () -> false,
                (bleAddress) -> true,
                (bleAddress) -> false,
                null,
                appPreferenceAccessor.getLengthUnit()
        );
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

}
