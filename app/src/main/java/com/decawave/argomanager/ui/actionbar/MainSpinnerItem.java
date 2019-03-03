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
public enum MainSpinnerItem implements SpinnerItem {
    LIST(R.string.ab_overview, FragmentType.OVERVIEW),
    GRID(R.string.ab_grid, FragmentType.GRID);

    private final int titleResId;
    private final FragmentType fragmentType;

    MainSpinnerItem(int titleResId, FragmentType fragmentType) {
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
