package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NodeDetailFragment_MembersInjector
    implements MembersInjector<NodeDetailFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  public NodeDetailFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
  }

  public static MembersInjector<NodeDetailFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider) {
    return new NodeDetailFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryManagerProvider,
        networkNodeManagerProvider,
        bleConnectionApiProvider);
  }

  @Override
  public void injectMembers(NodeDetailFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectDiscoveryManager(
      NodeDetailFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      NodeDetailFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectBleConnectionApi(
      NodeDetailFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      NodeDetailFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
