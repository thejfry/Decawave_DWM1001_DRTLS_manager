/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.common.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

import eu.kryl.android.common.Constants;

import static eu.kryl.android.common.util.WordUtils.capitalizeFirstChar;

/**
 * Android app information
 */
public class AndroidAppInfo {

    private static final String CHROMIUM = "chromium";

    /** Unique generated device UUID. It's value can be lost if app's data is reset. */
    private static volatile String deviceUuid = null;
    /** Device's model */
    private static volatile String deviceModel = null;
    /** Device's serial number (due to some Android bugs is not unique) */
    private static volatile String deviceSerial = null;

    private static final String PREF_DEVICE_ID = "pref_device_id";
    private static final String KEY_DEVICE_ID = "device_id";

    /**
     * Are we in an emulator, instead of a real device?
     */
    public static boolean isEmulator() {
        // maybe this check won't work for all emulators
        // need to extend the check if that happens
        return (Constants.DEBUG &&
                (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.FINGERPRINT.startsWith("Android/sdk_phone_x86_64/generic_x86_64")
                ));
    }

    /** Are we running in Chromium? */
    public static boolean isChromium() {
        return (CHROMIUM.equalsIgnoreCase(Build.MANUFACTURER) || CHROMIUM.equalsIgnoreCase(Build.BRAND));
    }

    /**
     * Get a unique device UUID. (Note: This ID does not survive app data reset)
     *
     * @param context Application context
     * @return unique device UUID
     */
    public static String getDeviceUuid(Context context) {
        if (deviceUuid == null) {
            synchronized (AndroidAppInfo.class) {
                if (deviceUuid == null) {
                    final SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_DEVICE_ID, Context.MODE_PRIVATE);
                    deviceUuid = sharedPrefs.getString(KEY_DEVICE_ID, null);
                    if (deviceUuid == null) {
                        deviceUuid = UUID.randomUUID().toString();
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putString(KEY_DEVICE_ID, deviceUuid);
                        editor.commit();
                    }
                }
            }
        }
        return deviceUuid;
    }

    /**
     * Get Android device model
     */
    @NotNull
    public static String getDeviceModel() {
        if (deviceModel == null) {
            synchronized (AndroidAppInfo.class) {
                if (deviceModel == null) {
                    String manufacturer = Build.MANUFACTURER;
                    String model = Build.MODEL;
                    if (model.startsWith(manufacturer)) {
                        deviceModel = capitalizeFirstChar(model);
                    } else {
                        deviceModel = capitalizeFirstChar(manufacturer) + " " + model;
                    }
                    if (TextUtils.isEmpty(deviceModel)) {
                        return "-";
                    }
                }
            }
        }
        return deviceModel;
    }

    public static String getDeviceSerial() {
        if (deviceSerial == null) {
            synchronized (AndroidAppInfo.class) {
                if (deviceSerial == null) {
                    if (Build.SERIAL == null) {
                        deviceSerial = "null";
                    } else {
                        deviceSerial = Build.SERIAL.toUpperCase(Locale.US);
                    }
                }
            }
        }
        return deviceSerial;
    }

}
