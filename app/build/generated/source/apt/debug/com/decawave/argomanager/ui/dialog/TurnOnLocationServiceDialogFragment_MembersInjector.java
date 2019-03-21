package com.decawave.argomanager.ui.dialog;

import com.decawave.argomanager.util.AndroidPermissionHelper;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class TurnOnLocationServiceDialogFragment_MembersInjector
    implements MembersInjector<TurnOnLocationServiceDialogFragment> {
  private final Provider<AndroidPermissionHelper> permissionHelperProvider;

  public TurnOnLocationServiceDialogFragment_MembersInjector(
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    assert permissionHelperProvider != null;
    this.permissionHelperProvider = permissionHelperProvider;
  }

  public static MembersInjector<TurnOnLocationServiceDialogFragment> create(
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    return new TurnOnLocationServiceDialogFragment_MembersInjector(permissionHelperProvider);
  }

  @Override
  public void injectMembers(TurnOnLocationServiceDialogFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.permissionHelper = permissionHelperProvider.get();
  }

  public static void injectPermissionHelper(
      TurnOnLocationServiceDialogFragment instance,
      Provider<AndroidPermissionHelper> permissionHelperProvider) {
    instance.permissionHelper = permissionHelperProvider.get();
  }
}
