package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
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
public final class DiscoveryFragment_MembersInjector implements MembersInjector<DiscoveryFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  public DiscoveryFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
  }

  public static MembersInjector<DiscoveryFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    return new DiscoveryFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        discoveryManagerProvider,
        networkNodeManagerProvider,
        permissionHelperProvider,
        bleConnectionApiProvider);
  }

  @Override
  public void injectMembers(DiscoveryFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((DiscoveryProgressAwareFragment) instance).discoveryApi = discoveryApiProvider.get();
    ((DiscoveryProgressAwareFragment) instance).errorManager = errorManagerProvider.get();
    ((DiscoveryProgressAwareFragment) instance).appPreferenceAccessor =
        appPreferenceAccessorProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.permissionHelper = permissionHelperProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectDiscoveryManager(
      DiscoveryFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      DiscoveryFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      DiscoveryFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectPermissionHelper(
      DiscoveryFragment instance, Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectBleConnectionApi(
      DiscoveryFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }
}
