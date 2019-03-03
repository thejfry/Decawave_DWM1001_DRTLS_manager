/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhPresenceApiListener;
import com.decawave.argomanager.components.impl.NetworkNodeManagerImpl;
import com.decawave.argomanager.components.impl.UniqueReorderingStack;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.ioc.IocContext;
import com.decawave.argomanager.prefs.AppPreference;
import com.decawave.argomanager.prefs.IhAppPreferenceListener;
import com.google.common.base.Preconditions;

import javax.inject.Inject;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.utils.Log;
import eu.kryl.android.common.async.AsyncJob;
import eu.kryl.android.common.async.FixedAsyncActivityScheduler;
import eu.kryl.android.common.async.SbHandler;
import eu.kryl.android.common.async.WorkerThread;
import eu.kryl.android.common.async.impl.SbHandlerAndroidImpl;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubFactory;
import eu.kryl.android.common.log.ComponentLog;
import eu.kryl.android.common.log.LogLevel;
import io.fabric.sdk.android.Fabric;

import static com.decawave.argomanager.ioc.IocContext.daCtx;


/**
 * Main application class.
 */
public class ArgoApp extends MultiDexApplication {

    static {
        ComponentLog.MAIN_PACKAGE_NAME = "com.decawave.argomanager";
        ComponentLog.APP_TAG = "ARGO";
        ComponentLog.DEFAULT_LOG_LEVEL = LogLevel.DEBUG;
    }

    public static final ComponentLog log = new ComponentLog(ArgoApp.class);

    public static ArgoApp daApp;

    public static long startTime;

    // handler to run things on UI thread
    public static SbHandler uiHandler;
    public static SbHandler workerSbHandler;

    // identification
    public static String ANDROID_ID;

    @Inject
    BlePresenceApi blePresenceApi;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    LogEntryCollector logEntryCollector;

    @Inject
    UniqueReorderingStack<Short> activeNetworkStack;

    // members
    ApplicationComponentLog appLog;

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        log.d("onCreate()");
        super.onCreate();
        // device ID
        ANDROID_ID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        // fabric init
        initializeFabric();
        // set up start time
        startTime = SystemClock.uptimeMillis();
        // make sure the debug flags are the same
        Preconditions.checkState(BuildConfig.DEBUG == eu.kryl.common.BuildConfig.DEBUG);
        // set up daApp reference
        daApp = this;
        // ui/worker handlers
        setupHandlers();
        // initialize our IOC container
        IocContext.init();
        // inject IOCed members
        daCtx.inject(this);
        // set up logging
        setupAppLog();
        // set up presence API
        initPresenceApi();
        loadNodesFromStorage();
    }

    private void loadNodesFromStorage() {
        // call the internal load routine
        ((NetworkNodeManagerImpl) networkNodeManager).load();
    }

    private void initializeFabric() {
        Fabric.Builder b = new Fabric.Builder(this);
        if (Constants.CRASH_REPORTS_ENABLED) {
            // initialize crashlytics
            b.kits(new Crashlytics());
        }
        Fabric.with(b.build());
        if (Constants.CRASH_REPORTS_ENABLED) {
            // set up the bits
            Crashlytics.setUserIdentifier(ArgoApp.ANDROID_ID);
            Crashlytics.setString("BuildConfig.BUILD_TIME", BuildConfig.BUILD_TIME);
            Crashlytics.setString("BuildConfig.BUILD_TYPE", BuildConfig.BUILD_TYPE);
            Crashlytics.setBool("BuildConfig.DEBUG", BuildConfig.DEBUG);
            Crashlytics.setBool("Constants.DEBUG", Constants.DEBUG);
            Crashlytics.setBool("Constants.ENFORCE_DEBUG_LOGGING_AND_ASSERTS", Constants.ENFORCE_DEBUG_LOGGING_AND_ASSERTS);
            Crashlytics.setBool("Constants.ENFORCE_DEBUG_UI", Constants.ENFORCE_DEBUG_UI);
        }
    }

    private void initPresenceApi() {
        // do initialization
        blePresenceApi.init();
        //noinspection ConstantConditions,ConstantIfStatement
        if (false) {
            InterfaceHub.registerHandler(new IhPresenceApiListener() {

                @Override
                public void onNodePresent(String nodeBleAddress) {
                    if (Constants.DEBUG) {
                        log.w("onNodePresent: " + "node = [" + nodeBleAddress + "]");
                    }
                }

                @Override
                public void onNodeMissing(String nodeBleAddress) {
                    if (Constants.DEBUG) {
                        log.w("onNodeMissing: " + "nodeBleAddress = [" + nodeBleAddress + "]");
                    }
                }

                @Override
                public void onNodeRssiChanged(String bleAddress, int rssi) {
                    if (Constants.DEBUG) {
                        log.d("onNodeRssiChanged: " + "bleAddress = [" + bleAddress + "], rssi = [" + rssi + "]");
                    }
                }

                @Override
                public void onTagDirectObserve(String bleAddress, boolean observe) {
                    if (Constants.DEBUG)
                        log.d("onTagDirectObserve() called with: " + "bleAddress = [" + bleAddress + "], observe = [" + observe + "]");
                }
            });
        }
    }

    private void setupAppLog() {
        // set up flexible adapter logging
        FlexibleAdapter.enableLogs(Log.Level.DEBUG);
        // we can do this only after our IOC container has been initialized
        appLog = ApplicationComponentLog.newComponentLog(log, "APP");
        // register as a listener to active network change
        InterfaceHub.registerHandler((IhAppPreferenceListener) (element, oldValue, newValue) -> {
            if (element == AppPreference.Element.ACTIVE_NETWORK_ID) {
                // print a new preamble
                printLogPreamble();
                // handle active network stack
                handleActiveNetworkSwitch((Short) oldValue, (Short) newValue);
            }
        });
    }

    private void handleActiveNetworkSwitch(Short oldNetworkId, Short newNetworkId) {
        if (newNetworkId != null && oldNetworkId != null) activeNetworkStack.pushOrMove(oldNetworkId);
    }

    public void printLogPreamble() {
        // print introductory log messages
        NetworkModel activeNetwork = networkNodeManager.getActiveNetwork();
        String title;
        if (activeNetwork != null) {
            title = ArgoApp.daApp.getString(R.string.log_title_network, activeNetwork.getNetworkName());
        } else {
            title = ArgoApp.daApp.getString(R.string.log_title_discovery);
        }
        appLog.i("**********************************************");
        appLog.i("**  " + String.format("%-39s", title)  +  " **");
        appLog.i("**********************************************");
    }


    private void setupHandlers() {
        Handler workerHandler = WorkerThread.startNewWorkerThreadAndWait().mHandler;
        // this test is needed because of interference with test app class initialization :( stupid Android
        if (uiHandler == null && workerSbHandler == null) {
            // setup handlers
            uiHandler = new SbHandlerAndroidImpl(new Handler(Looper.getMainLooper()));
            workerSbHandler = new SbHandlerAndroidImpl(workerHandler);
        }
        // setup AsyncJob toolkit
        AsyncJob.setCleanupWorkerHandler(workerSbHandler);
        // setup DelayedAsyncActivity toolkit
        FixedAsyncActivityScheduler.setFallbackWorkerHandler(workerSbHandler);
        // inject common ui handler to IH
        InterfaceHubFactory.setUiSbHandler(uiHandler);
    }

    public static void reportSilentException(Throwable t) {
        log.e("reportSilentException", t);
        Crashlytics.logException(t);
    }


}
