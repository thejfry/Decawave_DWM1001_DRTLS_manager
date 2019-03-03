/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ioc;

import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.impl.UniqueReorderingStack;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment;
import com.decawave.argomanager.ui.dialog.RenameNetworkDialogFragment;
import com.decawave.argomanager.ui.dialog.TurnOnLocationServiceDialogFragment;
import com.decawave.argomanager.ui.dialog.ZaxisValueDialogFragment;
import com.decawave.argomanager.ui.fragment.ApPreviewFragment;
import com.decawave.argomanager.ui.fragment.AutoPositioningFragment;
import com.decawave.argomanager.ui.fragment.DebugLogBufferFragment;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment;
import com.decawave.argomanager.ui.fragment.DeviceErrorFragment;
import com.decawave.argomanager.ui.fragment.DiscoveryFragment;
import com.decawave.argomanager.ui.fragment.FirmwareUpdateFragment;
import com.decawave.argomanager.ui.fragment.GridFragment;
import com.decawave.argomanager.ui.fragment.InstructionsFragment;
import com.decawave.argomanager.ui.fragment.LogBufferFragment;
import com.decawave.argomanager.ui.fragment.NodeDetailFragment;
import com.decawave.argomanager.ui.fragment.OverviewFragment;
import com.decawave.argomanager.ui.fragment.SettingsFragment;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.gatt.GattDecoderCache;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Argo project.
 *
 * Injection methods for application components.
 */
@Component(modules = { ArgoDependencyBindingsModule.class, ArgoDependencyProvider.class } )
@Singleton
public interface ArgoComponent {

    void inject(ArgoApp argoApp);

    void inject(MainActivity mainActivity);

    void inject(DiscoveryFragment fragment);

    void inject(NodeDetailFragment fragment);

    void inject(DebugLogBufferFragment fragment);

    void inject(GridFragment fragment);

    void inject(OverviewFragment fragment);

    void inject(NetworkPickerDialogFragment networkPickerDialogFragment);

    void inject(RenameNetworkDialogFragment renameNetworkDialogFragment);

    void inject(DeviceErrorFragment deviceErrorFragment);

    void inject(TurnOnLocationServiceDialogFragment fragment);

    void inject(LogBufferFragment logBufferFragment);

    void inject(DeviceDebugConsoleFragment deviceDebugConsoleFragment);

    void inject(ApplicationComponentLog applicationComponentLog);

    void inject(FirmwareUpdateFragment firmwareUpdateFragment);

    void inject(AutoPositioningFragment autoPositioningFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(ZaxisValueDialogFragment zaxisValueDialogFragment);

    void inject(ApPreviewFragment apPreviewFragment);

    void inject(InstructionsFragment instructionsFragment);

    UniqueReorderingStack<Short> getActiveNetworkIdStack();

    BleConnectionApi getBleConnectionApi();

    DiscoveryManager getDiscoveryManager();

    AndroidPermissionHelper getPermissionHelper();

    AutoPositioningManager getAutoPositioningManager();

    NetworkNodeManager getNetworkNodeManager();

    GattDecoderCache getGattDecoderCache();

}
