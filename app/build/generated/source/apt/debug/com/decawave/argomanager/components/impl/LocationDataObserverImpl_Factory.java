package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class LocationDataObserverImpl_Factory implements Factory<LocationDataObserverImpl> {
  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<BlePresenceApi> blePresenceApiProvider;

  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public LocationDataObserverImpl_Factory(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BlePresenceApi> blePresenceApiProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert blePresenceApiProvider != null;
    this.blePresenceApiProvider = blePresenceApiProvider;
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  @Override
  public LocationDataObserverImpl get() {
    return new LocationDataObserverImpl(
        bleConnectionApiProvider.get(),
        discoveryManagerProvider.get(),
        networkNodeManagerProvider.get(),
        blePresenceApiProvider.get(),
        appPreferenceAccessorProvider.get());
  }

  public static Factory<LocationDataObserverImpl> create(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BlePresenceApi> blePresenceApiProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new LocationDataObserverImpl_Factory(
        bleConnectionApiProvider,
        discoveryManagerProvider,
        networkNodeManagerProvider,
        blePresenceApiProvider,
        appPreferenceAccessorProvider);
  }

  /**
   * Proxies {@link LocationDataObserverImpl#LocationDataObserverImpl(BleConnectionApi,
   * DiscoveryManager, NetworkNodeManager, BlePresenceApi, AppPreferenceAccessor)}.
   */
  public static LocationDataObserverImpl newLocationDataObserverImpl(
      BleConnectionApi bleConnectionApi,
      DiscoveryManager discoveryManager,
      NetworkNodeManager networkNodeManager,
      BlePresenceApi blePresenceApi,
      AppPreferenceAccessor appPreferenceAccessor) {
    return new LocationDataObserverImpl(
        bleConnectionApi,
        discoveryManager,
        networkNodeManager,
        blePresenceApi,
        appPreferenceAccessor);
  }
}
