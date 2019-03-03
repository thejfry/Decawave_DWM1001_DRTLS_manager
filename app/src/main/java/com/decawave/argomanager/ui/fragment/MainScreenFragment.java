/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.view.Menu;
import android.view.MenuItem;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.NetworkNodeManager;

import javax.inject.Inject;

/**
 * Common predecessor for main screens.
 */
public abstract class MainScreenFragment extends DiscoveryProgressAwareFragment {

    @Inject
    NetworkNodeManager networkNodeManager;

    // state
    protected MenuItem instructionsItem;

    public MainScreenFragment(FragmentType fragmentType) {
        super(fragmentType);
    }

    protected void configureInstructionsMenuItem(Menu menu) {
        instructionsItem = menu.findItem(R.id.action_instructions);
        instructionsItem.setVisible(networkNodeManager.getActiveNetwork() != null && !appPreferenceAccessor.isInstructionsRead());
        instructionsItem.setOnMenuItemClickListener(item -> {
            getMainActivity().showFragment(FragmentType.INSTRUCTIONS);
            return true;
        });
    }

}
