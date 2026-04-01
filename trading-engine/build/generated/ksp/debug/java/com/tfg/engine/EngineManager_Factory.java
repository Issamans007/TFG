package com.tfg.engine;

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
public final class EngineManager_Factory implements Factory<EngineManager> {
  private final Provider<StrategyRunner> strategyRunnerProvider;

  private final Provider<AlertMonitor> alertMonitorProvider;

  public EngineManager_Factory(Provider<StrategyRunner> strategyRunnerProvider,
      Provider<AlertMonitor> alertMonitorProvider) {
    this.strategyRunnerProvider = strategyRunnerProvider;
    this.alertMonitorProvider = alertMonitorProvider;
  }

  @Override
  public EngineManager get() {
    return newInstance(strategyRunnerProvider.get(), alertMonitorProvider.get());
  }

  public static EngineManager_Factory create(Provider<StrategyRunner> strategyRunnerProvider,
      Provider<AlertMonitor> alertMonitorProvider) {
    return new EngineManager_Factory(strategyRunnerProvider, alertMonitorProvider);
  }

  public static EngineManager newInstance(StrategyRunner strategyRunner,
      AlertMonitor alertMonitor) {
    return new EngineManager(strategyRunner, alertMonitor);
  }
}
