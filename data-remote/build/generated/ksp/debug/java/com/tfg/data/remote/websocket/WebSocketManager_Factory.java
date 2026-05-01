package com.tfg.data.remote.websocket;

import com.google.gson.Gson;
import com.tfg.data.remote.api.BinanceApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class WebSocketManager_Factory implements Factory<WebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<BinanceApi> binanceApiProvider;

  public WebSocketManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<BinanceApi> binanceApiProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.gsonProvider = gsonProvider;
    this.binanceApiProvider = binanceApiProvider;
  }

  @Override
  public WebSocketManager get() {
    return newInstance(okHttpClientProvider.get(), gsonProvider.get(), binanceApiProvider.get());
  }

  public static WebSocketManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<BinanceApi> binanceApiProvider) {
    return new WebSocketManager_Factory(okHttpClientProvider, gsonProvider, binanceApiProvider);
  }

  public static WebSocketManager newInstance(OkHttpClient okHttpClient, Gson gson,
      BinanceApi binanceApi) {
    return new WebSocketManager(okHttpClient, gson, binanceApi);
  }
}
