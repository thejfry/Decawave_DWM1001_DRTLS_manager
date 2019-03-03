/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import com.decawave.argo.api.Util;

/**
 * Argo project.
 */
public class FirmwareMeta {
    public final String tag;
    public final int hardwareVersion;
    public final int firmwareVersion;
    public final int firmwareChecksum;
    public final int size;


    public FirmwareMeta(String tag, int hardwareVersion, int firmwareVersion, int firmwareChecksum, int size) {
        this.tag = tag;
        this.hardwareVersion = hardwareVersion;
        this.firmwareVersion = firmwareVersion;
        this.firmwareChecksum = firmwareChecksum;
        this.size = size;
    }

    @Override
    public String toString() {
        return "FirmwareMeta{" +
                "tag='" + tag + '\'' +
                ", hardwareVersion=" + hardwareVersion +
                ", firmwareVersion=" + firmwareVersion +
                ", firmwareChecksum=" + firmwareChecksum +
                ", size=" + size +
                '}';
    }
}
