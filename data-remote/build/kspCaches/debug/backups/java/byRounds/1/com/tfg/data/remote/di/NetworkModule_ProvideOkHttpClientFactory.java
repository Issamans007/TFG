package com.tfg.data.remote.di;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<OkHttpClient> webSocketClientProvider;

  public NetworkModule_ProvideOkHttpClientFactory(Provider<OkHttpClient> webSocketClientProvider) {
    this.webSocketClientProvider = webSocketClientProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(webSocketClientProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<OkHttpClient> webSocketClientProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(webSocketClientProvider);
  }

  public static OkHttpClient provideOkHttpClient(OkHttpClient webSocketClient) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(webSocketClient));
  }
}
