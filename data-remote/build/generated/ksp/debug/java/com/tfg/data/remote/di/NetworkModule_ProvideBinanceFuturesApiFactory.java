package com.tfg.data.remote.di;

import com.google.gson.Gson;
import com.tfg.data.remote.api.BinanceFuturesApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideBinanceFuturesApiFactory implements Factory<BinanceFuturesApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  public NetworkModule_ProvideBinanceFuturesApiFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public BinanceFuturesApi get() {
    return provideBinanceFuturesApi(okHttpClientProvider.get(), gsonProvider.get());
  }

  public static NetworkModule_ProvideBinanceFuturesApiFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<Gson> gsonProvider) {
    return new NetworkModule_ProvideBinanceFuturesApiFactory(okHttpClientProvider, gsonProvider);
  }

  public static BinanceFuturesApi provideBinanceFuturesApi(OkHttpClient okHttpClient, Gson gson) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBinanceFuturesApi(okHttpClient, gson));
  }
}
