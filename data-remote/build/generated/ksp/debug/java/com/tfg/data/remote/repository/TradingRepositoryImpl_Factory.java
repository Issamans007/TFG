package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.OrderDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.data.remote.api.BinanceFuturesApi;
import com.tfg.data.remote.api.BinanceTimeSync;
import com.tfg.data.remote.api.ExchangeFiltersCache;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlin.jvm.functions.Function0;

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
public final class TradingRepositoryImpl_Factory implements Factory<TradingRepositoryImpl> {
  private final Provider<BinanceApi> binanceApiProvider;

  private final Provider<BinanceFuturesApi> binanceFuturesApiProvider;

  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<BinanceTimeSync> timeSyncProvider;

  private final Provider<ExchangeFiltersCache> filtersProvider;

  private final Provider<Function0<String>> secretProvider;

  public TradingRepositoryImpl_Factory(Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider, Provider<OrderDao> orderDaoProvider,
      Provider<BinanceTimeSync> timeSyncProvider, Provider<ExchangeFiltersCache> filtersProvider,
      Provider<Function0<String>> secretProvider) {
    this.binanceApiProvider = binanceApiProvider;
    this.binanceFuturesApiProvider = binanceFuturesApiProvider;
    this.orderDaoProvider = orderDaoProvider;
    this.timeSyncProvider = timeSyncProvider;
    this.filtersProvider = filtersProvider;
    this.secretProvider = secretProvider;
  }

  @Override
  public TradingRepositoryImpl get() {
    return newInstance(binanceApiProvider.get(), binanceFuturesApiProvider.get(), orderDaoProvider.get(), timeSyncProvider.get(), filtersProvider.get(), secretProvider.get());
  }

  public static TradingRepositoryImpl_Factory create(Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider, Provider<OrderDao> orderDaoProvider,
      Provider<BinanceTimeSync> timeSyncProvider, Provider<ExchangeFiltersCache> filtersProvider,
      Provider<Function0<String>> secretProvider) {
    return new TradingRepositoryImpl_Factory(binanceApiProvider, binanceFuturesApiProvider, orderDaoProvider, timeSyncProvider, filtersProvider, secretProvider);
  }

  public static TradingRepositoryImpl newInstance(BinanceApi binanceApi,
      BinanceFuturesApi binanceFuturesApi, OrderDao orderDao, BinanceTimeSync timeSync,
      ExchangeFiltersCache filters, Function0<String> secretProvider) {
    return new TradingRepositoryImpl(binanceApi, binanceFuturesApi, orderDao, timeSync, filters, secretProvider);
  }
}
