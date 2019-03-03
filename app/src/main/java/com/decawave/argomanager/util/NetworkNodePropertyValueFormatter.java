/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import com.annimon.stream.function.FunctionalInterface;

/**
 * Argo project.
 */
@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface NetworkNodePropertyValueFormatter<T> {

    String format(T value);
}
