package com.tfg.feature.alerts;

import com.tfg.domain.repository.AlertRepository;
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
public final class AlertsViewModel_Factory implements Factory<AlertsViewModel> {
  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<MarketRepository> marketRepositoryProvider;

  public AlertsViewModel_Factory(Provider<AlertRepository> alertRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
  }

  @Override
  public AlertsViewModel get() {
    return newInstance(alertRepositoryProvider.get(), marketRepositoryProvider.get());
  }

  public static AlertsViewModel_Factory create(Provider<AlertRepository> alertRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    return new AlertsViewModel_Factory(alertRepositoryProvider, marketRepositoryProvider);
  }

  public static AlertsViewModel newInstance(AlertRepository alertRepository,
      MarketRepository marketRepository) {
    return new AlertsViewModel(alertRepository, marketRepository);
  }
}
