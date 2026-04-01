package com.tfg.feature.chart;

import androidx.lifecycle.SavedStateHandle;
import com.tfg.domain.model.IndicatorExecutor;
import com.tfg.domain.repository.IndicatorRepository;
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
public final class CoinDetailViewModel_Factory implements Factory<CoinDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<MarketRepository> marketRepositoryProvider;

  private final Provider<ScriptRepository> scriptRepositoryProvider;

  private final Provider<IndicatorRepository> indicatorRepositoryProvider;

  private final Provider<IndicatorExecutor> indicatorExecutorProvider;

  public CoinDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<IndicatorRepository> indicatorRepositoryProvider,
      Provider<IndicatorExecutor> indicatorExecutorProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
    this.scriptRepositoryProvider = scriptRepositoryProvider;
    this.indicatorRepositoryProvider = indicatorRepositoryProvider;
    this.indicatorExecutorProvider = indicatorExecutorProvider;
  }

  @Override
  public CoinDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), marketRepositoryProvider.get(), scriptRepositoryProvider.get(), indicatorRepositoryProvider.get(), indicatorExecutorProvider.get());
  }

  public static CoinDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<IndicatorRepository> indicatorRepositoryProvider,
      Provider<IndicatorExecutor> indicatorExecutorProvider) {
    return new CoinDetailViewModel_Factory(savedStateHandleProvider, marketRepositoryProvider, scriptRepositoryProvider, indicatorRepositoryProvider, indicatorExecutorProvider);
  }

  public static CoinDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      MarketRepository marketRepository, ScriptRepository scriptRepository,
      IndicatorRepository indicatorRepository, IndicatorExecutor indicatorExecutor) {
    return new CoinDetailViewModel(savedStateHandle, marketRepository, scriptRepository, indicatorRepository, indicatorExecutor);
  }
}
