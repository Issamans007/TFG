package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.CandleDao;
import com.tfg.data.local.dao.CustomTemplateDao;
import com.tfg.data.local.dao.ScriptDao;
import com.tfg.data.local.dao.SignalMarkerDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.data.remote.api.BinanceFuturesApi;
import com.tfg.domain.model.ScriptExecutor;
import com.tfg.domain.repository.FeeRepository;
import com.tfg.domain.service.ConsoleBus;
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
public final class ScriptRepositoryImpl_Factory implements Factory<ScriptRepositoryImpl> {
  private final Provider<ScriptDao> scriptDaoProvider;

  private final Provider<SignalMarkerDao> signalMarkerDaoProvider;

  private final Provider<CustomTemplateDao> customTemplateDaoProvider;

  private final Provider<CandleDao> candleDaoProvider;

  private final Provider<BinanceApi> binanceApiProvider;

  private final Provider<BinanceFuturesApi> binanceFuturesApiProvider;

  private final Provider<ScriptExecutor> scriptExecutorProvider;

  private final Provider<FeeRepository> feeRepositoryProvider;

  private final Provider<ConsoleBus> consoleBusProvider;

  public ScriptRepositoryImpl_Factory(Provider<ScriptDao> scriptDaoProvider,
      Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<CustomTemplateDao> customTemplateDaoProvider, Provider<CandleDao> candleDaoProvider,
      Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider,
      Provider<ScriptExecutor> scriptExecutorProvider,
      Provider<FeeRepository> feeRepositoryProvider, Provider<ConsoleBus> consoleBusProvider) {
    this.scriptDaoProvider = scriptDaoProvider;
    this.signalMarkerDaoProvider = signalMarkerDaoProvider;
    this.customTemplateDaoProvider = customTemplateDaoProvider;
    this.candleDaoProvider = candleDaoProvider;
    this.binanceApiProvider = binanceApiProvider;
    this.binanceFuturesApiProvider = binanceFuturesApiProvider;
    this.scriptExecutorProvider = scriptExecutorProvider;
    this.feeRepositoryProvider = feeRepositoryProvider;
    this.consoleBusProvider = consoleBusProvider;
  }

  @Override
  public ScriptRepositoryImpl get() {
    return newInstance(scriptDaoProvider.get(), signalMarkerDaoProvider.get(), customTemplateDaoProvider.get(), candleDaoProvider.get(), binanceApiProvider.get(), binanceFuturesApiProvider.get(), scriptExecutorProvider.get(), feeRepositoryProvider.get(), consoleBusProvider.get());
  }

  public static ScriptRepositoryImpl_Factory create(Provider<ScriptDao> scriptDaoProvider,
      Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<CustomTemplateDao> customTemplateDaoProvider, Provider<CandleDao> candleDaoProvider,
      Provider<BinanceApi> binanceApiProvider,
      Provider<BinanceFuturesApi> binanceFuturesApiProvider,
      Provider<ScriptExecutor> scriptExecutorProvider,
      Provider<FeeRepository> feeRepositoryProvider, Provider<ConsoleBus> consoleBusProvider) {
    return new ScriptRepositoryImpl_Factory(scriptDaoProvider, signalMarkerDaoProvider, customTemplateDaoProvider, candleDaoProvider, binanceApiProvider, binanceFuturesApiProvider, scriptExecutorProvider, feeRepositoryProvider, consoleBusProvider);
  }

  public static ScriptRepositoryImpl newInstance(ScriptDao scriptDao,
      SignalMarkerDao signalMarkerDao, CustomTemplateDao customTemplateDao, CandleDao candleDao,
      BinanceApi binanceApi, BinanceFuturesApi binanceFuturesApi, ScriptExecutor scriptExecutor,
      FeeRepository feeRepository, ConsoleBus consoleBus) {
    return new ScriptRepositoryImpl(scriptDao, signalMarkerDao, customTemplateDao, candleDao, binanceApi, binanceFuturesApi, scriptExecutor, feeRepository, consoleBus);
  }
}
