package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.ble.BleAdapter;
import com.decawave.argomanager.components.LocationDataLogger;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.debuglog.LogBlockStatus;
import com.decawave.argomanager.util.gatt.GattDecoderCache;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class BleConnectionApiImpl_Factory implements Factory<BleConnectionApiImpl> {
  private final Provider<BleAdapter> bleAdapterProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<LogBlockStatus> logBlockStatusProvider;

  private final Provider<LocationDataLogger> locationDataLoggerProvider;

  private final Provider<GattDecoderCache> gattDecoderCacheProvider;

  public BleConnectionApiImpl_Factory(
      Provider<BleAdapter> bleAdapterProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<LogBlockStatus> logBlockStatusProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider,
      Provider<GattDecoderCache> gattDecoderCacheProvider) {
    assert bleAdapterProvider != null;
    this.bleAdapterProvider = bleAdapterProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert logBlockStatusProvider != null;
    this.logBlockStatusProvider = logBlockStatusProvider;
    assert locationDataLoggerProvider != null;
    this.locationDataLoggerProvider = locationDataLoggerProvider;
    assert gattDecoderCacheProvider != null;
    this.gattDecoderCacheProvider = gattDecoderCacheProvider;
  }

  @Override
  public BleConnectionApiImpl get() {
    return new BleConnectionApiImpl(
        bleAdapterProvider.get(),
        networkNodeManagerProvider.get(),
        logBlockStatusProvider.get(),
        locationDataLoggerProvider.get(),
        gattDecoderCacheProvider.get());
  }

  public static Factory<BleConnectionApiImpl> create(
      Provider<BleAdapter> bleAdapterProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<LogBlockStatus> logBlockStatusProvider,
      Provider<LocationDataLogger> locationDataLoggerProvider,
      Provider<GattDecoderCache> gattDecoderCacheProvider) {
    return new BleConnectionApiImpl_Factory(
        bleAdapterProvider,
        networkNodeManagerProvider,
        logBlockStatusProvider,
        locationDataLoggerProvider,
        gattDecoderCacheProvider);
  }

  /**
   * Proxies {@link BleConnectionApiImpl#BleConnectionApiImpl(BleAdapter, NetworkNodeManager,
   * LogBlockStatus, LocationDataLogger, GattDecoderCache)}.
   */
  public static BleConnectionApiImpl newBleConnectionApiImpl(
      BleAdapter bleAdapter,
      NetworkNodeManager networkNodeManager,
      LogBlockStatus logBlockStatus,
      LocationDataLogger locationDataLogger,
      GattDecoderCache gattDecoderCache) {
    return new BleConnectionApiImpl(
        bleAdapter, networkNodeManager, logBlockStatus, locationDataLogger, gattDecoderCache);
  }
}
