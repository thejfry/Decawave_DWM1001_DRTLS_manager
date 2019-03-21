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
public final class LogBufferFragment_MembersInjector implements MembersInjector<LogBufferFragment> {
  private final Provider<AppPreferenceAccessor> appPreferenceAccessorProvider;

  private final Provider<LogEntryCollector> logEntryCollectorProvider;

  public LogBufferFragment_MembersInjector(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider) {
    assert appPreferenceAccessorProvider != null;
    this.appPreferenceAccessorProvider = appPreferenceAccessorProvider;
    assert logEntryCollectorProvider != null;
    this.logEntryCollectorProvider = logEntryCollectorProvider;
  }

  public static MembersInjector<LogBufferFragment> create(
      Provider<AppPreferenceAccessor> appPreferenceAccessorProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider) {
    return new LogBufferFragment_MembersInjector(
        appPreferenceAccessorProvider, logEntryCollectorProvider);
  }

  @Override
  public void injectMembers(LogBufferFragment instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    ((AbstractArgoFragment) instance).appPreferenceAccessor = appPreferenceAccessorProvider.get();
    instance.logEntryCollector = logEntryCollectorProvider.get();
  }

  public static void injectLogEntryCollector(
      LogBufferFragment instance, Provider<LogEntryCollector> logEntryCollectorProvider) {
    instance.logEntryCollector = logEntryCollectorProvider.get();
  }
}
