package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkModelManager;
import com.decawave.argomanager.components.NetworksNodesStorage;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import dagger.internal.Factory;
import dagger.internal.MembersInjectors;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworkNodeManagerImpl_Factory implements Factory<NetworkNodeManagerImpl> {
  private final MembersInjector<NetworkNodeManagerImpl> networkNodeManagerImplMembersInjector;

  private final Provider<NetworkModelManager> networkModelManagerProvider;

  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<LocationDataLogger> locationDataLoggerProvider;

  private final Provider<NetworksNodesStorage> storageProvider;

  private final Provider<UniqueReorderingStack<Short>> activeNetworkIdStackProvider;

  public NetworkNodeManagerImpl_Factory(
      MembersInjector<NetworkNodeManagerImpl> networkNodeManagerImplMembersInjector,
      Provider<NetworkModelManager> networkModelManagerProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider,
      Provider<NetworksNodesStorage> storageProvider,
      Provider<UniqueReorderingStack<Short>> activeNetworkIdStackProvider) {
    assert networkNodeManagerImplMembersInjector != null;
    this.networkNodeManagerImplMembersInjector = networkNodeManagerImplMembersInjector;
    assert networkModelManagerProvider != null;
    this.networkModelManagerProvider = networkModelManagerProvider;
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert locationDataLoggerProvider != null;
    this.locationDataLoggerProvider = locationDataLoggerProvider;
    assert storageProvider != null;
    this.storageProvider = storageProvider;
    assert activeNetworkIdStackProvider != null;
    this.activeNetworkIdStackProvider = activeNetworkIdStackProvider;
  }

  @Override
  public NetworkNodeManagerImpl get() {
    return MembersInjectors.injectMembers(
        networkNodeManagerImplMembersInjector,
        new NetworkNodeManagerImpl(
            networkModelManagerProvider.get(),
            appPreferenceAccessorProvider.get(),
            locationDataLoggerProvider.get(),
            storageProvider.get(),
            activeNetworkIdStackProvider.get()));
  }

  public static Factory<NetworkNodeManagerImpl> create(
      MembersInjector<NetworkNodeManagerImpl> networkNodeManagerImplMembersInjector,
      Provider<NetworkModelManager> networkModelManagerProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider,
      Provider<NetworksNodesStorage> storageProvider,
      Provider<UniqueReorderingStack<Short>> activeNetworkIdStackProvider) {
    return new NetworkNodeManagerImpl_Factory(
        networkNodeManagerImplMembersInjector,
        networkModelManagerProvider,
        appPreferenceAccessorProvider,
        locationDataLoggerProvider,
        storageProvider,
        activeNetworkIdStackProvider);
  }
}
