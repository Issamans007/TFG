package com.tfg.engine;

import android.content.Context;
import com.tfg.data.local.dao.OrderDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.data.remote.websocket.WebSocketManager;
import com.tfg.domain.repository.TradingRepository;
import com.tfg.domain.service.ConsoleBus;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TradeExecutor_Factory implements Factory<TradeExecutor> {
  private final Provider<TradingRepository> tradingRepositoryProvider;

  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<RiskEngine> riskEngineProvider;

  private final Provider<BinanceApi> binanceApiProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<ConsoleBus> consoleBusProvider;

  private final Provider<Context> contextProvider;

  public TradeExecutor_Factory(Provider<TradingRepository> tradingRepositoryProvider,
      Provider<OrderDao> orderDaoProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<BinanceApi> binanceApiProvider, Provider<WebSocketManager> webSocketManagerProvider,
      Provider<ConsoleBus> consoleBusProvider, Provider<Context> contextProvider) {
    this.tradingRepositoryProvider = tradingRepositoryProvider;
    this.orderDaoProvider = orderDaoProvider;
    this.riskEngineProvider = riskEngineProvider;
    this.binanceApiProvider = binanceApiProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.consoleBusProvider = consoleBusProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public TradeExecutor get() {
    return newInstance(tradingRepositoryProvider.get(), orderDaoProvider.get(), riskEngineProvider.get(), binanceApiProvider.get(), webSocketManagerProvider.get(), consoleBusProvider.get(), contextProvider.get());
  }

  public static TradeExecutor_Factory create(Provider<TradingRepository> tradingRepositoryProvider,
      Provider<OrderDao> orderDaoProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<BinanceApi> binanceApiProvider, Provider<WebSocketManager> webSocketManagerProvider,
      Provider<ConsoleBus> consoleBusProvider, Provider<Context> contextProvider) {
    return new TradeExecutor_Factory(tradingRepositoryProvider, orderDaoProvider, riskEngineProvider, binanceApiProvider, webSocketManagerProvider, consoleBusProvider, contextProvider);
  }

  public static TradeExecutor newInstance(TradingRepository tradingRepository, OrderDao orderDao,
      RiskEngine riskEngine, BinanceApi binanceApi, WebSocketManager webSocketManager,
      ConsoleBus consoleBus, Context context) {
    return new TradeExecutor(tradingRepository, orderDao, riskEngine, binanceApi, webSocketManager, consoleBus, context);
  }
}
