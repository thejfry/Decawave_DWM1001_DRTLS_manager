package com.decawave.argomanager.argoapi.ble;

import com.decawave.argomanager.ble.BleAdapter;
import dagger.Lazy;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class PeriodicBleScannerImpl_Factory implements Factory<PeriodicBleScannerImpl> {
  private final Provider<BleAdapter> bleAdapterSupplierProvider;

  public PeriodicBleScannerImpl_Factory(Provider<BleAdapter> bleAdapterSupplierProvider) {
    assert bleAdapterSupplierProvider != null;
    this.bleAdapterSupplierProvider = bleAdapterSupplierProvider;
  }

  @Override
  public PeriodicBleScannerImpl get() {
    return new PeriodicBleScannerImpl(DoubleCheck.lazy(bleAdapterSupplierProvider));
  }

  public static Factory<PeriodicBleScannerImpl> create(
      Provider<BleAdapter> bleAdapterSupplierProvider) {
    return new PeriodicBleScannerImpl_Factory(bleAdapterSupplierProvider);
  }

  /** Proxies {@link PeriodicBleScannerImpl#PeriodicBleScannerImpl(Lazy)}. */
  public static PeriodicBleScannerImpl newPeriodicBleScannerImpl(
      Lazy<BleAdapter> bleAdapterSupplier) {
    return new PeriodicBleScannerImpl(bleAdapterSupplier);
  }
}
