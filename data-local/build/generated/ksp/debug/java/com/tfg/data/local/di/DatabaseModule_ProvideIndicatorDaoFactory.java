package com.tfg.data.local.di;

import com.tfg.data.local.dao.IndicatorDao;
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
public final class DatabaseModule_ProvideIndicatorDaoFactory implements Factory<IndicatorDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideIndicatorDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public IndicatorDao get() {
    return provideIndicatorDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideIndicatorDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideIndicatorDaoFactory(dbProvider);
  }

  public static IndicatorDao provideIndicatorDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideIndicatorDao(db));
  }
}
