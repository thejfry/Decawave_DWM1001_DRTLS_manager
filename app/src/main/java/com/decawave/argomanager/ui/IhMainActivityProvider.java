/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui;

import eu.kryl.android.common.hub.SingletonHandler;
import rx.functions.Action1;

/**
 * Argo project.
 */

public interface IhMainActivityProvider extends SingletonHandler {

    void provideMainActivity(Action1<MainActivity> actionOnMainActivity);

}
