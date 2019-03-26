package com.decawave.argomanager.ble.signal;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class SignalStrengthInterpreterImpl_Factory
    implements Factory<SignalStrengthInterpreterImpl> {
  private static final SignalStrengthInterpreterImpl_Factory INSTANCE =
      new SignalStrengthInterpreterImpl_Factory();

  @Override
  public SignalStrengthInterpreterImpl get() {
    return new SignalStrengthInterpreterImpl();
  }

  public static Factory<SignalStrengthInterpreterImpl> create() {
    return INSTANCE;
  }

  /** Proxies {@link SignalStrengthInterpreterImpl#SignalStrengthInterpreterImpl()}. */
  public static SignalStrengthInterpreterImpl newSignalStrengthInterpreterImpl() {
    return new SignalStrengthInterpreterImpl();
  }
}
