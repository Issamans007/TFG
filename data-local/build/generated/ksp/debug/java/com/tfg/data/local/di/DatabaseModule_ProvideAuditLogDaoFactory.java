package com.tfg.data.local.di;

import com.tfg.data.local.dao.AuditLogDao;
import com.tfg.data.local.db.TfgDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideAuditLogDaoFactory implements Factory<AuditLogDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideAuditLogDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AuditLogDao get() {
    return provideAuditLogDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAuditLogDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideAuditLogDaoFactory(dbProvider);
  }

  public static AuditLogDao provideAuditLogDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAuditLogDao(db));
  }
}
