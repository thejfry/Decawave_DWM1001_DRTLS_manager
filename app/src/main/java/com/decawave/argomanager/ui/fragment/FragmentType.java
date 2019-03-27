/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.ioc.IocContext;
import com.decawave.argomanager.util.AndroidPermissionHelper;

import rx.functions.Action1;
import rx.functions.Func0;

/**
 * Distinct fragment types.
 */
public enum FragmentType {
    OVERVIEW(OverviewFragment::new),
    GRID(GridFragment::new, (newFragmentType) -> {
        // onExit
        if (newFragmentType == OVERVIEW) {
            IocContext.daCtx.getBleConnectionApi().limitLowPriorityConnections(-1);
        }
    }, (oldFragmentType) -> {
        // onEnter
        if (oldFragmentType == null || oldFragmentType == OVERVIEW) {
            IocContext.daCtx.getBleConnectionApi().limitLowPriorityConnections(2);
        }
    }),
    SETTINGS(R.string.screen_title_settings, SettingsFragment::new),
    DEBUG_LOG(R.string.screen_title_debug_log, DebugLogBufferFragment::new),
    DEVICE_DEBUG_CONSOLE(R.string.screen_title_debug_console, DeviceDebugConsoleFragment::new),
    POSITION_LOG(R.string.screen_title_position_log, PositionLogBufferFragment::new),
    DISCOVERY(R.string.screen_title_discovery, DiscoveryFragment::new, false, (newFragmentType) -> {
        // onExit
        if (newFragmentType.mainScreen) {
            // we are returning to the main screen, reset and stop the discovery
            DiscoveryManager discoveryManager = IocContext.daCtx.getDiscoveryManager();
            discoveryManager.stopDiscoveryIfRunning();
            if (DiscoveryFragment.networkAssignmentRunner != null) {
                if (!DiscoveryFragment.networkAssignmentRunner.getOverallStatus().terminal) {
                    // terminate the runner
                    DiscoveryFragment.networkAssignmentRunner.terminate();
                }
                // and forget the reference
                DiscoveryFragment.networkAssignmentRunner = null;
            }
        }
    }, (oldFragmentType) -> {
        // onEnter
        if (oldFragmentType != null && oldFragmentType.mainScreen) {
            AndroidPermissionHelper permissionHelper = IocContext.daCtx.getPermissionHelper();
            if (permissionHelper.allSetUp()) {
                DiscoveryManager discoveryManager = IocContext.daCtx.getDiscoveryManager();
                discoveryManager.startTimeLimitedDiscovery(true);
            }
        }
    }),
    NODE_DETAILS(R.string.screen_title_node_details, NodeDetailFragment::new, true, (newFragmentType) -> NodeDetailFragment.sInputNode = null),
    DEVICE_ERRORS(R.string.screen_title_problematic_nodes, DeviceErrorFragment::new),
    AUTO_POSITIONING(R.string.screen_title_auto_positioning, AutoPositioningFragment::new, false, (newFragmentType) -> {
        if (newFragmentType.mainScreen) {
            // we are returning to the main screen, terminate the manager
            AutoPositioningManager autoPositioningManager = IocContext.daCtx.getAutoPositioningManager();
            if (!autoPositioningManager.getApplicationState().idle) autoPositioningManager.terminate();
        }
    }),
    FIRMWARE_UPDATE(R.string.screen_title_firmware_update, FirmwareUpdateFragment::new, false, (newFragmentType) -> {
        if (newFragmentType.mainScreen) {
            if (FirmwareUpdateFragment.firmwareUpdateRunner != null && FirmwareUpdateFragment.firmwareUpdateRunner.getOverallStatus().terminal) {
                // we are returning to the main screen, and the runner is finished, reset the runner
                FirmwareUpdateFragment.firmwareUpdateRunner = null;
            }
        }
    }),
    AP_PREVIEW(R.string.ap_preview, ApPreviewFragment::new),
    INSTRUCTIONS(R.string.screen_title_instructions, InstructionsFragment::new),
    ABOUT_US(R.string.about_tab, AboutUsTab::new),
    SELECT_ANCHOR(R.string.screen_title_select_anchor, SelectAnchorsFragment::new);

    public final boolean mainScreen;
    public final boolean hasScreenTitle;
    public final boolean fullScreenDialog;
    public final int screenTitleId;
    public final Action1<FragmentType> onFragmentLeft;
    public final Action1<FragmentType> onFragmentEntered;

    private final Func0<AbstractArgoFragment> factory;

    FragmentType(Func0<AbstractArgoFragment> instanceFactory) {
        this(instanceFactory, null, null);
    }

    FragmentType(Func0<AbstractArgoFragment> instanceFactory,
                 Action1<FragmentType> onFragmentLeft,
                 Action1<FragmentType> onFragmentEntered) {
        this.mainScreen = true;
        this.fullScreenDialog = false;
        this.hasScreenTitle = false;
        this.factory = instanceFactory;
        this.screenTitleId = -1;
        this.onFragmentLeft = onFragmentLeft;
        this.onFragmentEntered = onFragmentEntered;
    }

    FragmentType(int screenTitleId, Func0<AbstractArgoFragment> instanceFactory) {
        this(screenTitleId, instanceFactory, false, null);
    }

    FragmentType(int screenTitleId, Func0<AbstractArgoFragment> instanceFactory, boolean fullscreenDialog, Action1<FragmentType> onFragmentLeft) {
        this(screenTitleId, instanceFactory, fullscreenDialog, onFragmentLeft, null);
    }

    FragmentType(int screenTitleId,
                 Func0<AbstractArgoFragment> instanceFactory,
                 boolean fullscreenDialog,
                 Action1<FragmentType> onFragmentLeft,
                 Action1<FragmentType> onFragmentEntered) {
        this.mainScreen = false;
        this.hasScreenTitle = true;
        this.fullScreenDialog = fullscreenDialog;
        this.screenTitleId = screenTitleId;
        this.factory = instanceFactory;
        this.onFragmentLeft = onFragmentLeft;
        this.onFragmentEntered = onFragmentEntered;
    }

    public AbstractArgoFragment newInstance() {
        return factory.call();
    }

}