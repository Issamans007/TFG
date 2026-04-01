package com.tfg.feature.portfolio;

import com.tfg.domain.repository.PortfolioRepository;
import com.tfg.domain.usecase.analytics.ExportTradesUseCase;
import com.tfg.domain.usecase.analytics.GetAnalyticsUseCase;
import com.tfg.domain.usecase.analytics.GetDailyPnlUseCase;
import com.tfg.domain.usecase.analytics.GetEquityCurveUseCase;
import com.tfg.domain.usecase.analytics.GetPairPerformanceUseCase;
import com.tfg.domain.usecase.analytics.GetPortfolioUseCase;
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
public final class PortfolioViewModel_Factory implements Factory<PortfolioViewModel> {
  private final Provider<PortfolioRepository> portfolioRepositoryProvider;

  private final Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider;

  private final Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider;

  private final Provider<GetEquityCurveUseCase> getEquityCurveUseCaseProvider;

  private final Provider<GetDailyPnlUseCase> getDailyPnlUseCaseProvider;

  private final Provider<GetPairPerformanceUseCase> getPairPerformanceUseCaseProvider;

  private final Provider<ExportTradesUseCase> exportTradesUseCaseProvider;

  public PortfolioViewModel_Factory(Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider,
      Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider,
      Provider<GetEquityCurveUseCase> getEquityCurveUseCaseProvider,
      Provider<GetDailyPnlUseCase> getDailyPnlUseCaseProvider,
      Provider<GetPairPerformanceUseCase> getPairPerformanceUseCaseProvider,
      Provider<ExportTradesUseCase> exportTradesUseCaseProvider) {
    this.portfolioRepositoryProvider = portfolioRepositoryProvider;
    this.getPortfolioUseCaseProvider = getPortfolioUseCaseProvider;
    this.getAnalyticsUseCaseProvider = getAnalyticsUseCaseProvider;
    this.getEquityCurveUseCaseProvider = getEquityCurveUseCaseProvider;
    this.getDailyPnlUseCaseProvider = getDailyPnlUseCaseProvider;
    this.getPairPerformanceUseCaseProvider = getPairPerformanceUseCaseProvider;
    this.exportTradesUseCaseProvider = exportTradesUseCaseProvider;
  }

  @Override
  public PortfolioViewModel get() {
    return newInstance(portfolioRepositoryProvider.get(), getPortfolioUseCaseProvider.get(), getAnalyticsUseCaseProvider.get(), getEquityCurveUseCaseProvider.get(), getDailyPnlUseCaseProvider.get(), getPairPerformanceUseCaseProvider.get(), exportTradesUseCaseProvider.get());
  }

  public static PortfolioViewModel_Factory create(
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<GetPortfolioUseCase> getPortfolioUseCaseProvider,
      Provider<GetAnalyticsUseCase> getAnalyticsUseCaseProvider,
      Provider<GetEquityCurveUseCase> getEquityCurveUseCaseProvider,
      Provider<GetDailyPnlUseCase> getDailyPnlUseCaseProvider,
      Provider<GetPairPerformanceUseCase> getPairPerformanceUseCaseProvider,
      Provider<ExportTradesUseCase> exportTradesUseCaseProvider) {
    return new PortfolioViewModel_Factory(portfolioRepositoryProvider, getPortfolioUseCaseProvider, getAnalyticsUseCaseProvider, getEquityCurveUseCaseProvider, getDailyPnlUseCaseProvider, getPairPerformanceUseCaseProvider, exportTradesUseCaseProvider);
  }

  public static PortfolioViewModel newInstance(PortfolioRepository portfolioRepository,
      GetPortfolioUseCase getPortfolioUseCase, GetAnalyticsUseCase getAnalyticsUseCase,
      GetEquityCurveUseCase getEquityCurveUseCase, GetDailyPnlUseCase getDailyPnlUseCase,
      GetPairPerformanceUseCase getPairPerformanceUseCase,
      ExportTradesUseCase exportTradesUseCase) {
    return new PortfolioViewModel(portfolioRepository, getPortfolioUseCase, getAnalyticsUseCase, getEquityCurveUseCase, getDailyPnlUseCase, getPairPerformanceUseCase, exportTradesUseCase);
  }
}
