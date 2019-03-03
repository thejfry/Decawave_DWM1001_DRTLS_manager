/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import android.support.annotation.NonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

/**
 * Argo project.
 */
public class LogEntryTagFactory {

    private static LoadingCache<String, LogEntryTag> deviceTagCache = CacheBuilder.newBuilder().build(
            new CacheLoader<String, LogEntryTag>() {
                @Override
                public LogEntryTag load(@NonNull String deviceBleAddress) throws Exception {
                    return new LogEntryDeviceTag(deviceBleAddress);
                }
            }
    );

    private static LoadingCache<String, LogEntryTag> componentTagCache = CacheBuilder.newBuilder().build(
            new CacheLoader<String, LogEntryTag>() {
                @Override
                public LogEntryTag load(@NonNull String componentName) throws Exception {
                    return new LogEntryComponentTag(componentName);
                }
            }
    );

    public static LogEntryTag getDeviceLogEntryTag(String deviceBleAddress) {
        try {
            return deviceTagCache.get(deviceBleAddress);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static LogEntryTag getComponentLogEntryTag(String componentName) {
        try {
            return componentTagCache.get(componentName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
