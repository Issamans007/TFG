package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.DonationDao;
import com.tfg.data.local.dao.FeeRecordDao;
import com.tfg.data.local.dao.OrderDao;
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
public final class AnalyticsRepositoryImpl_Factory implements Factory<AnalyticsRepositoryImpl> {
  private final Provider<OrderDao> orderDaoProvider;

  private final Provider<FeeRecordDao> feeRecordDaoProvider;

  private final Provider<DonationDao> donationDaoProvider;

  public AnalyticsRepositoryImpl_Factory(Provider<OrderDao> orderDaoProvider,
      Provider<FeeRecordDao> feeRecordDaoProvider, Provider<DonationDao> donationDaoProvider) {
    this.orderDaoProvider = orderDaoProvider;
    this.feeRecordDaoProvider = feeRecordDaoProvider;
    this.donationDaoProvider = donationDaoProvider;
  }

  @Override
  public AnalyticsRepositoryImpl get() {
    return newInstance(orderDaoProvider.get(), feeRecordDaoProvider.get(), donationDaoProvider.get());
  }

  public static AnalyticsRepositoryImpl_Factory create(Provider<OrderDao> orderDaoProvider,
      Provider<FeeRecordDao> feeRecordDaoProvider, Provider<DonationDao> donationDaoProvider) {
    return new AnalyticsRepositoryImpl_Factory(orderDaoProvider, feeRecordDaoProvider, donationDaoProvider);
  }

  public static AnalyticsRepositoryImpl newInstance(OrderDao orderDao, FeeRecordDao feeRecordDao,
      DonationDao donationDao) {
    return new AnalyticsRepositoryImpl(orderDao, feeRecordDao, donationDao);
  }
}
