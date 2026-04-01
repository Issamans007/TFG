package com.tfg.feature.notifications;

import com.tfg.domain.repository.AuditRepository;
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

  public NotificationsViewModel_Factory(Provider<AuditRepository> auditRepositoryProvider) {
    this.auditRepositoryProvider = auditRepositoryProvider;
  }

  @Override
  public NotificationsViewModel get() {
    return newInstance(auditRepositoryProvider.get());
  }

  public static NotificationsViewModel_Factory create(
      Provider<AuditRepository> auditRepositoryProvider) {
    return new NotificationsViewModel_Factory(auditRepositoryProvider);
  }

  public static NotificationsViewModel newInstance(AuditRepository auditRepository) {
    return new NotificationsViewModel(auditRepository);
  }
}
