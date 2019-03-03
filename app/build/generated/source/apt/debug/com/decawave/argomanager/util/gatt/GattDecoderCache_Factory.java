package com.decawave.argomanager.util.gatt;

import com.decawave.argomanager.components.DecodeContextManager;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class GattDecoderCache_Factory implements Factory<GattDecoderCache> {
  private final Provider<DecodeContextManager> decodeContextManagerProvider;

  public GattDecoderCache_Factory(Provider<DecodeContextManager> decodeContextManagerProvider) {
    assert decodeContextManagerProvider != null;
    this.decodeContextManagerProvider = decodeContextManagerProvider;
  }

  @Override
  public GattDecoderCache get() {
    return new GattDecoderCache(decodeContextManagerProvider.get());
  }

  public static Factory<GattDecoderCache> create(
      Provider<DecodeContextManager> decodeContextManagerProvider) {
    return new GattDecoderCache_Factory(decodeContextManagerProvider);
  }
}
