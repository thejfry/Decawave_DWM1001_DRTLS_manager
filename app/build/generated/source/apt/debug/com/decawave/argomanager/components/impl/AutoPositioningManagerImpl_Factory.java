package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.NetworkNodeManager;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class AutoPositioningManagerImpl_Factory
    implements Factory<AutoPositioningManagerImpl> {
  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  public AutoPositioningManagerImpl_Factory(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
  }

  @Override
  public AutoPositioningManagerImpl get() {
    return new AutoPositioningManagerImpl(
        bleConnectionApiProvider.get(), networkNodeManagerProvider.get());
  }

  public static Factory<AutoPositioningManagerImpl> create(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    return new AutoPositioningManagerImpl_Factory(
        bleConnectionApiProvider, networkNodeManagerProvider);
  }

  /**
   * Proxies {@link AutoPositioningManagerImpl#AutoPositioningManagerImpl(BleConnectionApi,
   * NetworkNodeManager)}.
   */
  public static AutoPositioningManagerImpl newAutoPositioningManagerImpl(
      BleConnectionApi bleConnectionApi, NetworkNodeManager networkNodeManager) {
    return new AutoPositioningManagerImpl(bleConnectionApi, networkNodeManager);
  }
}
