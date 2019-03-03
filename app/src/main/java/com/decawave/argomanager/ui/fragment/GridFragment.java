/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.PositionObservationManager;
import com.decawave.argomanager.components.ih.IhPersistedNodeChangeListener;
import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreference;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.ApplicationMode;
import com.decawave.argomanager.prefs.IhAppPreferenceListener;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.view.FloorPlan;
import com.decawave.argomanager.ui.view.GridView;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.Fixme;
import com.decawave.argomanager.util.IhOnActivityResultListener;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import eu.kryl.android.common.hub.InterfaceHub;

/**
 * Fragment showing the network grid.
 */
public class GridFragment extends MainScreenFragment implements IhPresenceApiListener {
    private static final String BK_SCALE = "SCALE";
    private static final String BK_FOCAL_POINT_X = "FOCAL_X";
    private static final String BK_FOCAL_POINT_Y = "FOCAL_Y";
    private static final String BK_EXTRA_ANIMATED_ZOOM = "ANIMATED_ZOOM";

    // floorplan configuration
    private static final int REQUEST_CODE_PHOTO_PICKER_ID = 101;
    public static final String TMP_FLOORPLAN_FILENAME_SUFFIX = "_tmp";
    // 10 MB
    private static final int MAX_FLOORPLAN_FILE_SIZE = 1024 * 1024 * 10;

    //
    @BindView(R.id.noNetwork)
    View noNetworkSelected;

    @BindView(R.id.gridView)
    GridView grid;

    // ***************************
    // * INPUT
    // ***************************

    @BindView(R.id.floorplan_center_x)
    EditText etPxCenterX;

    @BindView(R.id.floorplan_center_y)
    EditText etPxCenterY;

    @BindView(R.id.floorplan_zoom_factor)
    EditText etPx10m;

    @BindView(R.id.floorplan_zoom_factor_hint)
    TextInputLayout tilZoom;

    @BindView(R.id.floorPlanEts)
    View etFloorplanProperties;

    @BindView(R.id.rootView)
    ViewGroup rootView;

    @BindView(R.id.floorPlanControls)
    ViewGroup floorPlanControls;

    @BindView(R.id.floorplan_control_lock)
    ImageView lockControl;

    @BindView(R.id.floorplan_control_erase)
    ImageView eraseControl;

    @BindView(R.id.floorplan_control_rotate_left)
    ImageView rotateLeftControl;

    ///////////////////////////////////////////////////////////////////////////
    // dependencies
    ///////////////////////////////////////////////////////////////////////////

    @Inject
    PositionObservationManager positionObservationManager;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    DiscoveryManager discoveryManager;

    @Inject
    AndroidPermissionHelper permissionHelper;

    @Inject
    BlePresenceApi presenceApi;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    // members/state
    private FloorPlanConfiguration floorPlanConfiguration;

    private static class FloorPlanConfiguration {
        //
        boolean floorPlanLocked;
        // state
        FloorPlan floorPlan;

        FloorPlanConfiguration(FloorPlan floorPlan) {
            this.floorPlan = floorPlan;
            this.floorPlanLocked = false;
        }

        boolean anyFloorPlan() {
            return floorPlan != null;
        }

        boolean toggleLock() {
            return (floorPlanLocked = !floorPlanLocked);
        }
    }

    //
    private Float storedScale, storedFocalPointX, storedFocalPointY;
    private NetworkModel networkModel;
    private float extraAnimatedZoomFactor = 1f;

    private IhAppPreferenceListener ihActiveNetworkPreferenceListener = (IhAppPreferenceListener) (element, oldValue, newValue) -> {
        if (element == AppPreference.Element.ACTIVE_NETWORK_ID) {
            NetworkModel prevNetworkModel = this.networkModel;
            networkModel = networkNodeManager.getActiveNetwork();
            safeFloorPlanBitmapRecycle(prevNetworkModel);
            configureGridView();
        }
    };

    private MenuItem loadFloorplanMenuItem;

