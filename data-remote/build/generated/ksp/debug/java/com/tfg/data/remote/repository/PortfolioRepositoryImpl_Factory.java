package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.AssetBalanceDao;
import com.tfg.data.local.dao.AuditLogDao;
import com.tfg.data.local.dao.OrderDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.data.remote.api.BinanceFuturesApi;
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
public final class PortfolioRepositoryImpl_Factory implements Factory<PortfolioRepositoryImpl> {
  private final Provider<BinanceApi> binanceApiProvider;

  private final Provider<BinanceFuturesApi> binanceFuturesApiProvider;

  private final Provider<AssetBalanceDao> assetBalanceDaoProvider;

  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<AuditLogDao> auditLogDaoProvider;

  private final Provider<Function0<String>> secretProvider;

  public PortfolioRepositoryImpl_Factory(Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider,
      Provider<AssetBalanceDao> assetBalanceDaoProvider, Provider<OrderDao> orderDaoProvider,
      Provider<AuditLogDao> auditLogDaoProvider, Provider<Function0<String>> secretProvider) {
    this.binanceApiProvider = binanceApiProvider;
    this.binanceFuturesApiProvider = binanceFuturesApiProvider;
    this.assetBalanceDaoProvider = assetBalanceDaoProvider;
    this.orderDaoProvider = orderDaoProvider;
    this.auditLogDaoProvider = auditLogDaoProvider;
    this.secretProvider = secretProvider;
  }

  @Override
  public PortfolioRepositoryImpl get() {
    return newInstance(binanceApiProvider.get(), binanceFuturesApiProvider.get(), assetBalanceDaoProvider.get(), orderDaoProvider.get(), auditLogDaoProvider.get(), secretProvider.get());
  }

  public static PortfolioRepositoryImpl_Factory create(Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider,
      Provider<AssetBalanceDao> assetBalanceDaoProvider, Provider<OrderDao> orderDaoProvider,
      Provider<AuditLogDao> auditLogDaoProvider, Provider<Function0<String>> secretProvider) {
    return new PortfolioRepositoryImpl_Factory(binanceApiProvider, binanceFuturesApiProvider, assetBalanceDaoProvider, orderDaoProvider, auditLogDaoProvider, secretProvider);
  }

  public static PortfolioRepositoryImpl newInstance(BinanceApi binanceApi,
      BinanceFuturesApi binanceFuturesApi, AssetBalanceDao assetBalanceDao, OrderDao orderDao,
      AuditLogDao auditLogDao, Function0<String> secretProvider) {
    return new PortfolioRepositoryImpl(binanceApi, binanceFuturesApi, assetBalanceDao, orderDao, auditLogDao, secretProvider);
  }
}
