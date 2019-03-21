package com.decawave.argomanager.debuglog;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class LogBlockStatusImpl_Factory implements Factory<LogBlockStatusImpl> {
  private static final LogBlockStatusImpl_Factory INSTANCE = new LogBlockStatusImpl_Factory();

  @Override
  public LogBlockStatusImpl get() {
    return new LogBlockStatusImpl();
  }

  public static Factory<LogBlockStatusImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link LogBlockStatusImpl#LogBlockStatusImpl()}. */
  public static LogBlockStatusImpl newLogBlockStatusImpl() {
    return new LogBlockStatusImpl();
  }
}
