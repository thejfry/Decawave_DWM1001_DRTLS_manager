package com.decawave.argomanager.debuglog;

import com.decawave.argomanager.components.ErrorManager;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class LogEntryCollectorImpl_Factory implements Factory<LogEntryCollectorImpl> {
  private final Provider<ErrorManager> errorManagerProvider;

  private final Provider<LogBlockStatus> blockStatusProvider;

  public LogEntryCollectorImpl_Factory(
      Provider<ErrorManager> errorManagerProvider, Provider<LogBlockStatus> blockStatusProvider) {
    assert errorManagerProvider != null;
    this.errorManagerProvider = errorManagerProvider;
    assert blockStatusProvider != null;
    this.blockStatusProvider = blockStatusProvider;
  }

  @Override
  public LogEntryCollectorImpl get() {
    return new LogEntryCollectorImpl(errorManagerProvider.get(), blockStatusProvider.get());
  }

  public static Factory<LogEntryCollectorImpl> create(
      Provider<ErrorManager> errorManagerProvider, Provider<LogBlockStatus> blockStatusProvider) {
    return new LogEntryCollectorImpl_Factory(errorManagerProvider, blockStatusProvider);
  }

  /** Proxies {@link LogEntryCollectorImpl#LogEntryCollectorImpl(ErrorManager, LogBlockStatus)}. */
  public static LogEntryCollectorImpl newLogEntryCollectorImpl(
      ErrorManager errorManager, LogBlockStatus blockStatus) {
    return new LogEntryCollectorImpl(errorManager, blockStatus);
  }
}
