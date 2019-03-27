package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.AutoPositioningManager;
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
public final class AutoPositioningFragment_MembersInjector
    implements MembersInjector<AutoPositioningFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<AutoPositioningManager> autoPositioningManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  public AutoPositioningFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert autoPositioningManagerProvider != null;
    this.autoPositioningManagerProvider = autoPositioningManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
  }

  public static MembersInjector<AutoPositioningFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    return new AutoPositioningFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        autoPositioningManagerProvider,
        networkNodeManagerProvider,
        permissionHelperProvider,
        bleConnectionApiProvider);
  }

  @Override
  public void injectMembers(AutoPositioningFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((DiscoveryProgressAwareFragment) instance).discoveryApi = discoveryApiProvider.get();
    ((DiscoveryProgressAwareFragment) instance).errorManager = errorManagerProvider.get();
    ((DiscoveryProgressAwareFragment) instance).appPreferenceAccessor =
        appPreferenceAccessorProvider.get();
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.permissionHelper = permissionHelperProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectAutoPositioningManager(
      AutoPositioningFragment instance,
      Provider<AutoPositioningManager> autoPositioningManagerProvider) {
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      AutoPositioningFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectPermissionHelper(
      AutoPositioningFragment instance,
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectBleConnectionApi(
      AutoPositioningFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      AutoPositioningFragment instance,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
