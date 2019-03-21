package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DeviceDebugConsoleFragment_MembersInjector
    implements MembersInjector<DeviceDebugConsoleFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<LogEntryCollector> logEntryCollectorProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<LocationDataLogger> locationDataLoggerProvider;

  public DeviceDebugConsoleFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert logEntryCollectorProvider != null;
    this.logEntryCollectorProvider = logEntryCollectorProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert locationDataLoggerProvider != null;
    this.locationDataLoggerProvider = locationDataLoggerProvider;
  }

  public static MembersInjector<DeviceDebugConsoleFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider) {
    return new DeviceDebugConsoleFragment_MembersInjector(
        appPreferenceAccessorProvider,
        logEntryCollectorProvider,
        bleConnectionApiProvider,
        networkNodeManagerProvider,
        discoveryManagerProvider,
        locationDataLoggerProvider);
  }

  @Override
  public void injectMembers(DeviceDebugConsoleFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((LogBufferFragment) instance).logEntryCollector = logEntryCollectorProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.locationDataLogger = locationDataLoggerProvider.get();
  }

  public static void injectBleConnectionApi(
      DeviceDebugConsoleFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectNetworkNodeManager(
      DeviceDebugConsoleFragment instance,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectDiscoveryManager(
      DeviceDebugConsoleFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectLocationDataLogger(
      DeviceDebugConsoleFragment instance,
      Provider<LocationDataLogger> locationDataLoggerProvider) {
    instance.locationDataLogger = locationDataLoggerProvider.get();
  }
}
