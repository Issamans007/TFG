package com.tfg.engine;

import com.tfg.data.remote.websocket.WebSocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class ConnectionStateMachine_Factory implements Factory<ConnectionStateMachine> {
  private final Provider<WebSocketManager> webSocketManagerProvider;

  public ConnectionStateMachine_Factory(Provider<WebSocketManager> webSocketManagerProvider) {
    this.webSocketManagerProvider = webSocketManagerProvider;
  }

  @Override
  public ConnectionStateMachine get() {
    return newInstance(webSocketManagerProvider.get());
  }

  public static ConnectionStateMachine_Factory create(
      Provider<WebSocketManager> webSocketManagerProvider) {
    return new ConnectionStateMachine_Factory(webSocketManagerProvider);
  }

  public static ConnectionStateMachine newInstance(WebSocketManager webSocketManager) {
    return new ConnectionStateMachine(webSocketManager);
  }
}
