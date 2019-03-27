package com.decawave.argomanager.ioc;

import com.decawave.argomanager.ble.BleAdapter;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class ArgoDependencyProvider_ProvideAdapterFactory implements Factory<BleAdapter> {
  private final ArgoDependencyProvider module;

  public ArgoDependencyProvider_ProvideAdapterFactory(ArgoDependencyProvider module) {
    assert module != null;
    this.module = module;
  }

  @Override
  public BleAdapter get() {
    return Preconditions.checkNotNull(
        module.provideAdapter(), "Cannot return null from a non-@Nullable @Provides method");
  }

  public static Factory<BleAdapter> create(ArgoDependencyProvider module) {
    return new ArgoDependencyProvider_ProvideAdapterFactory(module);
  }

  /** Proxies {@link ArgoDependencyProvider#provideAdapter()}. */
  public static BleAdapter proxyProvideAdapter(ArgoDependencyProvider instance) {
    return instance.provideAdapter();
  }
}
