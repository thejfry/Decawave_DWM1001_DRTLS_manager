package com.decawave.argomanager.prefs;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class AppPreferenceAccessorImpl_Factory implements Factory<AppPreferenceAccessorImpl> {
  private static final AppPreferenceAccessorImpl_Factory INSTANCE =
      new AppPreferenceAccessorImpl_Factory();

  @Override
  public AppPreferenceAccessorImpl get() {
    return new AppPreferenceAccessorImpl();
  }

  public static Factory<AppPreferenceAccessorImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link AppPreferenceAccessorImpl#AppPreferenceAccessorImpl()}. */
  public static AppPreferenceAccessorImpl newAppPreferenceAccessorImpl() {
    return new AppPreferenceAccessorImpl();
  }
}
