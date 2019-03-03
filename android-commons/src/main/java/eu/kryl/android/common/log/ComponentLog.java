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

package eu.kryl.android.common.log;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;

import static eu.kryl.android.common.Constants.DEBUG;

/**
 * Initialize ComponentLog in your app's static initializer similarly as:
 *
 * static {
 *      if (ComponentLog.MAIN_PACKAGE_NAME == null) {
 *          ComponentLog.MAIN_PACKAGE_NAME = "eu.kryl.appackage";
 *          ComponentLog.APP_TAG = "MY_APP_TAG";
 *          ComponentLog.DEFAULT_LOG_LEVEL = LogLevel.DEBUG;
 *      }
 * }
 *
 * @author pavel
 *
 */
public class ComponentLog {
    // system wide/shared/global
    public static String MAIN_PACKAGE_NAME = "com.example.package";
    public static String APP_TAG = "APP_TAG";
    public static LogLevel DEFAULT_LOG_LEVEL = DEBUG ? LogLevel.DEBUG : LogLevel.WARNING;
    // instance specific
    private final String tag;
    //
    private @NotNull LogLevel logLevel;

    public ComponentLog(String subTag) {
        this.tag = getTag(subTag);
        this.logLevel = DEFAULT_LOG_LEVEL;
    }

    public ComponentLog disable() {
        logLevel = LogLevel.WARNING;
        return this;
    }

    @NonNull
    public LogLevel getLogLevel() {
        return logLevel;
    }

    public boolean isEnabled() {
        return logLevel == LogLevel.DEBUG;
    }

    public ComponentLog setLogLevel(@NonNull LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public ComponentLog setEnabled(boolean enabled) {
        this.logLevel = enabled ? LogLevel.DEBUG : LogLevel.WARNING;
        return this;
    }

    public ComponentLog(Class<?> instanceClass) {
        this(instanceClass, true);
    }

    public ComponentLog(Class<?> instanceClass, boolean alsoClassName) {
        this(getSubTag(instanceClass, alsoClassName));
    }

    private static String getTag(String subTag) {
        return APP_TAG + (subTag.length() > 0 ? "." + subTag : "");
    }

    public String getTag() {
        return tag;
    }

    private static String getSubTag(Class<?> instanceClass, boolean alsoClassName) {
        Package pkg = instanceClass.getPackage();
        String afterMainPkgName;
        if (pkg == null)
            afterMainPkgName = "";
        else {
            final String pkgName = pkg.getName();
            if (pkgName.startsWith(MAIN_PACKAGE_NAME)) {
                if (pkgName.equals(MAIN_PACKAGE_NAME))
                    afterMainPkgName = "";
                else
                    afterMainPkgName = pkgName.substring(MAIN_PACKAGE_NAME.length() + 1);
            } else {
                afterMainPkgName = pkgName;
            }
        }
        if (afterMainPkgName.endsWith(".impl")) {
            // cut the '.impl' - it says nothing
            afterMainPkgName = afterMainPkgName.substring(0, afterMainPkgName.length() - 5);
        }
        String subTag = "";
        if(afterMainPkgName.length() > 0) {
            subTag = afterMainPkgName + ".";
        }
        if(alsoClassName) {
            subTag = subTag + instanceClass.getSimpleName();
        }
        return subTag;
    }

    public final void d(Class<?> cls, String message) {
        if (logLevel.pass(LogLevel.DEBUG)) {
            final String _msg = getMessage(message);
            Log.d(getTag(getSubTag(cls, true)), getTimePrefix() + _msg);
        }
    }

    public final void d(String message) {
        if (logLevel.pass(LogLevel.DEBUG)) {
            final String _msg = getMessage(message);
            Log.d(tag, getTimePrefix() + _msg);
        }
    }

    private static NumberFormat nf;

    static {
        nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(3);
        nf.setMinimumIntegerDigits(3);
    }

    private static String getTimePrefix() {
        return nf.format((System.currentTimeMillis() % 1000000) / 1000f)+ " ";
    }

    private String getMessage(String message) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            return Thread.currentThread().toString() + " " + message;
        }
        // else
        return message;
    }


    public final void e(String message) {
        if (logLevel.pass(LogLevel.ERROR)) {
            final String _msg = getMessage(message);
            Log.e(tag, ((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg);
        }
    }

    public final void e(String message, Throwable t) {
        if (logLevel.pass(LogLevel.ERROR)) {
            final String _msg = getMessage(message);
            Log.e(tag, ((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg, t);
        }
    }

    public final void i(String message) {
        if (logLevel.pass(LogLevel.INFO)) {
            final String _msg = getMessage(message);
            Log.i(tag, ((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg);
        }
    }

    public final void w(String message) {
        if (logLevel.pass(LogLevel.WARNING)) {
            final String _msg = getMessage(message);
            Log.w(tag, ((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg);
        }
    }

    public final void w(String message, Throwable t) {
        if (logLevel.pass(LogLevel.WARNING)) {
            final String _msg = getMessage(message);
            Log.w(tag, ((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg, t);
        }
    }

    public final void w(Throwable t) {
        if (logLevel.pass(LogLevel.WARNING)) {
            Log.w(tag, String.valueOf((System.currentTimeMillis() % 1000000) / 1000f), t);
        }
    }

    public final void w(Class<?> cls, String message, Throwable t) {
        if (logLevel.pass(LogLevel.WARNING)) {
            final String _msg = getMessage(message);
            Log.w(getTag(getSubTag(cls, true)),((System.currentTimeMillis() % 1000000) / 1000f) + " " + _msg, t);
        }
    }

}
