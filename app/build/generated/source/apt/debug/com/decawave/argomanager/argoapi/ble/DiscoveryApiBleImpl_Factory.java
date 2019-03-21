package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.util.gatt.GattDecoderCache;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DiscoveryApiBleImpl_Factory implements Factory<DiscoveryApiBleImpl> {
  private final Provider<PeriodicBleScanner> bleScannerProvider;

  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<GattDecoderCache> gattDecoderCacheProvider;

  public DiscoveryApiBleImpl_Factory(
      Provider<PeriodicBleScanner> bleScannerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<GattDecoderCache> gattDecoderCacheProvider) {
    assert bleScannerProvider != null;
    this.bleScannerProvider = bleScannerProvider;
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert gattDecoderCacheProvider != null;
    this.gattDecoderCacheProvider = gattDecoderCacheProvider;
  }

  @Override
  public DiscoveryApiBleImpl get() {
    return new DiscoveryApiBleImpl(
        bleScannerProvider.get(), bleConnectionApiProvider.get(), gattDecoderCacheProvider.get());
  }

  public static Factory<DiscoveryApiBleImpl> create(
      Provider<PeriodicBleScanner> bleScannerProvider,
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<GattDecoderCache> gattDecoderCacheProvider) {
    return new DiscoveryApiBleImpl_Factory(
        bleScannerProvider, bleConnectionApiProvider, gattDecoderCacheProvider);
  }

  /**
   * Proxies {@link DiscoveryApiBleImpl#DiscoveryApiBleImpl(PeriodicBleScanner, BleConnectionApi,
   * GattDecoderCache)}.
   */
  public static DiscoveryApiBleImpl newDiscoveryApiBleImpl(
      PeriodicBleScanner bleScanner,
      BleConnectionApi bleConnectionApi,
      GattDecoderCache gattDecoderCache) {
    return new DiscoveryApiBleImpl(bleScanner, bleConnectionApi, gattDecoderCache);
  }
}
