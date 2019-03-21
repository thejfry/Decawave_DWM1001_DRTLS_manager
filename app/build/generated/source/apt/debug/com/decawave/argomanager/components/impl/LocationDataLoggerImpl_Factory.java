package com.decawave.argomanager.components.impl;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class LocationDataLoggerImpl_Factory implements Factory<LocationDataLoggerImpl> {
  private static final LocationDataLoggerImpl_Factory INSTANCE =
      new LocationDataLoggerImpl_Factory();

  @Override
  public LocationDataLoggerImpl get() {
    return new LocationDataLoggerImpl();
  }

  public static Factory<LocationDataLoggerImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link LocationDataLoggerImpl#LocationDataLoggerImpl()}. */
  public static LocationDataLoggerImpl newLocationDataLoggerImpl() {
    return new LocationDataLoggerImpl();
  }
}
