/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.firmware;

import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argomanager.R;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Argo project.
 */
@SuppressWarnings("ALL")
public class FirmwareRepository {

    public static final Firmware FW1_A = new Firmware(R.raw.dwm_core_fw1_a, new FirmwareMeta("1.1.5", 0xdeca002a, 0x01010500, 0x4B6ED75F, 138592));
    public static final Firmware FW2_A = new Firmware(R.raw.dwm_core_fw2_a, new FirmwareMeta("1.1.5", 0xdeca002a, 0x01010501, 0x5C32B716, 193000));

    public static final Firmware FW1_B = new Firmware(R.raw.dwm_core_fw1_b, new FirmwareMeta("1.1.5-B", 0xdeca002a, 0x01010500, 0x0239C13A, 138592));
    public static final Firmware FW2_B = new Firmware(R.raw.dwm_core_fw2_b, new FirmwareMeta("1.1.5-B", 0xdeca002a, 0x01010501, 0xCB152D93, 193000));

    public static final Firmware[] DEFAULT_FIRMWARE = new Firmware[] { FW1_B, FW2_B };

    static {
        Preconditions.checkState(Objects.equal(DEFAULT_FIRMWARE[0].getMeta().tag, DEFAULT_FIRMWARE[1].getMeta().tag),
                "firmware tags do not match!");
    }

}
