package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.CandleDao;
import com.tfg.data.local.dao.CustomTemplateDao;
import com.tfg.data.local.dao.ScriptDao;
import com.tfg.data.local.dao.SignalMarkerDao;
import com.tfg.data.remote.api.BinanceApi;
import com.tfg.domain.model.ScriptExecutor;
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

  private final Provider<ScriptExecutor> scriptExecutorProvider;

  public ScriptRepositoryImpl_Factory(Provider<ScriptDao> scriptDaoProvider,
      Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<CustomTemplateDao> customTemplateDaoProvider, Provider<CandleDao> candleDaoProvider,
      Provider<BinanceApi> binanceApiProvider, Provider<ScriptExecutor> scriptExecutorProvider) {
    this.scriptDaoProvider = scriptDaoProvider;
    this.signalMarkerDaoProvider = signalMarkerDaoProvider;
    this.customTemplateDaoProvider = customTemplateDaoProvider;
    this.candleDaoProvider = candleDaoProvider;
    this.binanceApiProvider = binanceApiProvider;
    this.scriptExecutorProvider = scriptExecutorProvider;
  }

  @Override
  public ScriptRepositoryImpl get() {
    return newInstance(scriptDaoProvider.get(), signalMarkerDaoProvider.get(), customTemplateDaoProvider.get(), candleDaoProvider.get(), binanceApiProvider.get(), scriptExecutorProvider.get());
  }

  public static ScriptRepositoryImpl_Factory create(Provider<ScriptDao> scriptDaoProvider,
      Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<CustomTemplateDao> customTemplateDaoProvider, Provider<CandleDao> candleDaoProvider,
      Provider<BinanceApi> binanceApiProvider, Provider<ScriptExecutor> scriptExecutorProvider) {
    return new ScriptRepositoryImpl_Factory(scriptDaoProvider, signalMarkerDaoProvider, customTemplateDaoProvider, candleDaoProvider, binanceApiProvider, scriptExecutorProvider);
  }

  public static ScriptRepositoryImpl newInstance(ScriptDao scriptDao,
      SignalMarkerDao signalMarkerDao, CustomTemplateDao customTemplateDao, CandleDao candleDao,
      BinanceApi binanceApi, ScriptExecutor scriptExecutor) {
    return new ScriptRepositoryImpl(scriptDao, signalMarkerDao, customTemplateDao, candleDao, binanceApi, scriptExecutor);
  }
}
