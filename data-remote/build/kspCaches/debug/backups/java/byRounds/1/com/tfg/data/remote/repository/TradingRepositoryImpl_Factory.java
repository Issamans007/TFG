package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.OrderDao;
import com.tfg.data.remote.api.BinanceApi;
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

  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<Function0<String>> secretProvider;

  public TradingRepositoryImpl_Factory(Provider<BinanceApi> binanceApiProvider,
      Provider<OrderDao> orderDaoProvider, Provider<Function0<String>> secretProvider) {
    this.binanceApiProvider = binanceApiProvider;
    this.orderDaoProvider = orderDaoProvider;
    this.secretProvider = secretProvider;
  }

  @Override
  public TradingRepositoryImpl get() {
    return newInstance(binanceApiProvider.get(), orderDaoProvider.get(), secretProvider.get());
  }

  public static TradingRepositoryImpl_Factory create(Provider<BinanceApi> binanceApiProvider,
      Provider<OrderDao> orderDaoProvider, Provider<Function0<String>> secretProvider) {
    return new TradingRepositoryImpl_Factory(binanceApiProvider, orderDaoProvider, secretProvider);
  }

  public static TradingRepositoryImpl newInstance(BinanceApi binanceApi, OrderDao orderDao,
      Function0<String> secretProvider) {
    return new TradingRepositoryImpl(binanceApi, orderDao, secretProvider);
  }
}
