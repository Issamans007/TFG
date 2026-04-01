package com.tfg.data.local.di;

import com.tfg.data.local.dao.CandleDao;
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
public final class DatabaseModule_ProvideCandleDaoFactory implements Factory<CandleDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideCandleDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CandleDao get() {
    return provideCandleDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCandleDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideCandleDaoFactory(dbProvider);
  }

  public static CandleDao provideCandleDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCandleDao(db));
  }
}
