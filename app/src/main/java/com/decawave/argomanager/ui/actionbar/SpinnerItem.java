/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.actionbar;

import com.decawave.argomanager.ui.fragment.FragmentType;

/**
 * Argo project.
 */

public interface SpinnerItem {

    FragmentType getFragmentType();

    int getTitleResId();

}
