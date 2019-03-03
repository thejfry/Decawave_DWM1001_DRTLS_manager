package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class MainScreenFragment_MembersInjector
    implements MembersInjector<MainScreenFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  public MainScreenFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
  }

  public static MembersInjector<MainScreenFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    return new MainScreenFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        networkNodeManagerProvider);
  }

  @Override
  public void injectMembers(MainScreenFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((DiscoveryProgressAwareFragment) instance).discoveryApi = discoveryApiProvider.get();
    ((DiscoveryProgressAwareFragment) instance).errorManager = errorManagerProvider.get();
    ((DiscoveryProgressAwareFragment) instance).appPreferenceAccessor =
        appPreferenceAccessorProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      MainScreenFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }
}
