package com.tfg.data.local.di;

import com.tfg.data.local.dao.AssetBalanceDao;
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
public final class DatabaseModule_ProvideAssetBalanceDaoFactory implements Factory<AssetBalanceDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideAssetBalanceDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AssetBalanceDao get() {
    return provideAssetBalanceDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAssetBalanceDaoFactory create(
      Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideAssetBalanceDaoFactory(dbProvider);
  }

  public static AssetBalanceDao provideAssetBalanceDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAssetBalanceDao(db));
  }
}
