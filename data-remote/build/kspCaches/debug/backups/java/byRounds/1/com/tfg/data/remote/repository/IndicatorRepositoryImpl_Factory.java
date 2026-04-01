package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.IndicatorDao;
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
public final class IndicatorRepositoryImpl_Factory implements Factory<IndicatorRepositoryImpl> {
  private final Provider<IndicatorDao> indicatorDaoProvider;

  public IndicatorRepositoryImpl_Factory(Provider<IndicatorDao> indicatorDaoProvider) {
    this.indicatorDaoProvider = indicatorDaoProvider;
  }

  @Override
  public IndicatorRepositoryImpl get() {
    return newInstance(indicatorDaoProvider.get());
  }

  public static IndicatorRepositoryImpl_Factory create(
      Provider<IndicatorDao> indicatorDaoProvider) {
    return new IndicatorRepositoryImpl_Factory(indicatorDaoProvider);
  }

  public static IndicatorRepositoryImpl newInstance(IndicatorDao indicatorDao) {
    return new IndicatorRepositoryImpl(indicatorDao);
  }
}
