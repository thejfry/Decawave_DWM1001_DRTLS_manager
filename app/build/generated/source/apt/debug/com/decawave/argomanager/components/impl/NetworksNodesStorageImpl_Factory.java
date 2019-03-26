package com.decawave.argomanager.components.impl;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworksNodesStorageImpl_Factory implements Factory<NetworksNodesStorageImpl> {
  private static final NetworksNodesStorageImpl_Factory INSTANCE =
      new NetworksNodesStorageImpl_Factory();

  @Override
  public NetworksNodesStorageImpl get() {
    return new NetworksNodesStorageImpl();
  }

  public static Factory<NetworksNodesStorageImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link NetworksNodesStorageImpl#NetworksNodesStorageImpl()}. */
  public static NetworksNodesStorageImpl newNetworksNodesStorageImpl() {
    return new NetworksNodesStorageImpl();
  }
}
