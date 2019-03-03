package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.PositionObservationManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class GridFragment_MembersInjector implements MembersInjector<GridFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<PositionObservationManager> positionObservationManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  private final Provider<BlePresenceApi> presenceApiProvider;

  public GridFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<PositionObservationManager> positionObservationManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert positionObservationManagerProvider != null;
    this.positionObservationManagerProvider = positionObservationManagerProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
    assert presenceApiProvider != null;
    this.presenceApiProvider = presenceApiProvider;
  }

  public static MembersInjector<GridFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<PositionObservationManager> positionObservationManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider) {
    return new GridFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        networkNodeManagerProvider,
        positionObservationManagerProvider,
        discoveryManagerProvider,
        permissionHelperProvider,
        presenceApiProvider);
  }

  @Override
  public void injectMembers(GridFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((DiscoveryProgressAwareFragment) instance).discoveryApi = discoveryApiProvider.get();
    ((DiscoveryProgressAwareFragment) instance).errorManager = errorManagerProvider.get();
    ((DiscoveryProgressAwareFragment) instance).appPreferenceAccessor =
        appPreferenceAccessorProvider.get();
    ((MainScreenFragment) instance).networkNodeManager = networkNodeManagerProvider.get();
    instance.positionObservationManager = positionObservationManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.permissionHelper = permissionHelperProvider.get();
    instance.presenceApi = presenceApiProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectPositionObservationManager(
      GridFragment instance,
      Provider<PositionObservationManager> positionObservationManagerProvider) {
    instance.positionObservationManager = positionObservationManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      GridFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectDiscoveryManager(
      GridFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectPermissionHelper(
      GridFragment instance, Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectPresenceApi(
      GridFragment instance, Provider<BlePresenceApi> presenceApiProvider) {
    instance.presenceApi = presenceApiProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      GridFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
