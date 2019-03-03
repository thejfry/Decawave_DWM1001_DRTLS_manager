/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ioc;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.PeriodicBleScanner;
import com.decawave.argomanager.argoapi.ble.PeriodicBleScannerImpl;
import com.decawave.argomanager.argoapi.ble.DiscoveryApiBleImpl;
import com.decawave.argomanager.argoapi.ble.connection.BleConnectionApiImpl;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreter;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreterImpl;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DecodeContextManager;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.NetworkModelManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.NetworksNodesStorage;
import com.decawave.argomanager.components.PositionObservationManager;
import com.decawave.argomanager.components.impl.AutoPositioningManagerImpl;
import com.decawave.argomanager.components.impl.BlePresenceApiImpl;
import com.decawave.argomanager.components.impl.DecodeContextManagerImpl;
import com.decawave.argomanager.components.impl.DiscoveryManagerImpl;
import com.decawave.argomanager.components.impl.ErrorManagerImpl;
import com.decawave.argomanager.components.impl.LocationDataLoggerImpl;
import com.decawave.argomanager.components.impl.LocationDataObserverImpl;
import com.decawave.argomanager.components.impl.NetworkModelManagerImpl;
import com.decawave.argomanager.components.impl.NetworkNodeManagerImpl;
import com.decawave.argomanager.components.impl.NetworksNodesStorageImpl;
import com.decawave.argomanager.components.impl.PositionObservationManagerImpl;
import com.decawave.argomanager.debuglog.LogBlockStatus;
import com.decawave.argomanager.debuglog.LogBlockStatusImpl;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.debuglog.LogEntryCollectorImpl;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.AppPreferenceAccessorImpl;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.AndroidPermissionHelperImpl;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.decawave.argomanager.util.NetworkNodePropertyDecoratorImpl;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;

/**
 * Argo project.
 *
 * Bind interfaces to their concrete implementations.
 */
@Module
abstract class ArgoDependencyBindingsModule {

    @Binds @Singleton
    abstract DiscoveryApi provideDiscoveryApi(DiscoveryApiBleImpl impl);

    @Binds @Singleton
    abstract BlePresenceApi provideBlePresenceApi(BlePresenceApiImpl impl);

    @Binds @Singleton
    abstract LocationDataObserver provideLocationDataObserver(LocationDataObserverImpl impl);

    @Binds @Singleton
    abstract BleConnectionApi provideBleConnectionApi(BleConnectionApiImpl impl);

    @Binds @Singleton
    abstract AppPreferenceAccessor provideAppPreferenceAccessor(AppPreferenceAccessorImpl impl);

    @Binds @Singleton
    abstract NetworkNodeManager provideNetworkModelManager(NetworkNodeManagerImpl impl);

    @Binds @Singleton
    abstract PositionObservationManager provideObservationManager(PositionObservationManagerImpl impl);

    @Binds @Singleton
    abstract DiscoveryManager provideDiscoveryManager(DiscoveryManagerImpl impl);

    @Binds @Singleton
    abstract SignalStrengthInterpreter provideSignalStrengthInterpreter(SignalStrengthInterpreterImpl impl);

    @Binds @Singleton
    abstract NetworkNodePropertyDecorator providePropertyDecorator(NetworkNodePropertyDecoratorImpl impl);

    @Binds @Singleton
    abstract LocationDataLogger provideLocationDataLogger(LocationDataLoggerImpl impl);

    @Binds @Singleton
    abstract ErrorManager provideErrorManager(ErrorManagerImpl errorManager);

    @Binds @Singleton
    abstract AndroidPermissionHelper provideAndroidPermissionHelper(AndroidPermissionHelperImpl impl);

    @Binds @Singleton
    abstract LogEntryCollector provideLogEntryCollector(LogEntryCollectorImpl impl);

    @Binds @Singleton
    abstract NetworkModelManager provideNetworkModelRepository(NetworkModelManagerImpl impl);

    @Binds @Singleton
    abstract NetworksNodesStorage provideNetworkModelStorage(NetworksNodesStorageImpl impl);

    @Binds @Singleton
    abstract AutoPositioningManager provideAutoPositioningManager(AutoPositioningManagerImpl impl);

    @Binds @Singleton
    abstract LogBlockStatus provideLogEntryCollectorBlockStatus(LogBlockStatusImpl impl);

    @Binds @Singleton
    abstract DecodeContextManager providedDecodeContextManager(DecodeContextManagerImpl impl);

    @Binds @Singleton
    abstract PeriodicBleScanner providedBleScanner(PeriodicBleScannerImpl impl);
}
