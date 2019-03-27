package com.decawave.argomanager.ui;

import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  public MainActivity_MembersInjector(
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    return new MainActivity_MembersInjector(
        networkNodeManagerProvider,
        discoveryManagerProvider,
        appPreferenceAccessorProvider,
        permissionHelperProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectNetworkNodeManager(
      MainActivity instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectDiscoveryManager(
      MainActivity instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      MainActivity instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectPermissionHelper(
      MainActivity instance, Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }
}
