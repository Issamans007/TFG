package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.CandleDao;
import com.tfg.data.local.dao.TradingPairDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.data.remote.api.TfgServerApi;
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
public final class MarketRepositoryImpl_Factory implements Factory<MarketRepositoryImpl> {
  private final Provider<BinanceApi> binanceApiProvider;

  private final Provider<TfgServerApi> tfgServerApiProvider;

  private final Provider<TradingPairDao> tradingPairDaoProvider;

  private final Provider<CandleDao> candleDaoProvider;

  public MarketRepositoryImpl_Factory(Provider<BinanceApi> binanceApiProvider,
      Provider<TfgServerApi> tfgServerApiProvider, Provider<TradingPairDao> tradingPairDaoProvider,
      Provider<CandleDao> candleDaoProvider) {
    this.binanceApiProvider = binanceApiProvider;
    this.tfgServerApiProvider = tfgServerApiProvider;
    this.tradingPairDaoProvider = tradingPairDaoProvider;
    this.candleDaoProvider = candleDaoProvider;
  }

  @Override
  public MarketRepositoryImpl get() {
    return newInstance(binanceApiProvider.get(), tfgServerApiProvider.get(), tradingPairDaoProvider.get(), candleDaoProvider.get());
  }

  public static MarketRepositoryImpl_Factory create(Provider<BinanceApi> binanceApiProvider,
      Provider<TfgServerApi> tfgServerApiProvider, Provider<TradingPairDao> tradingPairDaoProvider,
      Provider<CandleDao> candleDaoProvider) {
    return new MarketRepositoryImpl_Factory(binanceApiProvider, tfgServerApiProvider, tradingPairDaoProvider, candleDaoProvider);
  }

  public static MarketRepositoryImpl newInstance(BinanceApi binanceApi, TfgServerApi tfgServerApi,
      TradingPairDao tradingPairDao, CandleDao candleDao) {
    return new MarketRepositoryImpl(binanceApi, tfgServerApi, tradingPairDao, candleDao);
  }
}
