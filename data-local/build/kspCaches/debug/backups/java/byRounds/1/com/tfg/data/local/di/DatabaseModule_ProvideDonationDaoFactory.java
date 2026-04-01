package com.tfg.data.local.di;

import com.tfg.data.local.dao.DonationDao;
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
public final class DatabaseModule_ProvideDonationDaoFactory implements Factory<DonationDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideDonationDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DonationDao get() {
    return provideDonationDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDonationDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideDonationDaoFactory(dbProvider);
  }

  public static DonationDao provideDonationDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDonationDao(db));
  }
}
