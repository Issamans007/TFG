package com.tfg.feature.dashboard;

import com.tfg.domain.repository.DonationRepository;
import com.tfg.domain.repository.SignalRepository;
import com.tfg.domain.usecase.analytics.GetAnalyticsUseCase;
import com.tfg.domain.usecase.analytics.GetPortfolioUseCase;
import com.tfg.engine.EngineManager;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider;

  private final Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider;

  private final Provider<SignalRepository> signalRepositoryProvider;

  private final Provider<DonationRepository> donationRepositoryProvider;

  private final Provider<EngineManager> engineManagerProvider;

  public DashboardViewModel_Factory(Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider,
      Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider,
      Provider<SignalRepository> signalRepositoryProvider,
      Provider<DonationRepository> donationRepositoryProvider,
      Provider<EngineManager> engineManagerProvider) {
    this.getPortfolioUseCaseProvider = getPortfolioUseCaseProvider;
    this.getAnalyticsUseCaseProvider = getAnalyticsUseCaseProvider;
    this.signalRepositoryProvider = signalRepositoryProvider;
    this.donationRepositoryProvider = donationRepositoryProvider;
    this.engineManagerProvider = engineManagerProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(getPortfolioUseCaseProvider.get(), getAnalyticsUseCaseProvider.get(), signalRepositoryProvider.get(), donationRepositoryProvider.get(), engineManagerProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider,
      Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider,
      Provider<SignalRepository> signalRepositoryProvider,
      Provider<DonationRepository> donationRepositoryProvider,
      Provider<EngineManager> engineManagerProvider) {
    return new DashboardViewModel_Factory(getPortfolioUseCaseProvider, getAnalyticsUseCaseProvider, signalRepositoryProvider, donationRepositoryProvider, engineManagerProvider);
  }

  public static DashboardViewModel newInstance(GetPortfolioUseCase getPortfolioUseCase,
      GetAnalyticsUseCase getAnalyticsUseCase, SignalRepository signalRepository,
      DonationRepository donationRepository, EngineManager engineManager) {
    return new DashboardViewModel(getPortfolioUseCase, getAnalyticsUseCase, signalRepository, donationRepository, engineManager);
  }
}
