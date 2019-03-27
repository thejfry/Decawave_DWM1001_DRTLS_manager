package com.decawave.argomanager.components.impl;

import dagger.MembersInjector;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworkNodeManagerImpl_MembersInjector
    implements MembersInjector<NetworkNodeManagerImpl> {
  public NetworkNodeManagerImpl_MembersInjector() {}

  public static MembersInjector<NetworkNodeManagerImpl> create() {
    return new NetworkNodeManagerImpl_MembersInjector();
  }

  @Override
  public void injectMembers(NetworkNodeManagerImpl instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.init();
  }
}
