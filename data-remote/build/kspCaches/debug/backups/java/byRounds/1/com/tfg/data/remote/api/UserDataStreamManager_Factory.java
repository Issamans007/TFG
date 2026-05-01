package com.tfg.data.remote.api;

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
public final class UserDataStreamManager_Factory implements Factory<UserDataStreamManager> {
  private final Provider<BinanceApi> spotApiProvider;

  private final Provider<BinanceFuturesApi> futuresApiProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  public UserDataStreamManager_Factory(Provider<BinanceApi> spotApiProvider,
      Provider<BinanceFuturesApi> futuresApiProvider,
      Provider<WebSocketManager> webSocketManagerProvider) {
    this.spotApiProvider = spotApiProvider;
    this.futuresApiProvider = futuresApiProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
  }

  @Override
  public UserDataStreamManager get() {
    return newInstance(spotApiProvider.get(), futuresApiProvider.get(), webSocketManagerProvider.get());
  }

  public static UserDataStreamManager_Factory create(Provider<BinanceApi> spotApiProvider,
      Provider<BinanceFuturesApi> futuresApiProvider,
      Provider<WebSocketManager> webSocketManagerProvider) {
    return new UserDataStreamManager_Factory(spotApiProvider, futuresApiProvider, webSocketManagerProvider);
  }

  public static UserDataStreamManager newInstance(BinanceApi spotApi, BinanceFuturesApi futuresApi,
      WebSocketManager webSocketManager) {
    return new UserDataStreamManager(spotApi, futuresApi, webSocketManager);
  }
}
