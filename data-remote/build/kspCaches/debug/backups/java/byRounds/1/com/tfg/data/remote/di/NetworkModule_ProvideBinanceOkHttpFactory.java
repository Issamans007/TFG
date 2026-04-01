package com.tfg.data.remote.di;

import com.tfg.data.remote.interceptor.BinanceAuthInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
public final class NetworkModule_ProvideBinanceOkHttpFactory implements Factory<OkHttpClient> {
  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  private final Provider<BinanceAuthInterceptor> binanceAuthInterceptorProvider;

  public NetworkModule_ProvideBinanceOkHttpFactory(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<BinanceAuthInterceptor> binanceAuthInterceptorProvider) {
    this.loggingInterceptorProvider = loggingInterceptorProvider;
    this.binanceAuthInterceptorProvider = binanceAuthInterceptorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideBinanceOkHttp(loggingInterceptorProvider.get(), binanceAuthInterceptorProvider.get());
  }

  public static NetworkModule_ProvideBinanceOkHttpFactory create(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<BinanceAuthInterceptor> binanceAuthInterceptorProvider) {
    return new NetworkModule_ProvideBinanceOkHttpFactory(loggingInterceptorProvider, binanceAuthInterceptorProvider);
  }

  public static OkHttpClient provideBinanceOkHttp(HttpLoggingInterceptor loggingInterceptor,
      BinanceAuthInterceptor binanceAuthInterceptor) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBinanceOkHttp(loggingInterceptor, binanceAuthInterceptor));
  }
}
