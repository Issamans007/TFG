package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.AlertDao;
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
public final class AlertRepositoryImpl_Factory implements Factory<AlertRepositoryImpl> {
  private final Provider<AlertDao> alertDaoProvider;

  public AlertRepositoryImpl_Factory(Provider<AlertDao> alertDaoProvider) {
    this.alertDaoProvider = alertDaoProvider;
  }

  @Override
  public AlertRepositoryImpl get() {
    return newInstance(alertDaoProvider.get());
  }

  public static AlertRepositoryImpl_Factory create(Provider<AlertDao> alertDaoProvider) {
    return new AlertRepositoryImpl_Factory(alertDaoProvider);
  }

  public static AlertRepositoryImpl newInstance(AlertDao alertDao) {
    return new AlertRepositoryImpl(alertDao);
  }
}
