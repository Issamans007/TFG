package com.tfg.feature.risk;

import com.tfg.domain.repository.AuditRepository;
import com.tfg.domain.repository.RiskRepository;
import com.tfg.engine.RiskEngine;
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
public final class RiskViewModel_Factory implements Factory<RiskViewModel> {
  private final Provider<RiskRepository> riskRepositoryProvider;

  private final Provider<RiskEngine> riskEngineProvider;

  private final Provider<AuditRepository> auditRepositoryProvider;

  public RiskViewModel_Factory(Provider<RiskRepository> riskRepositoryProvider,
      Provider<RiskEngine> riskEngineProvider, Provider<AuditRepository> auditRepositoryProvider) {
    this.riskRepositoryProvider = riskRepositoryProvider;
    this.riskEngineProvider = riskEngineProvider;
    this.auditRepositoryProvider = auditRepositoryProvider;
  }

  @Override
  public RiskViewModel get() {
    return newInstance(riskRepositoryProvider.get(), riskEngineProvider.get(), auditRepositoryProvider.get());
  }

  public static RiskViewModel_Factory create(Provider<RiskRepository> riskRepositoryProvider,
      Provider<RiskEngine> riskEngineProvider, Provider<AuditRepository> auditRepositoryProvider) {
    return new RiskViewModel_Factory(riskRepositoryProvider, riskEngineProvider, auditRepositoryProvider);
  }

  public static RiskViewModel newInstance(RiskRepository riskRepository, RiskEngine riskEngine,
      AuditRepository auditRepository) {
    return new RiskViewModel(riskRepository, riskEngine, auditRepository);
  }
}
