package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class ApPreviewFragment_MembersInjector implements MembersInjector<ApPreviewFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<AutoPositioningManager> autoPositioningManagerProvider;

  public ApPreviewFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert autoPositioningManagerProvider != null;
    this.autoPositioningManagerProvider = autoPositioningManagerProvider;
  }

  public static MembersInjector<ApPreviewFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<AutoPositioningManager> autoPositioningManagerProvider) {
    return new ApPreviewFragment_MembersInjector(
        appPreferenceAccessorProvider, networkNodeManagerProvider, autoPositioningManagerProvider);
  }

  @Override
  public void injectMembers(ApPreviewFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      ApPreviewFragment instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectAppPreferenceAccessor(
      ApPreviewFragment instance, Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    instance.appPreferenceAccessor = appPreferenceAccessorProvider.get();
  }

  public static void injectAutoPositioningManager(
      ApPreviewFragment instance, Provider<AutoPositioningManager> autoPositioningManagerProvider) {
    instance.autoPositioningManager = autoPositioningManagerProvider.get();
  }
}
