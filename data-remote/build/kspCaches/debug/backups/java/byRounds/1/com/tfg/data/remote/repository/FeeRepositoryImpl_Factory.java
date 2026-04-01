package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.FeeRecordDao;
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
public final class FeeRepositoryImpl_Factory implements Factory<FeeRepositoryImpl> {
  private final Provider<FeeRecordDao> feeRecordDaoProvider;

  public FeeRepositoryImpl_Factory(Provider<FeeRecordDao> feeRecordDaoProvider) {
    this.feeRecordDaoProvider = feeRecordDaoProvider;
  }

  @Override
  public FeeRepositoryImpl get() {
    return newInstance(feeRecordDaoProvider.get());
  }

  public static FeeRepositoryImpl_Factory create(Provider<FeeRecordDao> feeRecordDaoProvider) {
    return new FeeRepositoryImpl_Factory(feeRecordDaoProvider);
  }

  public static FeeRepositoryImpl newInstance(FeeRecordDao feeRecordDao) {
    return new FeeRepositoryImpl(feeRecordDao);
  }
}
