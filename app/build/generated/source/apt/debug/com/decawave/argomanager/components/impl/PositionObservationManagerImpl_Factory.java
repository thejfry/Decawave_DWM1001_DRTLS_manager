package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.components.LocationDataObserver;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class PositionObservationManagerImpl_Factory
    implements Factory<PositionObservationManagerImpl> {
  private final Provider<LocationDataObserver> locationDataObserverProvider;

  public PositionObservationManagerImpl_Factory(
      Provider<LocationDataObserver> locationDataObserverProvider) {
    assert locationDataObserverProvider != null;
    this.locationDataObserverProvider = locationDataObserverProvider;
  }

  @Override
  public PositionObservationManagerImpl get() {
    return new PositionObservationManagerImpl(locationDataObserverProvider.get());
  }

  public static Factory<PositionObservationManagerImpl> create(
      Provider<LocationDataObserver> locationDataObserverProvider) {
    return new PositionObservationManagerImpl_Factory(locationDataObserverProvider);
  }

  /**
   * Proxies {@link
   * PositionObservationManagerImpl#PositionObservationManagerImpl(LocationDataObserver)}.
   */
  public static PositionObservationManagerImpl newPositionObservationManagerImpl(
      LocationDataObserver locationDataObserver) {
    return new PositionObservationManagerImpl(locationDataObserver);
  }
}