    private IhPersistedNodeChangeListener nodeChangeListener = new IhPersistedNodeChangeListener() {

        @Override
        public void onNodeUpdated(NetworkNodeEnhanced node) {
            genericOnNodeChanged(node);
        }

        @Override
        public void onNodeUpdatedAndOrAddedToNetwork(short networkId, NetworkNodeEnhanced node) {
            genericOnNodeChanged(node);
        }

        @Override
        public void onNodeUpdatedAndRemovedFromNetwork(short networkId, long nodeId, boolean userInitiated) {
            genericOnNodeChanged(networkNodeManager.getNode(nodeId));
        }

        @Override
        public void onNodeForgotten(long nodeId, Short networkId, boolean userInitiated) {
            if (grid.isAwareOfNode(nodeId)) {
                grid.onNodeRemoved(nodeId);
            }
        }
    };

    public GridFragment() {
        super(FragmentType.GRID);
    }

    private ActionMode mActionMode;

    private static int getIconForLockedFloorPlan(boolean floorPlanLocked) {
        return floorPlanLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open;
    }

    //
    private IhOnActivityResultListener onActivityResultListener = new IhOnActivityResultListener() {
        @Override
        public void onActivityResult(MainActivity mainActivity, int requestCode, int resultCode, Intent data) {
            if (Constants.DEBUG) {
                log.d("onActivityResult: " + "mainActivity = [" + mainActivity + "], requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
                Preconditions.checkState(requestCode == REQUEST_CODE_PHOTO_PICKER_ID, "invalid request code: " + requestCode);
            }
            // TODO: do everything on worker handler
            Preconditions.checkNotNull(floorPlanConfiguration);
            // evaluate the result of request
            if (resultCode == Activity.RESULT_OK) {
                // set up the preview
                NetworkModel activeNetwork = networkNodeManager.getActiveNetworkNullSafe();
                // the data is there
                Uri uri = data.getData();
                InputStream inputStream = null;
                BufferedOutputStream bos = null;
                String tmpBitmapFilename = "floorplan_" + activeNetwork.getNetworkId() + TMP_FLOORPLAN_FILENAME_SUFFIX;
                try {
                    //noinspection ConstantConditions
                    inputStream = mainActivity.getContentResolver().openInputStream(uri);
                    // copy the stream elsewhere
                    File floorPlanFile = new File(ArgoApp.daApp.getFilesDir(), tmpBitmapFilename);
                    bos = new BufferedOutputStream(new FileOutputStream(floorPlanFile));
                    //noinspection ConstantConditions
                    int size = IOUtils.copy(inputStream, bos);
                    if (size > MAX_FLOORPLAN_FILE_SIZE) {
                        ToastUtil.showToast("Floorplan file size exceeds " + getMaxFloorplanSizeDesc() + "!", Toast.LENGTH_LONG);
                        resetFloorplanConfigurationIf(!floorPlanConfiguration.anyFloorPlan());
                        return;
                    }
                } catch (IOException e) {
                    log.w("cannot copy floorplan", e);
                    ToastUtil.showToast("Failed to copy floor plan!", Toast.LENGTH_LONG);
                    resetFloorplanConfigurationIf(!floorPlanConfiguration.anyFloorPlan());
                    return;
                } finally {
                    // close the streams
                    try {
                        if (inputStream != null) inputStream.close();
                        if (bos != null) bos.close();
                    } catch (IOException e) {
                        // ignoring
                        log.w("ignoring exception during close", e);
                    }
                }
                // there must be an active network
                Bitmap bitmap = FloorPlan.newBitmapFromFile(tmpBitmapFilename);
                Preconditions.checkNotNull(activeNetwork);
                Preconditions.checkNotNull(bitmap, "bitmap cannot be null, we have just copied the file!");
                // set up the floorplan properties - extract different properties from the bitmap
                float cmToPx = grid.getScaleFactorCmToPx();
                //  1 * cmToPx =  pixels in 1 cm
                // 10 * cmToPx =  pixels in 10 cm

                // TenMetersInPixels = 1000 cm in pixels
                // 1000 * cmToPx = TenMetersInPixels

                // let the bitmap fit into the width and height
                int pxWidth = grid.getWidth();
                int pxHeight = grid.getHeight();
                // how big is the virtual space which the screen is showing
                int cmWidth = (int) (pxWidth / cmToPx + 0.5);
                int cmHeight = (int) (pxHeight / cmToPx + 0.5);
                // what is the cmWidth/cmHeight in 10 meters scale
                float tenMetersWidth = cmWidth / 1000f;
                float tenMetersHeight = cmHeight / 1000f;
                //
                int bpPxWidth = bitmap.getWidth();
                int bpPxHeight = bitmap.getHeight();
                // we need to map it to the physical dimensions of the screen
                // the bpPxWidth must correspond to tenMetersWidth
                int tenMetersInPixelsScale = (int) (Math.max(bpPxWidth / tenMetersWidth, bpPxHeight / tenMetersHeight) + 0.5);
                // compute the shift of the central point
                PointF focalPointInPx = grid.getFocalPointInPx();
                // how much is it in the real units
                int xShiftRealCm = Math.round(focalPointInPx.x / cmToPx);
                int yShiftRealCm = Math.round(focalPointInPx.y / cmToPx);
                // now transform the shift to pixels (with respect to computed scale)
                int xShiftPixels = Math.round((xShiftRealCm / 1000f) * tenMetersInPixelsScale);
                int yShiftPixels = Math.round((yShiftRealCm / 1000f) * tenMetersInPixelsScale);
                // set up the floorplan configuration
                floorPlanConfiguration.floorPlan = new FloorPlan(tmpBitmapFilename, bpPxWidth / 2 - xShiftPixels, bpPxHeight / 2 - yShiftPixels, 0, tenMetersInPixelsScale);
                // if the actionbar is not in action mode, switch it now
                if (mActionMode == null) {
                    startFloorPlanConfigurationMode();
                }
                enableFloorPlanControls(true);
                grid.fetchFreshFloorPlanAndRedraw(false);
                fillFloorPlanEditProperties(floorPlanConfiguration.floorPlan);
                grid.setFloorPlanBound(floorPlanConfiguration.floorPlanLocked);
            } // else: result code is something else
            InterfaceHub.unregisterHandler(this);
        }

        @Override
        public void onRequestPermissionsResult(MainActivity mainActivity, int requestCode, String[] permissions, int[] grantResults) {
            // do nothing
            throw new Fixme("illegal state");
        }

    };

    boolean settingFpProperties = false;

    private void fillFloorPlanEditProperties(FloorPlan floorPlan) {
        Preconditions.checkState(floorPlan == floorPlanConfiguration.floorPlan,
                "floorplans do not match: floorPlan = " + floorPlan + ", floorPlanConfiguration.floorPlan = " + floorPlanConfiguration.floorPlan);
        Locale dl = Locale.getDefault();
        settingFpProperties = true;
        etPxCenterX.setText(String.format(dl, "%d", floorPlan.pxCenterX));
        etPxCenterY.setText(String.format(dl, "%d", floorPlan.pxCenterY));
        String px10mContent;
        if (appPreferenceAccessor.getLengthUnit() == LengthUnit.IMPERIAL) {
            // we need to recompute the zoom factor from pixels in 10 meters to pixels in 10 yards
            px10mContent = String.format(dl, "%d", Math.round(floorPlan.tenMetersInPixels * Util.MM_IN_YARD / Util.MM_IN_METER));
        } else {
            px10mContent = String.format(dl, "%d", floorPlan.tenMetersInPixels);
        }
        etPx10m.setText(px10mContent);
        settingFpProperties = false;
    }

    public void resetFloorplanConfigurationIf(boolean condition) {
        if (condition) {
            floorPlanConfiguration = null;
        }
    }

    @OnTextChanged(value = { R.id.floorplan_center_x, R.id.floorplan_center_y, R.id.floorplan_zoom_factor }, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void applyFloorPlanChange() {
        if (!settingFpProperties && floorPlanConfiguration != null && floorPlanConfiguration.floorPlan != null) {
            // apply the change
            applyFloorPlanEtChange();
        }
    }

    private void applyFloorPlanEtChange() {
        int zoomFactor = parsePx(etPx10m, 100);
        if (appPreferenceAccessor.getLengthUnit() == LengthUnit.IMPERIAL) {
            // we need to recompute the zoom factor from pixels in 10 yards to pixels in 10 meters
            zoomFactor = (int) (zoomFactor * (10000 / (Util.MM_IN_YARD * 10)) + 0.5);
        }
        floorPlanConfiguration.floorPlan.tenMetersInPixels = zoomFactor;
        floorPlanConfiguration.floorPlan.pxCenterX = parsePx(etPxCenterX, 0);
        floorPlanConfiguration.floorPlan.pxCenterY = parsePx(etPxCenterY, 0);
        // apply
        grid.fetchFreshFloorPlanAndRedraw(false);
        grid.fpMatrixToVirtualFpMatrix();   // we need to have these two matched
    }

    int parsePx(EditText pxInput, int defaultValue) {
        String text = pxInput.getText().toString();
        if (text.length() == 0) {
            return defaultValue;
        } // else:
        return Integer.valueOf(text);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_grid, menu);
        configureBasicMenuItems(menu);
        //
        configureInstructionsMenuItem(menu);
        // floor plan
        loadFloorplanMenuItem = menu.findItem(R.id.action_floorplan);
        loadFloorplanMenuItem.setEnabled(networkNodeManager.getActiveNetwork() != null);
        loadFloorplanMenuItem.setOnMenuItemClickListener((m) -> {
            // create floorplan configuration
            FloorPlan currFloorPlan = networkNodeManager.getActiveNetworkNullSafe().getFloorPlan();
            // we have to make a copy of the floorplan, otherwise we are modifying floorplan associated with
            // the network directly, which then evaluates as no-change and the changes are not saved/lost
            floorPlanConfiguration = new FloorPlanConfiguration(FloorPlan.copyNullSafe(currFloorPlan));
            if (floorPlanConfiguration.anyFloorPlan()) {
                startFloorPlanConfigurationMode();
                fillFloorPlanEditProperties(floorPlanConfiguration.floorPlan);
            } else {
                // there is no previous floor plan, launch directly image picker
                launchImagePicker();
            }
            return true;
        });
        // show grid
        MenuItem showGridInfoMenuItem = menu.findItem(R.id.action_show_grid);
        showGridInfoMenuItem.setOnMenuItemClickListener((m) -> {
            boolean b = !appPreferenceAccessor.getShowGrid();
            appPreferenceAccessor.setShowGrid(b);
            m.setChecked(b);
            grid.invalidate();
            return true;
        });
        showGridInfoMenuItem.setChecked(appPreferenceAccessor.getShowGrid());
        // show average
        MenuItem showAverageInfoMenuItem = menu.findItem(R.id.action_show_average);
        showAverageInfoMenuItem.setOnMenuItemClickListener((m) -> {
            boolean b = !appPreferenceAccessor.getShowAverage();
            appPreferenceAccessor.setShowAverage(b);
            m.setChecked(b);
            return true;
        });
        showAverageInfoMenuItem.setChecked(appPreferenceAccessor.getShowAverage());
        // show debug info
        if (appPreferenceAccessor.getApplicationMode() == ApplicationMode.SIMPLE) {
            // we will remove the show debug info menu item
            menu.removeItem(R.id.action_show_debug_info);
        } else {
            MenuItem showDebugInfoMenuItem = menu.findItem(R.id.action_show_debug_info);
            showDebugInfoMenuItem.setOnMenuItemClickListener((m) -> {
                boolean b = !appPreferenceAccessor.getShowGridDebugInfo();
                appPreferenceAccessor.setShowGridDebugInfo(b);
                m.setChecked(b);
                grid.invalidate();
                return true;
            });
            showDebugInfoMenuItem.setChecked(appPreferenceAccessor.getShowGridDebugInfo());
        }
    }

    private void startFloorPlanConfigurationMode() {
        mActionMode = getMainActivity().startActionMode(new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_floorplan, menu);
                // configure the lock control appearance
                lockControl.setImageDrawable(getContext().getDrawable(getIconForLockedFloorPlan(floorPlanConfiguration.floorPlanLocked)));
                switchUiToFloorPlanControlMode(true);
                // we have a new floorplan (either completely new or copy of the previous floorplan instance)
                grid.fetchFreshFloorPlanAndRedraw(false);
                grid.setFloorPlanBound(floorPlanConfiguration.floorPlanLocked);
                // enable the EditText's
                enableFloorPlanControls(true);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_save:
                        // save (or reset) the floorplan
                        // NPE bugfix
                        if (floorPlanConfiguration != null) {
                            if (floorPlanConfiguration.floorPlan != null) {
                                // rename the floorplan file first - if it's not renamed yet
                                String fileName = floorPlanConfiguration.floorPlan.getFloorPlanFileName();
                                if (fileName.endsWith(TMP_FLOORPLAN_FILENAME_SUFFIX)) {
                                    File filesDir = ArgoApp.daApp.getFilesDir();
                                    File tmpFile = new File(filesDir, fileName);
                                    String newFloorPlanFileName = fileName.substring(0, fileName.length() - TMP_FLOORPLAN_FILENAME_SUFFIX.length());
                                    if (tmpFile.renameTo(new File(filesDir, newFloorPlanFileName))) {
                                        FloorPlan oldFp = floorPlanConfiguration.floorPlan;
                                        floorPlanConfiguration.floorPlan = new FloorPlan(newFloorPlanFileName, oldFp.pxCenterX, oldFp.pxCenterY, oldFp.rotation, oldFp.tenMetersInPixels);
                                    } // else: rename did not succeed, ok keep the temporary name (maybe it will be fixed the next time)
                                }
                            }
                            networkNodeManager.getActiveNetworkNullSafe().setFloorPlan(floorPlanConfiguration.floorPlan);
                            // exit the action mode
                            actionMode.finish();
                            return true;
                        } // else: ignore the touch
                        return false;
                    case R.id.action_load:
                        launchImagePicker();
                        return true;
                    default:
                        return false;

                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                switchUiToFloorPlanControlMode(false);
                // reset action mode
                mActionMode = null;
                // we will also reset the working FP configuration
                floorPlanConfiguration = null;
                //
                grid.fetchFreshFloorPlanAndRedraw(false);
                grid.setFloorPlanBound(true);
            }
        });
    }

    private void switchUiToFloorPlanControlMode(boolean enable) {
        TransitionSet tSet = new TransitionSet();
        Slide slideBottomTransition = new Slide(Gravity.BOTTOM);
        slideBottomTransition.addTarget(etFloorplanProperties);
        Slide slideRightTransition = new Slide(Gravity.END);
        slideRightTransition.addTarget(floorPlanControls);
        tSet.addTransition(slideBottomTransition);
        tSet.addTransition(slideRightTransition);
        TransitionManager.beginDelayedTransition(rootView, tSet);
        etFloorplanProperties.setVisibility(enable ? View.VISIBLE : View.GONE);
        floorPlanControls.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grid, container, false);
        ButterKnife.bind(this, view);
        //
        Util.configureNoNetworkScreen(view, permissionHelper, getMainActivity());
        // check if we need to restore the state
        if (storedScale != null) {
            grid.setFocalPointAndScale(storedScale, new PointF(storedFocalPointX, storedFocalPointY), extraAnimatedZoomFactor);
        }
        grid.setNodeClickListener(this::onNetworkNodeSelected);
        // set listeners of the control
        eraseControl.setOnClickListener((v) -> {
            floorPlanConfiguration.floorPlan = null;
            grid.fetchFreshFloorPlanAndRedraw(false);
            enableFloorPlanControls(false);
        });
        rotateLeftControl.setOnClickListener((v) -> {
            if (floorPlanConfiguration.floorPlan != null) {
                // rotate left
                floorPlanConfiguration.floorPlan.rotation = (floorPlanConfiguration.floorPlan.rotation - 90) % 360;
                grid.fetchFreshFloorPlanAndRedraw(true);
            }
        });
        lockControl.setOnClickListener((v) -> {
            if (floorPlanConfiguration.floorPlan != null) {
                // switch the mode & icon
                boolean locked = floorPlanConfiguration.toggleLock();
                lockControl.setImageDrawable(getContext().getDrawable(getIconForLockedFloorPlan(locked)));
                grid.setFloorPlanBound(floorPlanConfiguration.floorPlanLocked);
            }
        });
        //
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

    private void onNetworkNodeSelected(NetworkNode networkNode) {
        if (floorPlanConfiguration == null) {
            if (Constants.DEBUG) {
                log.d("onNetworkNodeSelected: " + "networkNode = [" + networkNode + "]");
            }
            getMainActivity().showFragment(FragmentType.OVERVIEW, OverviewFragment.getBundleForExpandedNode(networkNode.getId()));
        } // else: we are currently in the process of floorplan configuration, ignoring click
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

    public static Bundle getArgsForPosition(Position position) {
        Bundle b = new Bundle();
        float factor = 0.1f;
        b.putFloat(BK_SCALE, factor);
        b.putFloat(BK_FOCAL_POINT_X, Math.round(factor * position.x / 10));
        b.putFloat(BK_FOCAL_POINT_Y, -Math.round(factor * position.y / 10));
            // we've got inverted y-axis
        b.putFloat(BK_EXTRA_ANIMATED_ZOOM, 20);
        return b;
    }

    @Override
    public void onResume() {
        super.onResume();
        // set enabled status of floorplan menu item
        NetworkModel activeNetwork = networkNodeManager.getActiveNetwork();
        if (activeNetwork != null) {
            permissionHelper.mkSureServicesEnabledAndPermissionsGranted(getMainActivity(), this::mkSurePositionObservationRunning);
        }
        if (loadFloorplanMenuItem != null) {
            loadFloorplanMenuItem.setEnabled(activeNetwork != null);
        }
        // inject fresh node set into the grid view
        configureGridView();
        // adjust scale hint
        LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit();
        tilZoom.setHint(daApp.getString(lengthUnit == LengthUnit.METRIC ? R.string.floorplan_zoom_factor_meters : R.string.floorplan_zoom_factor_yards));
        // register handlers
        InterfaceHub.registerHandler(nodeChangeListener);
        InterfaceHub.registerHandler(ihActiveNetworkPreferenceListener);
    }

    private void configureGridView() {
        if (networkNodeManager.getActiveNetwork() != null) {
            grid.setDependencies(Stream.of(networkNodeManager.getActiveNetworkNodes())
                            // filter
                            .filter(GridFragment::filter)
                            // transform to a plain node
                            .map(NetworkNodeEnhanced::asPlainNode)
                            .collect(Collectors.toList()),
                    (nodeId) -> {
                        // lookup by short
                        NetworkNodeEnhanced node = networkNodeManager.getNodeByShortId(nodeId);
                        if (node != null) {
                            return node.asPlainNode();
                        } else {
                            return null;
                        }
                    },
                    (nodeId) -> networkNodeManager.getNodeTrackMode(nodeId),
                    () -> floorPlanConfiguration != null ? floorPlanConfiguration.floorPlan : networkNodeManager.getActiveNetwork().getFloorPlan(),
                    () -> appPreferenceAccessor.getShowGridDebugInfo() && appPreferenceAccessor.getApplicationMode() == ApplicationMode.ADVANCED,
                    () -> appPreferenceAccessor.getShowGrid(),
                    () -> appPreferenceAccessor.getShowAverage(),
                    () -> appPreferenceAccessor.getApplicationMode() == ApplicationMode.ADVANCED,
                    presenceApi::isNodePresent,
                    this::drawTag,
                    this::fillFloorPlanEditProperties,
                    appPreferenceAccessor.getLengthUnit());
        } else {
            grid.setDependencies(Collections.emptyList(), null, null, null,
                    () -> appPreferenceAccessor.getShowGridDebugInfo(),
                    () -> appPreferenceAccessor.getShowGrid(),
                    () -> appPreferenceAccessor.getShowAverage(),
                    () -> false,
                    (bleAddress) -> false,
                    (bleAddress) -> false,
                    null,
                    appPreferenceAccessor.getLengthUnit()
                    );
        }
        adjustViewsVisibility();
    }

    public static boolean filter(NetworkNodeEnhanced nne) {
        NetworkNode plainNode = nne.asPlainNode();
        return isNodeTracked(plainNode.getType(), plainNode.extractPositionDirect(), nne.getTrackMode(), plainNode.getUwbMode());
    }

    private boolean drawTag(String bleAddress) {
        // node is tag, presence API result is not sufficient, the tag needs to be tracked
        return presenceApi.isTagTrackedDirectly(bleAddress) || presenceApi.isTagTrackedViaProxy(bleAddress);
    }

    private void adjustViewsVisibility() {
        if (networkNodeManager.getActiveNetwork() != null) {
            //
            grid.setVisibility(View.VISIBLE);
            noNetworkSelected.setVisibility(View.GONE);
        } else {
            noNetworkSelected.setVisibility(View.VISIBLE);
            grid.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        InterfaceHub.unregisterHandler(nodeChangeListener);
        InterfaceHub.unregisterHandler(ihActiveNetworkPreferenceListener);
        if (positionObservationManager.isObservingPosition()) {
            // schedule stop position observation
            positionObservationManager.schedulePositionObservationStop(2000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void mkSurePositionObservationRunning() {
        if (Constants.DEBUG) {
            log.d("startPositionObservation()");
            Preconditions.checkState(permissionHelper.allSetUp());
        }
        if (positionObservationManager.isObservingPosition()) {
            // cancel possible stop
            positionObservationManager.cancelScheduledPositionObservationStop();
        } else {
            // start observing
            positionObservationManager.startPositionObservation();
        }
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }


    @Override
    public void onNodePresent(String nodeBleAddress) {
        // redraw
        grid.invalidate();
    }

    @Override
    public void onTagDirectObserve(String bleAddress, boolean observe) {
        // redraw
        grid.invalidate();
    }

    @Override
    public void onNodeMissing(String nodeBleAddress) {
        // redraw
        grid.invalidate();
    }

    @Override
    public void onNodeRssiChanged(String bleAddress, int rssi) {
        // do nothing
    }

    private void genericOnNodeChanged(NetworkNodeEnhanced node) {
        boolean isAwareOf = grid.isAwareOfNode(node.getId());
        boolean shouldBeAwareOf = networkNodeManager.isInActiveNetwork(node) && filter(node);
        // process the change
        if (isAwareOf && !shouldBeAwareOf) {
            grid.onNodeRemoved(node.getId());
        } else if (isAwareOf) {
            //  && shouldBeAwareOfNode
            grid.onNodeChanged(node.asPlainNode());
        } else if (shouldBeAwareOf) {
            //  && !awareOfNode
            grid.onNodeAdded(node.asPlainNode());
        } // else: !awareOfNode && !shouldBeAwareOfNode
    }

    private static String getMaxFloorplanSizeDesc() {
        return daApp.getString(R.string.floorplan_mb, String.format(Locale.US, "%.1f", MAX_FLOORPLAN_FILE_SIZE / (1024f * 1024)));
    }

    private void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        getMainActivity().startActivityForResult(Intent.createChooser(intent,
                "Complete action using"), REQUEST_CODE_PHOTO_PICKER_ID);
        // register as IH listener to get the result
        InterfaceHub.registerHandler(onActivityResultListener);
    }

    private void enableFloorPlanControls(boolean enable) {
        TransitionManager.beginDelayedTransition(rootView, new Slide(Gravity.END));
        floorPlanControls.setVisibility(enable ? View.VISIBLE : View.GONE);
        etPx10m.setEnabled(enable);
        etPxCenterX.setEnabled(enable);
        etPxCenterY.setEnabled(enable);
        if (!enable) {
            // bind the floorplan
            grid.setFloorPlanBound(true);
            settingFpProperties = true;
            etPxCenterX.setText("");
            etPxCenterY.setText("");
            etPx10m.setText("");
            settingFpProperties = false;
        }
    }

    public static boolean isNodeTracked(NodeType nodeType,
                                        Position position,
                                        TrackMode trackMode,
                                        UwbMode uwbMode) {
        return position != null
                // active
                && uwbMode == UwbMode.ACTIVE
                && (
                    // tracked tags
                    (nodeType == NodeType.TAG && trackMode.tracked)
                    ||
                    // anchors
                    (nodeType == NodeType.ANCHOR));
    }
}
