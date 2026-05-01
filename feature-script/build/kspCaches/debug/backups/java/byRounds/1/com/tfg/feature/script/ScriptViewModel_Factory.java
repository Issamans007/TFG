package com.tfg.feature.script;

import com.tfg.domain.model.ScriptExecutor;
import com.tfg.domain.repository.MarketRepository;
import com.tfg.domain.repository.ScriptRepository;
import com.tfg.domain.service.ConsoleBus;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ScriptViewModel_Factory implements Factory<ScriptViewModel> {
  private final Provider<ScriptRepository> scriptRepositoryProvider;

  private final Provider<MarketRepository> marketRepositoryProvider;

  private final Provider<ScriptExecutor> scriptExecutorProvider;

  private final Provider<ConsoleBus> consoleBusProvider;

  public ScriptViewModel_Factory(Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<ScriptExecutor> scriptExecutorProvider, Provider<ConsoleBus> consoleBusProvider) {
    this.scriptRepositoryProvider = scriptRepositoryProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
    this.scriptExecutorProvider = scriptExecutorProvider;
    this.consoleBusProvider = consoleBusProvider;
  }

  @Override
  public ScriptViewModel get() {
    return newInstance(scriptRepositoryProvider.get(), marketRepositoryProvider.get(), scriptExecutorProvider.get(), consoleBusProvider.get());
  }

  public static ScriptViewModel_Factory create(Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<ScriptExecutor> scriptExecutorProvider, Provider<ConsoleBus> consoleBusProvider) {
    return new ScriptViewModel_Factory(scriptRepositoryProvider, marketRepositoryProvider, scriptExecutorProvider, consoleBusProvider);
  }

  public static ScriptViewModel newInstance(ScriptRepository scriptRepository,
      MarketRepository marketRepository, ScriptExecutor scriptExecutor, ConsoleBus consoleBus) {
    return new ScriptViewModel(scriptRepository, marketRepository, scriptExecutor, consoleBus);
  }
}
