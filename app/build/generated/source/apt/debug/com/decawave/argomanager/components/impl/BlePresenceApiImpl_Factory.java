package com.decawave.argomanager.components.impl;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.NetworkNodeManager;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class BlePresenceApiImpl_Factory implements Factory<BlePresenceApiImpl> {
  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  public BlePresenceApiImpl_Factory(
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
  }

  @Override
  public BlePresenceApiImpl get() {
    return new BlePresenceApiImpl(
        discoveryApiProvider.get(),
        networkNodeManagerProvider.get(),
        bleConnectionApiProvider.get());
  }

  public static Factory<BlePresenceApiImpl> create(
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    return new BlePresenceApiImpl_Factory(
        discoveryApiProvider, networkNodeManagerProvider, bleConnectionApiProvider);
  }

  /**
   * Proxies {@link BlePresenceApiImpl#BlePresenceApiImpl(DiscoveryApi, NetworkNodeManager,
   * BleConnectionApi)}.
   */
  public static BlePresenceApiImpl newBlePresenceApiImpl(
      DiscoveryApi discoveryApi,
      NetworkNodeManager networkNodeManager,
      BleConnectionApi bleConnectionApi) {
    return new BlePresenceApiImpl(discoveryApi, networkNodeManager, bleConnectionApi);
  }
}
