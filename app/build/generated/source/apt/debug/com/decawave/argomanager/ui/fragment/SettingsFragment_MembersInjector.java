package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class SettingsFragment_MembersInjector implements MembersInjector<SettingsFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public SettingsFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  public static MembersInjector<SettingsFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new SettingsFragment_MembersInjector(appPreferenceAccessorProvider);
  }

  @Override
  public void injectMembers(SettingsFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      SettingsFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }
}
