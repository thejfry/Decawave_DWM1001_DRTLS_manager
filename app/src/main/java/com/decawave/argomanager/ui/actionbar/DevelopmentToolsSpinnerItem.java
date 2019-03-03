/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.actionbar;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.fragment.FragmentType;

/**
 * Argo project.
 */
public enum DevelopmentToolsSpinnerItem implements SpinnerItem {
    BLE(R.string.ab_ble_log, FragmentType.DEBUG_LOG),
    ERRORS_WARNINGS(R.string.ab_errors_warnings, FragmentType.DEVICE_ERRORS);

    private final int titleResId;
    private final FragmentType fragmentType;

    DevelopmentToolsSpinnerItem(int titleResId, FragmentType fragmentType) {
        this.titleResId = titleResId;
        this.fragmentType = fragmentType;
    }

    @Override
    public FragmentType getFragmentType() {
        return fragmentType;
    }

    @Override
    public int getTitleResId() {
        return titleResId;
    }
}
