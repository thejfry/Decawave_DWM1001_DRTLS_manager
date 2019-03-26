package com.decawave.argomanager.ui.dialog;

import com.decawave.argomanager.components.NetworkNodeManager;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class NetworkPickerDialogFragment_MembersInjector
    implements MembersInjector<NetworkPickerDialogFragment> {
  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  public NetworkPickerDialogFragment_MembersInjector(
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
  }

  public static MembersInjector<NetworkPickerDialogFragment> create(
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    return new NetworkPickerDialogFragment_MembersInjector(networkNodeManagerProvider);
  }

  @Override
  public void injectMembers(NetworkPickerDialogFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      NetworkPickerDialogFragment instance,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }
}
