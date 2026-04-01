package com.tfg.data.local.di;

import com.tfg.data.local.dao.OrderDao;
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
public final class DatabaseModule_ProvideOrderDaoFactory implements Factory<OrderDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideOrderDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public OrderDao get() {
    return provideOrderDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideOrderDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideOrderDaoFactory(dbProvider);
  }

  public static OrderDao provideOrderDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideOrderDao(db));
  }
}
