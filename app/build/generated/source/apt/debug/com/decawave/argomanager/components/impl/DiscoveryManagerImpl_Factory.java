package com.decawave.argomanager.components.impl;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.NetworkNodeManager;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DiscoveryManagerImpl_Factory implements Factory<DiscoveryManagerImpl> {
  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<BlePresenceApi> blePresenceApiProvider;

  public DiscoveryManagerImpl_Factory(
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BlePresenceApi> blePresenceApiProvider) {
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert blePresenceApiProvider != null;
    this.blePresenceApiProvider = blePresenceApiProvider;
  }

  @Override
  public DiscoveryManagerImpl get() {
    return new DiscoveryManagerImpl(
        discoveryApiProvider.get(), networkNodeManagerProvider.get(), blePresenceApiProvider.get());
  }

  public static Factory<DiscoveryManagerImpl> create(
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BlePresenceApi> blePresenceApiProvider) {
    return new DiscoveryManagerImpl_Factory(
        discoveryApiProvider, networkNodeManagerProvider, blePresenceApiProvider);
  }

  /**
   * Proxies {@link DiscoveryManagerImpl#DiscoveryManagerImpl(DiscoveryApi, NetworkNodeManager,
   * BlePresenceApi)}.
   */
  public static DiscoveryManagerImpl newDiscoveryManagerImpl(
      DiscoveryApi discoveryApi,
      NetworkNodeManager networkNodeManager,
      BlePresenceApi blePresenceApi) {
    return new DiscoveryManagerImpl(discoveryApi, networkNodeManager, blePresenceApi);
  }
}
