/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.debuglog;

import android.os.Looper;
import android.os.SystemClock;

import com.crashlytics.android.Crashlytics;
import com.decawave.argo.api.interaction.Fail;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.error.ErrorCodeInterpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

import eu.kryl.android.common.android.AndroidValidate;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ioc.IocContext.daCtx;

/**
 * Exposes the basic set of ComponentLog routines and those specific to application component log.
 * It delegates log message processing unto LogEntryCollector and also forwards the logging to
 * passed {@link ComponentLog}.
 */
public class ApplicationComponentLog {
    // unmodifiable
    private final ComponentLog logDelegate;
    private final LogEntryTag defaultTag;
    private final String prefix;

    // dependencies
    @Inject
    LogEntryCollector logCollector;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // factory methods
    //
    public static ApplicationComponentLog newNetworkNodeLog(@Nullable ComponentLog systemLogDelegate, @NotNull String bleDeviceAddress) {
        return new ApplicationComponentLog(systemLogDelegate, bleDeviceAddress, new LogEntryDeviceTag(bleDeviceAddress));
    }

    public static ApplicationComponentLog newComponentLog(@Nullable ComponentLog systemLogDelegate, @NotNull String componentName) {
        return new ApplicationComponentLog(systemLogDelegate, componentName, null);
    }
    
    public static ApplicationComponentLog newPlainLog(@Nullable ComponentLog systemLogDelegate) {
        return new ApplicationComponentLog(systemLogDelegate, null, null);
    }

    public static ApplicationComponentLog newPositionLog(@Nullable ComponentLog systemLogDelegate) {
        return new ApplicationComponentLog(systemLogDelegate, null, LogEntryPositionTag.INSTANCE);
    }

    // use one of the above factory methods in order to instantiate ApplicationComponentLog
    private ApplicationComponentLog(@Nullable ComponentLog systemLogDelegate, @Nullable String prefix, @Nullable LogEntryTag defaultTag) {
        this.logDelegate = systemLogDelegate;
        this.prefix = prefix;
        this.defaultTag = defaultTag;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ComponentLog like methods
    //
    public final void d(String message) {
        d(message, null);
    }

    public final void d(String message, LogEntryTag deviceLogEntryTag) {
        // we will print debug regardless the enabled flag
        logMsg(getApplicationLogMessage(message), Severity.DEBUG, null, defaultTag, deviceLogEntryTag);
        if (logDelegate != null) logDelegate.d(getSystemLogMessage(message, deviceLogEntryTag));
    }

    public final void i(String message) {
        i(message, null);
    }

    public final void i(String message, LogEntryTag extraTag) {
        logMsg(getApplicationLogMessage(message), Severity.INFO, null, defaultTag, extraTag);
        if (logDelegate != null) logDelegate.i(getSystemLogMessage(message, extraTag));
    }

    @SuppressWarnings("unused")
    public final void imp(String message) {
        imp(message, null);
    }

    public void imp(String message, LogEntryTag tag) {
        logMsg(getApplicationLogMessage(message), Severity.IMPORTANT, null, defaultTag, tag);
        if (logDelegate != null) logDelegate.i(getSystemLogMessage(message, tag));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // warning / error logging
    //

    public final void we(String message, Fail causeFail) {
        we(message + ": " + causeFail.message, causeFail.errorCode, null, null);
    }

    public final void we(String message, int errorCode) {
        we(message, errorCode, null, null);
    }

    public final void we(String message, int errorCode, Throwable exc) {
        we(message, errorCode, exc, null);
    }

    public final void we(String message, int errorCode, Throwable exc, LogEntryTag extraTag) {
        Severity severity = ErrorCodeInterpreter.interpret(errorCode).warningOnly ? Severity.WARNING : Severity.ERROR;
        logMsg(getApplicationLogMessage(message), severity, errorCode, exc, defaultTag, extraTag);
        if (logDelegate != null) {
            if (severity == Severity.ERROR) {
                logDelegate.e(getSystemLogMessage(message, extraTag), exc);
            } else {
                logDelegate.w(getSystemLogMessage(message, extraTag), exc);
            }
        }
    }

    public void we(String message, Fail causeFail, LogEntryTag extraTag) {
        we(message + ": " + causeFail.message, causeFail.errorCode, extraTag);
    }

    public void we(String message, int errorCode, LogEntryTag extraTag) {
        Severity severity = ErrorCodeInterpreter.interpret(errorCode).warningOnly ? Severity.WARNING : Severity.ERROR;
        logMsg(getApplicationLogMessage(message), severity, errorCode, defaultTag, extraTag);
        if (logDelegate != null) {
            if (severity == Severity.ERROR) {
                logDelegate.e(getSystemLogMessage(message, extraTag));
            } else {
                logDelegate.w(getSystemLogMessage(message, extraTag));
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //

    private void logMsg(String message, Severity severity, Integer errorCode, LogEntryTag... tags) {
        logMsg(message, severity, errorCode, null, tags);
    }

    private synchronized void logMsg(String msg, Severity severity, Integer errorCode, Throwable exception, LogEntryTag... tags) {
        AndroidValidate.runningOnUiThread();
        if (logCollector == null) {
            // inject log collector instance
            daCtx.inject(this);
        }
        logCollector.add(new LogEntry(SystemClock.uptimeMillis(), msg, severity, errorCode, exception, tags));
        if (Constants.CRASH_REPORTS_ENABLED) {
            Crashlytics.log(msg);
        }
    }

    private String getApplicationLogMessage(String message) {
        if (prefix != null) {
            message = "[" + prefix + "] " + message;
        }
        if(Looper.myLooper() != Looper.getMainLooper()) {
            message = Thread.currentThread().toString() + " " + message;
        }
        return message;
    }

    private String getSystemLogMessage(String message, LogEntryTag... tags) {
        message = prefixMessageIfDeviceTag(message, defaultTag);
        for (LogEntryTag tag : tags) {
            message = prefixMessageIfDeviceTag(message, tag);
        }
        if(Looper.myLooper() != Looper.getMainLooper()) {
            message = Thread.currentThread().toString() + " " + message;
        }
        return message;
    }

    private String prefixMessageIfDeviceTag(String message, LogEntryTag tag) {
        if (tag instanceof LogEntryDeviceTag) {
            String bleAddress = ((LogEntryDeviceTag) tag).bleAddress;
            if (!message.contains(bleAddress)) {
                message = "[" + bleAddress + "] " + message;
            }
        }
        return message;
    }


}
