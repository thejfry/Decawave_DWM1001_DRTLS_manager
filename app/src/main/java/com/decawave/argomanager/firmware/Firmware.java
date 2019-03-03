/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.firmware;

import com.decawave.argo.api.struct.FirmwareMeta;

import java.io.InputStream;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Argo project.
 */
public class Firmware {
    private final int resourceId;
    private final FirmwareMeta meta;

    Firmware(int resourceId, FirmwareMeta meta) {
        this.resourceId = resourceId;
        this.meta = meta;
    }

    public InputStream getByteStream() {
        return daApp.getResources().openRawResource(resourceId);
    }

    public FirmwareMeta getMeta() {
        return meta;
    }

}
