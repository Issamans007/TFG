package com.tfg.feature.script;

import com.tfg.domain.repository.MarketRepository;
import com.tfg.domain.repository.ScriptRepository;
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

  public ScriptViewModel_Factory(Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    this.scriptRepositoryProvider = scriptRepositoryProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
  }

  @Override
  public ScriptViewModel get() {
    return newInstance(scriptRepositoryProvider.get(), marketRepositoryProvider.get());
  }

  public static ScriptViewModel_Factory create(Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    return new ScriptViewModel_Factory(scriptRepositoryProvider, marketRepositoryProvider);
  }

  public static ScriptViewModel newInstance(ScriptRepository scriptRepository,
      MarketRepository marketRepository) {
    return new ScriptViewModel(scriptRepository, marketRepository);
  }
}
