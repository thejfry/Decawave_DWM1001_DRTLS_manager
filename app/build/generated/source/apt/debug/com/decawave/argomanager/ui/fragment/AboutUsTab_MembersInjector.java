package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class AboutUsTab_MembersInjector implements MembersInjector<AboutUsTab> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public AboutUsTab_MembersInjector(Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  public static MembersInjector<AboutUsTab> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new AboutUsTab_MembersInjector(appPreferenceAccessorProvider);
  }

  @Override
  public void injectMembers(AboutUsTab instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      AboutUsTab instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
