package com.decawave.argomanager.ioc;

import com.decawave.argo.api.DiscoveryApi;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.ArgoApp_MembersInjector;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.DiscoveryApiBleImpl;
import com.decawave.argomanager.argoapi.ble.DiscoveryApiBleImpl_Factory;
import com.decawave.argomanager.argoapi.ble.PeriodicBleScanner;
import com.decawave.argomanager.argoapi.ble.PeriodicBleScannerImpl;
import com.decawave.argomanager.argoapi.ble.PeriodicBleScannerImpl_Factory;
import com.decawave.argomanager.argoapi.ble.connection.BleConnectionApiImpl;
import com.decawave.argomanager.argoapi.ble.connection.BleConnectionApiImpl_Factory;
import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreter;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreterImpl_Factory;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.DecodeContextManager;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.NetworkModelManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.NetworksNodesStorage;
import com.decawave.argomanager.components.PositionObservationManager;
import com.decawave.argomanager.components.impl.AutoPositioningManagerImpl;
import com.decawave.argomanager.components.impl.AutoPositioningManagerImpl_Factory;
import com.decawave.argomanager.components.impl.BlePresenceApiImpl;
import com.decawave.argomanager.components.impl.BlePresenceApiImpl_Factory;
import com.decawave.argomanager.components.impl.DecodeContextManagerImpl_Factory;
import com.decawave.argomanager.components.impl.DiscoveryManagerImpl;
import com.decawave.argomanager.components.impl.DiscoveryManagerImpl_Factory;
import com.decawave.argomanager.components.impl.ErrorManagerImpl;
import com.decawave.argomanager.components.impl.ErrorManagerImpl_Factory;
import com.decawave.argomanager.components.impl.LocationDataLoggerImpl_Factory;
import com.decawave.argomanager.components.impl.LocationDataObserverImpl;
import com.decawave.argomanager.components.impl.LocationDataObserverImpl_Factory;
import com.decawave.argomanager.components.impl.NetworkModelManagerImpl_Factory;
import com.decawave.argomanager.components.impl.NetworkNodeManagerImpl;
import com.decawave.argomanager.components.impl.NetworkNodeManagerImpl_Factory;
import com.decawave.argomanager.components.impl.NetworkNodeManagerImpl_MembersInjector;
import com.decawave.argomanager.components.impl.NetworksNodesStorageImpl_Factory;
import com.decawave.argomanager.components.impl.PositionObservationManagerImpl;
import com.decawave.argomanager.components.impl.PositionObservationManagerImpl_Factory;
import com.decawave.argomanager.components.impl.UniqueReorderingStack;
import com.decawave.argomanager.components.impl.UniqueReorderingStack_Factory;
import com.decawave.argomanager.debuglog.ApplicationComponentLog;
import com.decawave.argomanager.debuglog.ApplicationComponentLog_MembersInjector;
import com.decawave.argomanager.debuglog.LogBlockStatus;
import com.decawave.argomanager.debuglog.LogBlockStatusImpl_Factory;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.debuglog.LogEntryCollectorImpl;
import com.decawave.argomanager.debuglog.LogEntryCollectorImpl_Factory;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.AppPreferenceAccessorImpl;
import com.decawave.argomanager.prefs.AppPreferenceAccessorImpl_Factory;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.MainActivity_MembersInjector;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment;
import com.decawave.argomanager.ui.dialog.NetworkPickerDialogFragment_MembersInjector;
import com.decawave.argomanager.ui.dialog.RenameNetworkDialogFragment;
import com.decawave.argomanager.ui.dialog.RenameNetworkDialogFragment_MembersInjector;
import com.decawave.argomanager.ui.dialog.TurnOnLocationServiceDialogFragment;
import com.decawave.argomanager.ui.dialog.TurnOnLocationServiceDialogFragment_MembersInjector;
import com.decawave.argomanager.ui.dialog.ZaxisValueDialogFragment;
import com.decawave.argomanager.ui.fragment.ApPreviewFragment;
import com.decawave.argomanager.ui.fragment.ApPreviewFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.AutoPositioningFragment;
import com.decawave.argomanager.ui.fragment.AutoPositioningFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.DebugLogBufferFragment;
import com.decawave.argomanager.ui.fragment.DebugLogBufferFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.DeviceErrorFragment;
import com.decawave.argomanager.ui.fragment.DeviceErrorFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.DiscoveryFragment;
import com.decawave.argomanager.ui.fragment.DiscoveryFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.FirmwareUpdateFragment;
import com.decawave.argomanager.ui.fragment.FirmwareUpdateFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.GridFragment;
import com.decawave.argomanager.ui.fragment.GridFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.InstructionsFragment;
import com.decawave.argomanager.ui.fragment.InstructionsFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.LogBufferFragment;
import com.decawave.argomanager.ui.fragment.LogBufferFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.NodeDetailFragment;
import com.decawave.argomanager.ui.fragment.NodeDetailFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.OverviewFragment;
import com.decawave.argomanager.ui.fragment.OverviewFragment_MembersInjector;
import com.decawave.argomanager.ui.fragment.SettingsFragment;
import com.decawave.argomanager.ui.fragment.SettingsFragment_MembersInjector;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.AndroidPermissionHelperImpl;
import com.decawave.argomanager.util.AndroidPermissionHelperImpl_Factory;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.decawave.argomanager.util.NetworkNodePropertyDecoratorImpl;
import com.decawave.argomanager.util.NetworkNodePropertyDecoratorImpl_Factory;
import com.decawave.argomanager.util.gatt.GattDecoderCache;
import com.decawave.argomanager.util.gatt.GattDecoderCache_Factory;
import dagger.MembersInjector;
import dagger.internal.DoubleCheck;
import dagger.internal.MembersInjectors;
import dagger.internal.Preconditions;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DaggerArgoComponent implements ArgoComponent {
  private Provider<BleAdapter> provideAdapterProvider;

  private Provider<PeriodicBleScannerImpl> periodicBleScannerImplProvider;

  private Provider<PeriodicBleScanner> providedBleScannerProvider;

  private MembersInjector<NetworkNodeManagerImpl> networkNodeManagerImplMembersInjector;

  private Provider<NetworkModelManager> provideNetworkModelRepositoryProvider;

  private Provider<AppPreferenceAccessorImpl> appPreferenceAccessorImplProvider;

  private Provider<AppPreferenceAccessor> provideAppPreferenceAccessorProvider;

  private Provider<LocationDataLogger> provideLocationDataLoggerProvider;

  private Provider<NetworksNodesStorage> provideNetworkModelStorageProvider;

  private Provider<NetworkNodeManagerImpl> networkNodeManagerImplProvider;

  private Provider<NetworkNodeManager> provideNetworkModelManagerProvider;

  private Provider<LogBlockStatus> provideLogEntryCollectorBlockStatusProvider;

  private Provider<DecodeContextManager> providedDecodeContextManagerProvider;

  private Provider<GattDecoderCache> gattDecoderCacheProvider;

  private Provider<BleConnectionApiImpl> bleConnectionApiImplProvider;

  private Provider<BleConnectionApi> provideBleConnectionApiProvider;

  private Provider<DiscoveryApiBleImpl> discoveryApiBleImplProvider;

  private Provider<DiscoveryApi> provideDiscoveryApiProvider;

  private Provider<BlePresenceApiImpl> blePresenceApiImplProvider;

  private Provider<BlePresenceApi> provideBlePresenceApiProvider;

  private Provider<ErrorManagerImpl> errorManagerImplProvider;

  private Provider<ErrorManager> provideErrorManagerProvider;

  private Provider<LogEntryCollectorImpl> logEntryCollectorImplProvider;

  private Provider<LogEntryCollector> provideLogEntryCollectorProvider;

  private MembersInjector<ArgoApp> argoAppMembersInjector;

  private Provider<DiscoveryManagerImpl> discoveryManagerImplProvider;

  private Provider<DiscoveryManager> provideDiscoveryManagerProvider;

  private Provider<AndroidPermissionHelperImpl> androidPermissionHelperImplProvider;

  private Provider<AndroidPermissionHelper> provideAndroidPermissionHelperProvider;

  private MembersInjector<MainActivity> mainActivityMembersInjector;

  private MembersInjector<DiscoveryFragment> discoveryFragmentMembersInjector;

  private MembersInjector<NodeDetailFragment> nodeDetailFragmentMembersInjector;

  private MembersInjector<DebugLogBufferFragment> debugLogBufferFragmentMembersInjector;

  private Provider<LocationDataObserverImpl> locationDataObserverImplProvider;

  private Provider<LocationDataObserver> provideLocationDataObserverProvider;

  private Provider<PositionObservationManagerImpl> positionObservationManagerImplProvider;

  private Provider<PositionObservationManager> provideObservationManagerProvider;

  private MembersInjector<GridFragment> gridFragmentMembersInjector;

  private Provider<SignalStrengthInterpreter> provideSignalStrengthInterpreterProvider;

  private Provider<NetworkNodePropertyDecoratorImpl> networkNodePropertyDecoratorImplProvider;

  private Provider<NetworkNodePropertyDecorator> providePropertyDecoratorProvider;

  private Provider<AutoPositioningManagerImpl> autoPositioningManagerImplProvider;

  private Provider<AutoPositioningManager> provideAutoPositioningManagerProvider;

  private MembersInjector<OverviewFragment> overviewFragmentMembersInjector;

  private MembersInjector<NetworkPickerDialogFragment> networkPickerDialogFragmentMembersInjector;

  private MembersInjector<RenameNetworkDialogFragment> renameNetworkDialogFragmentMembersInjector;

  private MembersInjector<DeviceErrorFragment> deviceErrorFragmentMembersInjector;

  private MembersInjector<TurnOnLocationServiceDialogFragment>
      turnOnLocationServiceDialogFragmentMembersInjector;

  private MembersInjector<LogBufferFragment> logBufferFragmentMembersInjector;

  private MembersInjector<DeviceDebugConsoleFragment> deviceDebugConsoleFragmentMembersInjector;

  private MembersInjector<ApplicationComponentLog> applicationComponentLogMembersInjector;

  private MembersInjector<FirmwareUpdateFragment> firmwareUpdateFragmentMembersInjector;

  private MembersInjector<AutoPositioningFragment> autoPositioningFragmentMembersInjector;

  private MembersInjector<SettingsFragment> settingsFragmentMembersInjector;

  private MembersInjector<ApPreviewFragment> apPreviewFragmentMembersInjector;

  private MembersInjector<InstructionsFragment> instructionsFragmentMembersInjector;

  private DaggerArgoComponent(Builder builder) {
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ArgoComponent create() {
    return builder().build();
  }

  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {

    this.provideAdapterProvider =
        DoubleCheck.provider(
            ArgoDependencyProvider_ProvideAdapterFactory.create(builder.argoDependencyProvider));

    this.periodicBleScannerImplProvider =
        PeriodicBleScannerImpl_Factory.create(provideAdapterProvider);

    this.providedBleScannerProvider =
        DoubleCheck.provider((Provider) periodicBleScannerImplProvider);

    this.networkNodeManagerImplMembersInjector = NetworkNodeManagerImpl_MembersInjector.create();

    this.provideNetworkModelRepositoryProvider =
        DoubleCheck.provider((Provider) NetworkModelManagerImpl_Factory.create());

    this.appPreferenceAccessorImplProvider =
        DoubleCheck.provider(AppPreferenceAccessorImpl_Factory.create());

    this.provideAppPreferenceAccessorProvider =
        DoubleCheck.provider((Provider) appPreferenceAccessorImplProvider);

    this.provideLocationDataLoggerProvider =
        DoubleCheck.provider((Provider) LocationDataLoggerImpl_Factory.create());

    this.provideNetworkModelStorageProvider =
        DoubleCheck.provider((Provider) NetworksNodesStorageImpl_Factory.create());

    this.networkNodeManagerImplProvider =
        NetworkNodeManagerImpl_Factory.create(
            networkNodeManagerImplMembersInjector,
            provideNetworkModelRepositoryProvider,
            provideAppPreferenceAccessorProvider,
            provideLocationDataLoggerProvider,
            provideNetworkModelStorageProvider,
            UniqueReorderingStack_Factory.<Short>create());

    this.provideNetworkModelManagerProvider =
        DoubleCheck.provider((Provider) networkNodeManagerImplProvider);

    this.provideLogEntryCollectorBlockStatusProvider =
        DoubleCheck.provider((Provider) LogBlockStatusImpl_Factory.create());

    this.providedDecodeContextManagerProvider =
        DoubleCheck.provider((Provider) DecodeContextManagerImpl_Factory.create());

    this.gattDecoderCacheProvider =
        GattDecoderCache_Factory.create(providedDecodeContextManagerProvider);

    this.bleConnectionApiImplProvider =
        BleConnectionApiImpl_Factory.create(
            provideAdapterProvider,
            provideNetworkModelManagerProvider,
            provideLogEntryCollectorBlockStatusProvider,
            provideLocationDataLoggerProvider,
            gattDecoderCacheProvider);

    this.provideBleConnectionApiProvider =
        DoubleCheck.provider((Provider) bleConnectionApiImplProvider);

    this.discoveryApiBleImplProvider =
        DiscoveryApiBleImpl_Factory.create(
            providedBleScannerProvider, provideBleConnectionApiProvider, gattDecoderCacheProvider);

    this.provideDiscoveryApiProvider = DoubleCheck.provider((Provider) discoveryApiBleImplProvider);

    this.blePresenceApiImplProvider =
        DoubleCheck.provider(
            BlePresenceApiImpl_Factory.create(
                provideDiscoveryApiProvider,
                provideNetworkModelManagerProvider,
                provideBleConnectionApiProvider));

    this.provideBlePresenceApiProvider =
        DoubleCheck.provider((Provider) blePresenceApiImplProvider);

    this.errorManagerImplProvider =
        ErrorManagerImpl_Factory.create(
            provideBleConnectionApiProvider, provideAppPreferenceAccessorProvider);

    this.provideErrorManagerProvider = DoubleCheck.provider((Provider) errorManagerImplProvider);

    this.logEntryCollectorImplProvider =
        LogEntryCollectorImpl_Factory.create(
            provideErrorManagerProvider, provideLogEntryCollectorBlockStatusProvider);

    this.provideLogEntryCollectorProvider =
        DoubleCheck.provider((Provider) logEntryCollectorImplProvider);

    this.argoAppMembersInjector =
        ArgoApp_MembersInjector.create(
            provideBlePresenceApiProvider,
            provideNetworkModelManagerProvider,
            provideLogEntryCollectorProvider,
            UniqueReorderingStack_Factory.<Short>create());

    this.discoveryManagerImplProvider =
        DiscoveryManagerImpl_Factory.create(
            provideDiscoveryApiProvider,
            provideNetworkModelManagerProvider,
            provideBlePresenceApiProvider);

    this.provideDiscoveryManagerProvider =
        DoubleCheck.provider((Provider) discoveryManagerImplProvider);

    this.androidPermissionHelperImplProvider =
        AndroidPermissionHelperImpl_Factory.create(provideAdapterProvider);

    this.provideAndroidPermissionHelperProvider =
        DoubleCheck.provider((Provider) androidPermissionHelperImplProvider);

    this.mainActivityMembersInjector =
        MainActivity_MembersInjector.create(
            provideNetworkModelManagerProvider,
            provideDiscoveryManagerProvider,
            provideAppPreferenceAccessorProvider,
            provideAndroidPermissionHelperProvider);

    this.discoveryFragmentMembersInjector =
        DiscoveryFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryApiProvider,
            provideErrorManagerProvider,
            provideDiscoveryManagerProvider,
            provideNetworkModelManagerProvider,
            provideAndroidPermissionHelperProvider,
            provideBleConnectionApiProvider);

    this.nodeDetailFragmentMembersInjector =
        NodeDetailFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryManagerProvider,
            provideNetworkModelManagerProvider,
            provideBleConnectionApiProvider);

    this.debugLogBufferFragmentMembersInjector =
        DebugLogBufferFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider, provideLogEntryCollectorProvider);

    this.locationDataObserverImplProvider =
        DoubleCheck.provider(
            LocationDataObserverImpl_Factory.create(
                provideBleConnectionApiProvider,
                provideDiscoveryManagerProvider,
                provideNetworkModelManagerProvider,
                provideBlePresenceApiProvider,
                provideAppPreferenceAccessorProvider));

    this.provideLocationDataObserverProvider =
        DoubleCheck.provider((Provider) locationDataObserverImplProvider);

    this.positionObservationManagerImplProvider =
        PositionObservationManagerImpl_Factory.create(provideLocationDataObserverProvider);

    this.provideObservationManagerProvider =
        DoubleCheck.provider((Provider) positionObservationManagerImplProvider);

    this.gridFragmentMembersInjector =
        GridFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryApiProvider,
            provideErrorManagerProvider,
            provideNetworkModelManagerProvider,
            provideObservationManagerProvider,
            provideDiscoveryManagerProvider,
            provideAndroidPermissionHelperProvider,
            provideBlePresenceApiProvider);

    this.provideSignalStrengthInterpreterProvider =
        DoubleCheck.provider((Provider) SignalStrengthInterpreterImpl_Factory.create());

    this.networkNodePropertyDecoratorImplProvider =
        NetworkNodePropertyDecoratorImpl_Factory.create(provideAppPreferenceAccessorProvider);

    this.providePropertyDecoratorProvider =
        DoubleCheck.provider((Provider) networkNodePropertyDecoratorImplProvider);

    this.autoPositioningManagerImplProvider =
        AutoPositioningManagerImpl_Factory.create(
            provideBleConnectionApiProvider, provideNetworkModelManagerProvider);

    this.provideAutoPositioningManagerProvider =
        DoubleCheck.provider((Provider) autoPositioningManagerImplProvider);

    this.overviewFragmentMembersInjector =
        OverviewFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryApiProvider,
            provideErrorManagerProvider,
            provideNetworkModelManagerProvider,
            provideDiscoveryManagerProvider,
            provideSignalStrengthInterpreterProvider,
            providePropertyDecoratorProvider,
            provideAndroidPermissionHelperProvider,
            provideBlePresenceApiProvider,
            provideBleConnectionApiProvider,
            provideAutoPositioningManagerProvider,
            provideLocationDataObserverProvider);

    this.networkPickerDialogFragmentMembersInjector =
        NetworkPickerDialogFragment_MembersInjector.create(provideNetworkModelManagerProvider);

    this.renameNetworkDialogFragmentMembersInjector =
        RenameNetworkDialogFragment_MembersInjector.create(provideNetworkModelManagerProvider);

    this.deviceErrorFragmentMembersInjector =
        DeviceErrorFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideErrorManagerProvider,
            provideNetworkModelManagerProvider);

    this.turnOnLocationServiceDialogFragmentMembersInjector =
        TurnOnLocationServiceDialogFragment_MembersInjector.create(
            provideAndroidPermissionHelperProvider);

    this.logBufferFragmentMembersInjector =
        LogBufferFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider, provideLogEntryCollectorProvider);

    this.deviceDebugConsoleFragmentMembersInjector =
        DeviceDebugConsoleFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideLogEntryCollectorProvider,
            provideBleConnectionApiProvider,
            provideNetworkModelManagerProvider,
            provideDiscoveryManagerProvider,
            provideLocationDataLoggerProvider);

    this.applicationComponentLogMembersInjector =
        ApplicationComponentLog_MembersInjector.create(provideLogEntryCollectorProvider);

    this.firmwareUpdateFragmentMembersInjector =
        FirmwareUpdateFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryApiProvider,
            provideErrorManagerProvider,
            provideDiscoveryManagerProvider,
            provideNetworkModelManagerProvider,
            provideAndroidPermissionHelperProvider,
            provideBlePresenceApiProvider,
            providePropertyDecoratorProvider,
            provideBleConnectionApiProvider);

    this.autoPositioningFragmentMembersInjector =
        AutoPositioningFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideDiscoveryApiProvider,
            provideErrorManagerProvider,
            provideAutoPositioningManagerProvider,
            provideNetworkModelManagerProvider,
            provideAndroidPermissionHelperProvider,
            provideBleConnectionApiProvider);

    this.settingsFragmentMembersInjector =
        SettingsFragment_MembersInjector.create(provideAppPreferenceAccessorProvider);

    this.apPreviewFragmentMembersInjector =
        ApPreviewFragment_MembersInjector.create(
            provideAppPreferenceAccessorProvider,
            provideNetworkModelManagerProvider,
            provideAutoPositioningManagerProvider);

    this.instructionsFragmentMembersInjector =
        InstructionsFragment_MembersInjector.create(provideAppPreferenceAccessorProvider);
  }

  @Override
  public void inject(ArgoApp argoApp) {
    argoAppMembersInjector.injectMembers(argoApp);
  }

  @Override
  public void inject(MainActivity mainActivity) {
    mainActivityMembersInjector.injectMembers(mainActivity);
  }

  @Override
  public void inject(DiscoveryFragment fragment) {
    discoveryFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(NodeDetailFragment fragment) {
    nodeDetailFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(DebugLogBufferFragment fragment) {
    debugLogBufferFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(GridFragment fragment) {
    gridFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(OverviewFragment fragment) {
    overviewFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(NetworkPickerDialogFragment networkPickerDialogFragment) {
    networkPickerDialogFragmentMembersInjector.injectMembers(networkPickerDialogFragment);
  }

  @Override
  public void inject(RenameNetworkDialogFragment renameNetworkDialogFragment) {
    renameNetworkDialogFragmentMembersInjector.injectMembers(renameNetworkDialogFragment);
  }

  @Override
  public void inject(DeviceErrorFragment deviceErrorFragment) {
    deviceErrorFragmentMembersInjector.injectMembers(deviceErrorFragment);
  }

  @Override
  public void inject(TurnOnLocationServiceDialogFragment fragment) {
    turnOnLocationServiceDialogFragmentMembersInjector.injectMembers(fragment);
  }

  @Override
  public void inject(LogBufferFragment logBufferFragment) {
    logBufferFragmentMembersInjector.injectMembers(logBufferFragment);
  }

  @Override
  public void inject(DeviceDebugConsoleFragment deviceDebugConsoleFragment) {
    deviceDebugConsoleFragmentMembersInjector.injectMembers(deviceDebugConsoleFragment);
  }

  @Override
  public void inject(ApplicationComponentLog applicationComponentLog) {
    applicationComponentLogMembersInjector.injectMembers(applicationComponentLog);
  }

  @Override
  public void inject(FirmwareUpdateFragment firmwareUpdateFragment) {
    firmwareUpdateFragmentMembersInjector.injectMembers(firmwareUpdateFragment);
  }

  @Override
  public void inject(AutoPositioningFragment autoPositioningFragment) {
    autoPositioningFragmentMembersInjector.injectMembers(autoPositioningFragment);
  }

  @Override
  public void inject(SettingsFragment settingsFragment) {
    settingsFragmentMembersInjector.injectMembers(settingsFragment);
  }

  @Override
  public void inject(ZaxisValueDialogFragment zaxisValueDialogFragment) {
    MembersInjectors.<ZaxisValueDialogFragment>noOp().injectMembers(zaxisValueDialogFragment);
  }

  @Override
  public void inject(ApPreviewFragment apPreviewFragment) {
    apPreviewFragmentMembersInjector.injectMembers(apPreviewFragment);
  }

  @Override
  public void inject(InstructionsFragment instructionsFragment) {
    instructionsFragmentMembersInjector.injectMembers(instructionsFragment);
  }

  @Override
  public UniqueReorderingStack<Short> getActiveNetworkIdStack() {
    return new UniqueReorderingStack<Short>();
  }

  @Override
  public BleConnectionApi getBleConnectionApi() {
    return provideBleConnectionApiProvider.get();
  }

  @Override
  public DiscoveryManager getDiscoveryManager() {
    return provideDiscoveryManagerProvider.get();
  }

  @Override
  public AndroidPermissionHelper getPermissionHelper() {
    return provideAndroidPermissionHelperProvider.get();
  }

  @Override
  public AutoPositioningManager getAutoPositioningManager() {
    return provideAutoPositioningManagerProvider.get();
  }

  @Override
  public NetworkNodeManager getNetworkNodeManager() {
    return provideNetworkModelManagerProvider.get();
  }

  @Override
  public GattDecoderCache getGattDecoderCache() {
    return new GattDecoderCache(providedDecodeContextManagerProvider.get());
  }

  public static final class Builder {
    private ArgoDependencyProvider argoDependencyProvider;

    private Builder() {}

    public ArgoComponent build() {
      if (argoDependencyProvider == null) {
        this.argoDependencyProvider = new ArgoDependencyProvider();
      }
      return new DaggerArgoComponent(this);
    }

    public Builder argoDependencyProvider(ArgoDependencyProvider argoDependencyProvider) {
      this.argoDependencyProvider = Preconditions.checkNotNull(argoDependencyProvider);
      return this;
    }
  }
}
