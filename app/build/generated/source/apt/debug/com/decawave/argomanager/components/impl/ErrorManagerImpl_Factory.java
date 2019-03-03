package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class ErrorManagerImpl_Factory implements Factory<ErrorManagerImpl> {
  private final Provider<BleConnectionApi> bleConnectionApiProvider;

  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public ErrorManagerImpl_Factory(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert bleConnectionApiProvider != null;
    this.bleConnectionApiProvider = bleConnectionApiProvider;
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  @Override
  public ErrorManagerImpl get() {
    return new ErrorManagerImpl(
        bleConnectionApiProvider.get(), appPreferenceAccessorProvider.get());
  }

  public static Factory<ErrorManagerImpl> create(
      Provider<BleConnectionApi> bleConnectionApiProvider,
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new ErrorManagerImpl_Factory(bleConnectionApiProvider, appPreferenceAccessorProvider);
  }

  /** Proxies {@link ErrorManagerImpl#ErrorManagerImpl(BleConnectionApi, AppPreferenceAccessor)}. */
  public static ErrorManagerImpl newErrorManagerImpl(
      BleConnectionApi bleConnectionApi, AppPreferenceAccessor appPreferenceAccessor) {
    return new ErrorManagerImpl(bleConnectionApi, appPreferenceAccessor);
  }
}
