/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.error;

import org.jetbrains.annotations.NotNull;

import eu.kryl.android.common.hub.InterfaceHubHandler;

/**
 * Argo project.
 */

public interface IhErrorManagerListener extends InterfaceHubHandler {

    void onErrorDetailAdded(@NotNull String deviceBleAddress, @NotNull ErrorDetail errorDetail);

    void onErrorRemoved(@NotNull String deviceBleAddress);

    void onErrorsClear();

}
