package com.decawave.argomanager.components.impl;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworkModelManagerImpl_Factory implements Factory<NetworkModelManagerImpl> {
  private static final NetworkModelManagerImpl_Factory INSTANCE =
      new NetworkModelManagerImpl_Factory();

  @Override
  public NetworkModelManagerImpl get() {
    return new NetworkModelManagerImpl();
  }

  public static Factory<NetworkModelManagerImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link NetworkModelManagerImpl#NetworkModelManagerImpl()}. */
  public static NetworkModelManagerImpl newNetworkModelManagerImpl() {
    return new NetworkModelManagerImpl();
  }
}
