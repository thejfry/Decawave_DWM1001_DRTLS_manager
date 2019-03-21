package com.decawave.argomanager.ui.fragment;

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
public final class DeviceErrorFragment_MembersInjector
    implements MembersInjector<DeviceErrorFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  public DeviceErrorFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
  }

  public static MembersInjector<DeviceErrorFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    return new DeviceErrorFragment_MembersInjector(
        appPreferenceAccessorProvider, errorManagerProvider, networkNodeManagerProvider);
  }

  @Override
  public void injectMembers(DeviceErrorFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.errorManager = errorManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectErrorManager(
      DeviceErrorFragment instance, Provider<ErrorManager> errorManagerProvider) {
    instance.errorManager = errorManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      DeviceErrorFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      DeviceErrorFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
