package com.decawave.argomanager.components.impl;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DecodeContextManagerImpl_Factory implements Factory<DecodeContextManagerImpl> {
  private static final DecodeContextManagerImpl_Factory INSTANCE =
      new DecodeContextManagerImpl_Factory();

  @Override
  public DecodeContextManagerImpl get() {
    return new DecodeContextManagerImpl();
  }

  public static Factory<DecodeContextManagerImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link DecodeContextManagerImpl#DecodeContextManagerImpl()}. */
  public static DecodeContextManagerImpl newDecodeContextManagerImpl() {
    return new DecodeContextManagerImpl();
  }
}
