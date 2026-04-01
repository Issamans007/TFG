package com.tfg.feature.markets;

import com.tfg.domain.repository.MarketRepository;
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
public final class MarketsViewModel_Factory implements Factory<MarketsViewModel> {
  private final Provider<MarketRepository> marketRepositoryProvider;

  public MarketsViewModel_Factory(Provider<MarketRepository> marketRepositoryProvider) {
    this.marketRepositoryProvider = marketRepositoryProvider;
  }

  @Override
  public MarketsViewModel get() {
    return newInstance(marketRepositoryProvider.get());
  }

  public static MarketsViewModel_Factory create(
      Provider<MarketRepository> marketRepositoryProvider) {
    return new MarketsViewModel_Factory(marketRepositoryProvider);
  }

  public static MarketsViewModel newInstance(MarketRepository marketRepository) {
    return new MarketsViewModel(marketRepository);
  }
}
