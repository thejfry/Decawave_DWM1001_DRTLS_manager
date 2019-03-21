package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class FirmwareUpdateFragment_MembersInjector
    implements MembersInjector<FirmwareUpdateFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  private final Provider<BlePresenceApi> presenceApiProvider;

  private final Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  public FirmwareUpdateFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider,
      Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider,
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
    assert presenceApiProvider != null;
    this.presenceApiProvider = presenceApiProvider;
    assert propertyDecoratorProvider != null;
    this.propertyDecoratorProvider = propertyDecoratorProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
  }

  public static MembersInjector<FirmwareUpdateFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider,
      Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    return new FirmwareUpdateFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        discoveryManagerProvider,
        networkNodeManagerProvider,
        permissionHelperProvider,
        presenceApiProvider,
        propertyDecoratorProvider,
        bleConnectionApiProvider);
  }

  @Override
  public void injectMembers(FirmwareUpdateFragment instance) {
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
    instance.permissionHelper = permissionHelperProvider.get();
    instance.presenceApi = presenceApiProvider.get();
    instance.propertyDecorator = propertyDecoratorProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectDiscoveryManager(
      FirmwareUpdateFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      FirmwareUpdateFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectPermissionHelper(
      FirmwareUpdateFragment instance, Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectPresenceApi(
      FirmwareUpdateFragment instance, Provider<BlePresenceApi> presenceApiProvider) {
    instance.presenceApi = presenceApiProvider.get();
  }

  public static void injectPropertyDecorator(
      FirmwareUpdateFragment instance,
      Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider) {
    instance.propertyDecorator = propertyDecoratorProvider.get();
  }

  public static void injectBleConnectionApi(
      FirmwareUpdateFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }
}
