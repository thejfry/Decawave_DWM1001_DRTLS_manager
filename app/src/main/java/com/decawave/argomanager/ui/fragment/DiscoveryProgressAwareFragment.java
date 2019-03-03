/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.IhDiscoveryStateListener;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.error.ErrorDetail;
import com.decawave.argomanager.error.IhErrorManagerListener;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

/**
 * Common predecessor for main screens.
 */
public abstract class DiscoveryProgressAwareFragment extends AbstractArgoFragment implements IhDiscoveryStateListener, IhErrorManagerListener {

    @Nullable
    protected MenuItem progressMenuItem;
    protected MenuItem showErrorsMenuItem;

    protected boolean progressMenuItemDisplayed;

    @Inject
    DiscoveryApi discoveryApi;

    @Inject
    ErrorManager errorManager;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    public DiscoveryProgressAwareFragment(FragmentType fragmentType) {
        super(fragmentType);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        configureBasicMenuItems(menu);
    }

    protected void configureBasicMenuItems(Menu menu) {
        showErrorsMenuItem = menu.findItem(R.id.action_show_errors);
        showErrorsMenuItem.setOnMenuItemClickListener((v) -> {
            getMainActivity().showFragment(FragmentType.DEVICE_ERRORS);
            return true;
        });
        // progress menu item
        progressMenuItem = menu.findItem(R.id.action_progress);
        // fixing bug with stale cached state
        progressMenuItemDisplayed = false;
        uiSetMenuItemsVisibility();
    }

    @Override
    public void onCreate(@android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Update progress menu item state
     */
    protected void uiSetMenuItemsVisibility() {
        if (progressMenuItem == null) return;

        boolean showProgress = showProgress();

        if (showProgress) {
            // show progress menu item (but do not refresh animation if already running)
            if (!progressMenuItemDisplayed) {
                progressMenuItem.setVisible(true);
                MenuItemCompat.setActionView(progressMenuItem, R.layout.actionbar_indeterminate_progress);
                progressMenuItemDisplayed = true;
            }
        } else {
            // hide progress menu item
            progressMenuItemDisplayed = false;
            progressMenuItem.setVisible(false);
        }
        if (showErrorsMenuItem != null) {
            showErrorsMenuItem.setVisible(errorManager.anyUnreadError());
        }
    }

    protected boolean showProgress() {
        return discoveryApi.isDiscovering();
    }

    @Override
    public void onResume() {
        super.onResume();
        uiSetMenuItemsVisibility();
    }

    @Override
    public final void afterDiscoveryStarted() {
        uiSetMenuItemsVisibility();
        onAfterDiscoveryStarted();
    }

    protected void onAfterDiscoveryStarted() {

    }

    @Override
    public final void afterDiscoveryStopped() {
        uiSetMenuItemsVisibility();
        onAfterDiscoveryStopped();
    }

    protected void onAfterDiscoveryStopped() {

    }

    @Override
    public void onErrorDetailAdded(@NonNull String deviceBleAddress, @NonNull ErrorDetail errorDetail) {
        uiSetMenuItemsVisibility();
    }

    @Override
    public void onErrorRemoved(@NonNull String deviceBleAddress) {
        uiSetMenuItemsVisibility();
    }

    @Override
    public void onErrorsClear() {
        uiSetMenuItemsVisibility();
    }

}
