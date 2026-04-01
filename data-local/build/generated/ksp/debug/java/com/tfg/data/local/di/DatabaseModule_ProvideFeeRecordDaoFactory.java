package com.tfg.data.local.di;

import com.tfg.data.local.dao.FeeRecordDao;
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
public final class DatabaseModule_ProvideFeeRecordDaoFactory implements Factory<FeeRecordDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideFeeRecordDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public FeeRecordDao get() {
    return provideFeeRecordDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideFeeRecordDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideFeeRecordDaoFactory(dbProvider);
  }

  public static FeeRecordDao provideFeeRecordDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFeeRecordDao(db));
  }
}
