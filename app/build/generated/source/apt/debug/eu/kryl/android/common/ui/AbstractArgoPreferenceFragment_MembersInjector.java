package eu.kryl.android.common.ui;

import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class AbstractArgoPreferenceFragment_MembersInjector
    implements MembersInjector<AbstractArgoPreferenceFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  public AbstractArgoPreferenceFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
  }

  public static MembersInjector<AbstractArgoPreferenceFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider) {
    return new AbstractArgoPreferenceFragment_MembersInjector(appPreferenceAccessorProvider);
  }

  @Override
  public void injectMembers(AbstractArgoPreferenceFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    com.decawave.argomanager.ui.fragment.AbstractArgoFragment_MembersInjector
        .injectAppPreferenceAccessor(instance, appPreferenceAccessorProvider);
  }
}
