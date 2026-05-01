package com.tfg.feature.notifications;

import com.tfg.domain.repository.AuditRepository;
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
public final class NotificationsViewModel_Factory implements Factory<NotificationsViewModel> {
  private final Provider<AuditRepository> auditRepositoryProvider;

  private final Provider<ConsoleBus> consoleBusProvider;

  public NotificationsViewModel_Factory(Provider<AuditRepository> auditRepositoryProvider,
      Provider<ConsoleBus> consoleBusProvider) {
    this.auditRepositoryProvider = auditRepositoryProvider;
    this.consoleBusProvider = consoleBusProvider;
  }

  @Override
  public NotificationsViewModel get() {
    return newInstance(auditRepositoryProvider.get(), consoleBusProvider.get());
  }

  public static NotificationsViewModel_Factory create(
      Provider<AuditRepository> auditRepositoryProvider, Provider<ConsoleBus> consoleBusProvider) {
    return new NotificationsViewModel_Factory(auditRepositoryProvider, consoleBusProvider);
  }

  public static NotificationsViewModel newInstance(AuditRepository auditRepository,
      ConsoleBus consoleBus) {
    return new NotificationsViewModel(auditRepository, consoleBus);
  }
}
