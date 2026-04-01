package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.AuditLogDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AuditRepositoryImpl_Factory implements Factory<AuditRepositoryImpl> {
  private final Provider<AuditLogDao> auditLogDaoProvider;

  public AuditRepositoryImpl_Factory(Provider<AuditLogDao> auditLogDaoProvider) {
    this.auditLogDaoProvider = auditLogDaoProvider;
  }

  @Override
  public AuditRepositoryImpl get() {
    return newInstance(auditLogDaoProvider.get());
  }

  public static AuditRepositoryImpl_Factory create(Provider<AuditLogDao> auditLogDaoProvider) {
    return new AuditRepositoryImpl_Factory(auditLogDaoProvider);
  }

  public static AuditRepositoryImpl newInstance(AuditLogDao auditLogDao) {
    return new AuditRepositoryImpl(auditLogDao);
  }
}
