package com.decawave.argomanager.util;

import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworkNodePropertyDecoratorImpl_Factory
    implements Factory<NetworkNodePropertyDecoratorImpl> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public NetworkNodePropertyDecoratorImpl_Factory(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  @Override
  public NetworkNodePropertyDecoratorImpl get() {
    return new NetworkNodePropertyDecoratorImpl(appPreferenceAccessorProvider.get());
  }

  public static Factory<NetworkNodePropertyDecoratorImpl> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new NetworkNodePropertyDecoratorImpl_Factory(appPreferenceAccessorProvider);
  }

  /**
   * Proxies {@link
   * NetworkNodePropertyDecoratorImpl#NetworkNodePropertyDecoratorImpl(AppPreferenceAccessor)}.
   */
  public static NetworkNodePropertyDecoratorImpl newNetworkNodePropertyDecoratorImpl(
      AppPreferenceAccessor appPreferenceAccessor) {
    return new NetworkNodePropertyDecoratorImpl(appPreferenceAccessor);
  }
}
