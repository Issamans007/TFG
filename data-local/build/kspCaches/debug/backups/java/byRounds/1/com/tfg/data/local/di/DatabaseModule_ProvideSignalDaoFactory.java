package com.tfg.data.local.di;

import com.tfg.data.local.dao.SignalDao;
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
public final class DatabaseModule_ProvideSignalDaoFactory implements Factory<SignalDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideSignalDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SignalDao get() {
    return provideSignalDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSignalDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideSignalDaoFactory(dbProvider);
  }

  public static SignalDao provideSignalDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSignalDao(db));
  }
}
