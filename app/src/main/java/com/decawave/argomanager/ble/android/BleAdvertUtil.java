/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ble.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Various BLE utility methods.
 */

class BleAdvertUtil {

    static List<AdRecord> parseAdRecord(byte[] scanRecord) {
        List<AdRecord> records = new ArrayList<>(2);

        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            int type = scanRecord[index];
            //Done if our record isn't a valid type
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.add(new AdRecord(length, type, data));
            //Advance
            index += length;
        }
        return records;
    }

    static class AdRecord {
        int length;
        int type;
        byte[] data;

        AdRecord(int length, int type, byte[] data) {
            this.length = length;
            this.type = type;
            this.data = data;
        }

    }
}
