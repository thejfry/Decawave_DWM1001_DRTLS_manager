package com.decawave.argomanager.components.impl;

import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class UniqueReorderingStack_Factory<T> implements Factory<UniqueReorderingStack<T>> {
  @SuppressWarnings("rawtypes")
  private static final UniqueReorderingStack_Factory INSTANCE = new UniqueReorderingStack_Factory();

  @Override
  public UniqueReorderingStack<T> get() {
    return new UniqueReorderingStack<T>();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <T> Factory<UniqueReorderingStack<T>> create() {
    return (Factory) INSTANCE;
  }
}
