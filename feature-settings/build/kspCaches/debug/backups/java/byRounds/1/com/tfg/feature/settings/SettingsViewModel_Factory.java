package com.tfg.feature.settings;

import android.content.Context;
import com.tfg.domain.repository.RiskRepository;
import com.tfg.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<RiskRepository> riskRepositoryProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<RiskRepository> riskRepositoryProvider, Provider<Context> contextProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.riskRepositoryProvider = riskRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), riskRepositoryProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<RiskRepository> riskRepositoryProvider, Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(settingsRepositoryProvider, riskRepositoryProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settingsRepository,
      RiskRepository riskRepository, Context context) {
    return new SettingsViewModel(settingsRepository, riskRepository, context);
  }
}
