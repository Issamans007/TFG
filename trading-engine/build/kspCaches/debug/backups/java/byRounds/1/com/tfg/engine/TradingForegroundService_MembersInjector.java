package com.tfg.engine;

import com.tfg.data.local.dao.AlertDao;
import com.tfg.data.local.dao.OfflineQueueDao;
import com.tfg.data.local.dao.OrderDao;
import com.tfg.data.local.dao.TradingPairDao;
import com.tfg.data.remote.api.UserDataStreamManager;
import com.tfg.data.remote.websocket.WebSocketManager;
import com.tfg.domain.repository.AuditRepository;
import com.tfg.domain.repository.PortfolioRepository;
import com.tfg.domain.repository.TradingRepository;
import com.tfg.domain.service.ConsoleBus;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TradingForegroundService_MembersInjector implements MembersInjector<TradingForegroundService> {
  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<RiskEngine> riskEngineProvider;

  private final Provider<TradeExecutor> tradeExecutorProvider;

  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<OfflineQueueDao> offlineQueueDaoProvider;

  private final Provider<StrategyRunner> strategyRunnerProvider;

  private final Provider<EngineManager> engineManagerProvider;

  private final Provider<AuditRepository> auditRepositoryProvider;

  private final Provider<TradingRepository> tradingRepositoryProvider;

  private final Provider<PortfolioRepository> portfolioRepositoryProvider;

  private final Provider<ConsoleBus> consoleBusProvider;

  private final Provider<UserDataStreamManager> userDataStreamManagerProvider;

  private final Provider<AlertDao> alertDaoProvider;

  private final Provider<TradingPairDao> tradingPairDaoProvider;

  public TradingForegroundService_MembersInjector(
      Provider<WebSocketManager> webSocketManagerProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<TradeExecutor> tradeExecutorProvider, Provider<OrderDao> orderDaoProvider,
      Provider<OfflineQueueDao> offlineQueueDaoProvider,
      Provider<StrategyRunner> strategyRunnerProvider,
      Provider<EngineManager> engineManagerProvider,
      Provider<AuditRepository> auditRepositoryProvider,
      Provider<TradingRepository> tradingRepositoryProvider,
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<ConsoleBus> consoleBusProvider,
      Provider<UserDataStreamManager> userDataStreamManagerProvider,
      Provider<AlertDao> alertDaoProvider, Provider<TradingPairDao> tradingPairDaoProvider) {
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.riskEngineProvider = riskEngineProvider;
    this.tradeExecutorProvider = tradeExecutorProvider;
    this.orderDaoProvider = orderDaoProvider;
    this.offlineQueueDaoProvider = offlineQueueDaoProvider;
    this.strategyRunnerProvider = strategyRunnerProvider;
    this.engineManagerProvider = engineManagerProvider;
    this.auditRepositoryProvider = auditRepositoryProvider;
    this.tradingRepositoryProvider = tradingRepositoryProvider;
    this.portfolioRepositoryProvider = portfolioRepositoryProvider;
    this.consoleBusProvider = consoleBusProvider;
    this.userDataStreamManagerProvider = userDataStreamManagerProvider;
    this.alertDaoProvider = alertDaoProvider;
    this.tradingPairDaoProvider = tradingPairDaoProvider;
  }

  public static MembersInjector<TradingForegroundService> create(
      Provider<WebSocketManager> webSocketManagerProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<TradeExecutor> tradeExecutorProvider, Provider<OrderDao> orderDaoProvider,
      Provider<OfflineQueueDao> offlineQueueDaoProvider,
      Provider<StrategyRunner> strategyRunnerProvider,
      Provider<EngineManager> engineManagerProvider,
      Provider<AuditRepository> auditRepositoryProvider,
      Provider<TradingRepository> tradingRepositoryProvider,
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<ConsoleBus> consoleBusProvider,
      Provider<UserDataStreamManager> userDataStreamManagerProvider,
      Provider<AlertDao> alertDaoProvider, Provider<TradingPairDao> tradingPairDaoProvider) {
    return new TradingForegroundService_MembersInjector(webSocketManagerProvider, riskEngineProvider, tradeExecutorProvider, orderDaoProvider, offlineQueueDaoProvider, strategyRunnerProvider, engineManagerProvider, auditRepositoryProvider, tradingRepositoryProvider, portfolioRepositoryProvider, consoleBusProvider, userDataStreamManagerProvider, alertDaoProvider, tradingPairDaoProvider);
  }

  @Override
  public void injectMembers(TradingForegroundService instance) {
    injectWebSocketManager(instance, webSocketManagerProvider.get());
    injectRiskEngine(instance, riskEngineProvider.get());
    injectTradeExecutor(instance, tradeExecutorProvider.get());
    injectOrderDao(instance, orderDaoProvider.get());
    injectOfflineQueueDao(instance, offlineQueueDaoProvider.get());
    injectStrategyRunner(instance, strategyRunnerProvider.get());
    injectEngineManager(instance, engineManagerProvider.get());
    injectAuditRepository(instance, auditRepositoryProvider.get());
    injectTradingRepository(instance, tradingRepositoryProvider.get());
    injectPortfolioRepository(instance, portfolioRepositoryProvider.get());
    injectConsoleBus(instance, consoleBusProvider.get());
    injectUserDataStreamManager(instance, userDataStreamManagerProvider.get());
    injectAlertDao(instance, alertDaoProvider.get());
    injectTradingPairDao(instance, tradingPairDaoProvider.get());
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.webSocketManager")
  public static void injectWebSocketManager(TradingForegroundService instance,
      WebSocketManager webSocketManager) {
    instance.webSocketManager = webSocketManager;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.riskEngine")
  public static void injectRiskEngine(TradingForegroundService instance, RiskEngine riskEngine) {
    instance.riskEngine = riskEngine;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.tradeExecutor")
  public static void injectTradeExecutor(TradingForegroundService instance,
      TradeExecutor tradeExecutor) {
    instance.tradeExecutor = tradeExecutor;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.orderDao")
  public static void injectOrderDao(TradingForegroundService instance, OrderDao orderDao) {
    instance.orderDao = orderDao;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.offlineQueueDao")
  public static void injectOfflineQueueDao(TradingForegroundService instance,
      OfflineQueueDao offlineQueueDao) {
    instance.offlineQueueDao = offlineQueueDao;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.strategyRunner")
  public static void injectStrategyRunner(TradingForegroundService instance,
      StrategyRunner strategyRunner) {
    instance.strategyRunner = strategyRunner;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.engineManager")
  public static void injectEngineManager(TradingForegroundService instance,
      EngineManager engineManager) {
    instance.engineManager = engineManager;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.auditRepository")
  public static void injectAuditRepository(TradingForegroundService instance,
      AuditRepository auditRepository) {
    instance.auditRepository = auditRepository;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.tradingRepository")
  public static void injectTradingRepository(TradingForegroundService instance,
      TradingRepository tradingRepository) {
    instance.tradingRepository = tradingRepository;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.portfolioRepository")
  public static void injectPortfolioRepository(TradingForegroundService instance,
      PortfolioRepository portfolioRepository) {
    instance.portfolioRepository = portfolioRepository;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.consoleBus")
  public static void injectConsoleBus(TradingForegroundService instance, ConsoleBus consoleBus) {
    instance.consoleBus = consoleBus;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.userDataStreamManager")
  public static void injectUserDataStreamManager(TradingForegroundService instance,
      UserDataStreamManager userDataStreamManager) {
    instance.userDataStreamManager = userDataStreamManager;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.alertDao")
  public static void injectAlertDao(TradingForegroundService instance, AlertDao alertDao) {
    instance.alertDao = alertDao;
  }

  @InjectedFieldSignature("com.tfg.engine.TradingForegroundService.tradingPairDao")
  public static void injectTradingPairDao(TradingForegroundService instance,
      TradingPairDao tradingPairDao) {
    instance.tradingPairDao = tradingPairDao;
  }
}
