package com.decawave.argomanager.ui.fragment;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreter;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.LocationDataObserver;
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
public final class OverviewFragment_MembersInjector implements MembersInjector<OverviewFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<DiscoveryApi> discoveryApiProvider;

  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<SignalStrengthInterpreter> signalStrengthInterpreterProvider;

  private final Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider;

  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  private final Provider<BlePresenceApi> presenceApiProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<AutoPositioningManager> autoPositioningManagerProvider;

  private final Provider<LocationDataObserver> locationDataObserverProvider;

  public OverviewFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<SignalStrengthInterpreter> signalStrengthInterpreterProvider,
      Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider,
      Provider<LocationDataObserver> locationDataObserverProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert discoveryApiProvider != null;
    this.discoveryApiProvider = discoveryApiProvider;
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert discoveryManagerProvider != null;
    this.discoveryManagerProvider = discoveryManagerProvider;
    assert signalStrengthInterpreterProvider != null;
    this.signalStrengthInterpreterProvider = signalStrengthInterpreterProvider;
    assert propertyDecoratorProvider != null;
    this.propertyDecoratorProvider = propertyDecoratorProvider;
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
    assert presenceApiProvider != null;
    this.presenceApiProvider = presenceApiProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert autoPositioningManagerProvider != null;
    this.autoPositioningManagerProvider = autoPositioningManagerProvider;
    assert locationDataObserverProvider != null;
    this.locationDataObserverProvider = locationDataObserverProvider;
  }

  public static MembersInjector<OverviewFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<DiscoveryApi> discoveryApiProvider,
      Provider<ErrorManager> errorManagerProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<SignalStrengthInterpreter> signalStrengthInterpreterProvider,
      Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider,
      Provider<AndroidPermissionHelper> permissionHelperProvider,
      Provider<BlePresenceApi> presenceApiProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider,
      Provider<LocationDataObserver> locationDataObserverProvider) {
    return new OverviewFragment_MembersInjector(
        appPreferenceAccessorProvider,
        discoveryApiProvider,
        errorManagerProvider,
        networkNodeManagerProvider,
        discoveryManagerProvider,
        signalStrengthInterpreterProvider,
        propertyDecoratorProvider,
        permissionHelperProvider,
        presenceApiProvider,
        bleConnectionApiProvider,
        autoPositioningManagerProvider,
        locationDataObserverProvider);
  }

  @Override
  public void injectMembers(OverviewFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((DiscoveryProgressAwareFragment) instance).discoveryApi = discoveryApiProvider.get();
    ((DiscoveryProgressAwareFragment) instance).errorManager = errorManagerProvider.get();
    ((DiscoveryProgressAwareFragment) instance).appPreferenceAccessor =
        appPreferenceAccessorProvider.get();
    ((MainScreenFragment) instance).networkNodeManager = networkNodeManagerProvider.get();
    instance.discoveryManager = discoveryManagerProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.signalStrengthInterpreter = signalStrengthInterpreterProvider.get();
    instance.propertyDecorator = propertyDecoratorProvider.get();
    instance.errorManager = errorManagerProvider.get();
    instance.permissionHelper = permissionHelperProvider.get();
    instance.presenceApi = presenceApiProvider.get();
    instance.bleConnectionApi = bleConnectionApiProvider.get();
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.locationDataObserver = locationDataObserverProvider.get();
  }

  public static void injectDiscoveryManager(
      OverviewFragment instance, Provider<DiscoveryManager> discoveryManagerProvider) {
    instance.discoveryManager = discoveryManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      OverviewFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectSignalStrengthInterpreter(
      OverviewFragment instance,
      Provider<SignalStrengthInterpreter> signalStrengthInterpreterProvider) {
    instance.signalStrengthInterpreter = signalStrengthInterpreterProvider.get();
  }

  public static void injectPropertyDecorator(
      OverviewFragment instance, Provider<NetworkNodePropertyDecorator> propertyDecoratorProvider) {
    instance.propertyDecorator = propertyDecoratorProvider.get();
  }

  public static void injectErrorManager(
      OverviewFragment instance, Provider<ErrorManager> errorManagerProvider) {
    instance.errorManager = errorManagerProvider.get();
  }

  public static void injectPermissionHelper(
      OverviewFragment instance, Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectPresenceApi(
      OverviewFragment instance, Provider<BlePresenceApi> presenceApiProvider) {
    instance.presenceApi = presenceApiProvider.get();
  }

  public static void injectBleConnectionApi(
      OverviewFragment instance, Provider<BleConnectionApi> bleConnectionApiProvider) {
    instance.bleConnectionApi = bleConnectionApiProvider.get();
  }

  public static void injectAutoPositioningManager(
      OverviewFragment instance, Provider<AutoPositioningManager> autoPositioningManagerProvider) {
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      OverviewFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectLocationDataObserver(
      OverviewFragment instance, Provider<LocationDataObserver> locationDataObserverProvider) {
    instance.locationDataObserver = locationDataObserverProvider.get();
  }
}
