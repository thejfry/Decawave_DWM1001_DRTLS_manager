package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DiscoveryProgressAwareFragment_MembersInjector
    implements MembersInjector<DiscoveryProgressAwareFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  public DiscoveryProgressAwareFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
  }

  public static MembersInjector<DiscoveryProgressAwareFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider) {
    return new DiscoveryProgressAwareFragment_MembersInjector(
        appPreferenceAccessorProvider, discoveryApiProvider, errorManagerProvider);
  }

  @Override
  public void injectMembers(DiscoveryProgressAwareFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.discoveryApi = discoveryApiProvider.get();
    instance.errorManager = errorManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectDiscoveryApi(
      DiscoveryProgressAwareFragment instance, Provider<DiscoveryApi> discoveryApiProvider) {
    instance.discoveryApi = discoveryApiProvider.get();
  }

  public static void injectErrorManager(
      DiscoveryProgressAwareFragment instance, Provider<ErrorManager> errorManagerProvider) {
    instance.errorManager = errorManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      DiscoveryProgressAwareFragment instance,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
