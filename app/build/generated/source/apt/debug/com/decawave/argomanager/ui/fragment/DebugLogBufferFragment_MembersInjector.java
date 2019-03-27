package com.decawave.argomanager.ui.fragment;

import com.decawave.argomanager.debuglog.LogEntryCollector;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class DebugLogBufferFragment_MembersInjector
    implements MembersInjector<DebugLogBufferFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<LogEntryCollector> logEntryCollectorProvider;

  public DebugLogBufferFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert logEntryCollectorProvider != null;
    this.logEntryCollectorProvider = logEntryCollectorProvider;
  }

  public static MembersInjector<DebugLogBufferFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider) {
    return new DebugLogBufferFragment_MembersInjector(
        appPreferenceAccessorProvider, logEntryCollectorProvider);
  }

  @Override
  public void injectMembers(DebugLogBufferFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    ((LogBufferFragment) instance).logEntryCollector = logEntryCollectorProvider.get();
  }
}
