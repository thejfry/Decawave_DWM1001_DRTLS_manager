package com.decawave.argomanager.util;

import com.decawave.argomanager.ble.BleAdapter;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class AndroidPermissionHelperImpl_Factory
    implements Factory<AndroidPermissionHelperImpl> {
  private final Provider<BleAdapter> bleAdapterProvider;

  public AndroidPermissionHelperImpl_Factory(Provider<BleAdapter> bleAdapterProvider) {
    assert bleAdapterProvider != null;
    this.bleAdapterProvider = bleAdapterProvider;
  }

  @Override
  public AndroidPermissionHelperImpl get() {
    return new AndroidPermissionHelperImpl(bleAdapterProvider.get());
  }

  public static Factory<AndroidPermissionHelperImpl> create(
      Provider<BleAdapter> bleAdapterProvider) {
    return new AndroidPermissionHelperImpl_Factory(bleAdapterProvider);
  }

  /** Proxies {@link AndroidPermissionHelperImpl#AndroidPermissionHelperImpl(BleAdapter)}. */
  public static AndroidPermissionHelperImpl newAndroidPermissionHelperImpl(BleAdapter bleAdapter) {
    return new AndroidPermissionHelperImpl(bleAdapter);
  }
}
