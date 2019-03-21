package com.decawave.argomanager.ui.dialog;

import com.decawave.argomanager.components.NetworkNodeManager;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class RenameNetworkDialogFragment_MembersInjector
    implements MembersInjector<RenameNetworkDialogFragment> {
  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  public RenameNetworkDialogFragment_MembersInjector(
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
  }

  public static MembersInjector<RenameNetworkDialogFragment> create(
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    return new RenameNetworkDialogFragment_MembersInjector(networkNodeManagerProvider);
  }

  @Override
  public void injectMembers(RenameNetworkDialogFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectNetworkNodeManager(
      RenameNetworkDialogFragment instance,
      Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }
}
