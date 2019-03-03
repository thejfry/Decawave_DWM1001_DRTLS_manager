package com.decawave.argomanager;

import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.impl.UniqueReorderingStack;
import com.decawave.argomanager.debuglog.LogEntryCollector;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class ArgoApp_MembersInjector implements MembersInjector<ArgoApp> {
  private final Provider<BlePresenceApi> blePresenceApiProvider;

  private final Provider<NetworkNodeManager> networkNodeManagerProvider;

  private final Provider<LogEntryCollector> logEntryCollectorProvider;

  private final Provider<UniqueReorderingStack<Short>> activeNetworkStackProvider;

  public ArgoApp_MembersInjector(
      Provider<BlePresenceApi> blePresenceApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider,
      Provider<UniqueReorderingStack<Short>> activeNetworkStackProvider) {
    assert blePresenceApiProvider != null;
    this.blePresenceApiProvider = blePresenceApiProvider;
    assert networkNodeManagerProvider != null;
    this.networkNodeManagerProvider = networkNodeManagerProvider;
    assert logEntryCollectorProvider != null;
    this.logEntryCollectorProvider = logEntryCollectorProvider;
    assert activeNetworkStackProvider != null;
    this.activeNetworkStackProvider = activeNetworkStackProvider;
  }

  public static MembersInjector<ArgoApp> create(
      Provider<BlePresenceApi> blePresenceApiProvider,
      Provider<NetworkNodeManager> networkNodeManagerProvider,
      Provider<LogEntryCollector> logEntryCollectorProvider,
      Provider<UniqueReorderingStack<Short>> activeNetworkStackProvider) {
    return new ArgoApp_MembersInjector(
        blePresenceApiProvider,
        networkNodeManagerProvider,
        logEntryCollectorProvider,
        activeNetworkStackProvider);
  }

  @Override
  public void injectMembers(ArgoApp instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.blePresenceApi = blePresenceApiProvider.get();
    instance.networkNodeManager = networkNodeManagerProvider.get();
    instance.logEntryCollector = logEntryCollectorProvider.get();
    instance.activeNetworkStack = activeNetworkStackProvider.get();
  }

  public static void injectBlePresenceApi(
      ArgoApp instance, Provider<BlePresenceApi> blePresenceApiProvider) {
    instance.blePresenceApi = blePresenceApiProvider.get();
  }

  public static void injectNetworkNodeManager(
      ArgoApp instance, Provider<NetworkNodeManager> networkNodeManagerProvider) {
    instance.networkNodeManager = networkNodeManagerProvider.get();
  }

  public static void injectLogEntryCollector(
      ArgoApp instance, Provider<LogEntryCollector> logEntryCollectorProvider) {
    instance.logEntryCollector = logEntryCollectorProvider.get();
  }

  public static void injectActiveNetworkStack(
      ArgoApp instance, Provider<UniqueReorderingStack<Short>> activeNetworkStackProvider) {
    instance.activeNetworkStack = activeNetworkStackProvider.get();
  }
}
