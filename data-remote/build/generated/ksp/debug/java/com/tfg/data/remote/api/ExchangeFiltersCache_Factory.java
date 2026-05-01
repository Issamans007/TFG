package com.tfg.data.remote.api;

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
public final class ExchangeFiltersCache_Factory implements Factory<ExchangeFiltersCache> {
  private final Provider<BinanceApi> spotApiProvider;

  private final Provider<BinanceFuturesApi> futuresApiProvider;

  public ExchangeFiltersCache_Factory(Provider<BinanceApi> spotApiProvider,
      Provider<BinanceFuturesApi> futuresApiProvider) {
    this.spotApiProvider = spotApiProvider;
    this.futuresApiProvider = futuresApiProvider;
  }

  @Override
  public ExchangeFiltersCache get() {
    return newInstance(spotApiProvider.get(), futuresApiProvider.get());
  }

  public static ExchangeFiltersCache_Factory create(Provider<BinanceApi> spotApiProvider,
      Provider<BinanceFuturesApi> futuresApiProvider) {
    return new ExchangeFiltersCache_Factory(spotApiProvider, futuresApiProvider);
  }

  public static ExchangeFiltersCache newInstance(BinanceApi spotApi, BinanceFuturesApi futuresApi) {
    return new ExchangeFiltersCache(spotApi, futuresApi);
  }
}
