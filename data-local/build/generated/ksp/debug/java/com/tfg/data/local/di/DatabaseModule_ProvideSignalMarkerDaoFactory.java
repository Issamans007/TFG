package com.tfg.data.local.di;

import com.tfg.data.local.dao.SignalMarkerDao;
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
public final class DatabaseModule_ProvideSignalMarkerDaoFactory implements Factory<SignalMarkerDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideSignalMarkerDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SignalMarkerDao get() {
    return provideSignalMarkerDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSignalMarkerDaoFactory create(
      Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideSignalMarkerDaoFactory(dbProvider);
  }

  public static SignalMarkerDao provideSignalMarkerDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSignalMarkerDao(db));
  }
}
