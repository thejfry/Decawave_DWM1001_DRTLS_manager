package com.decawave.argomanager.debuglog;

import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class ApplicationComponentLog_MembersInjector
    implements MembersInjector<ApplicationComponentLog> {
  private final Provider<LogEntryCollector> logCollectorProvider;

  public ApplicationComponentLog_MembersInjector(Provider<LogEntryCollector> logCollectorProvider) {
    assert logCollectorProvider != null;
    this.logCollectorProvider = logCollectorProvider;
  }

  public static MembersInjector<ApplicationComponentLog> create(
      Provider<LogEntryCollector> logCollectorProvider) {
    return new ApplicationComponentLog_MembersInjector(logCollectorProvider);
  }

  @Override
  public void injectMembers(ApplicationComponentLog instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.logCollector = logCollectorProvider.get();
  }

  public static void injectLogCollector(
      ApplicationComponentLog instance, Provider<LogEntryCollector> logCollectorProvider) {
    instance.logCollector = logCollectorProvider.get();
  }
}
