package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.DonationDao;
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
public final class DonationRepositoryImpl_Factory implements Factory<DonationRepositoryImpl> {
  private final Provider<DonationDao> donationDaoProvider;

  public DonationRepositoryImpl_Factory(Provider<DonationDao> donationDaoProvider) {
    this.donationDaoProvider = donationDaoProvider;
  }

  @Override
  public DonationRepositoryImpl get() {
    return newInstance(donationDaoProvider.get());
  }

  public static DonationRepositoryImpl_Factory create(Provider<DonationDao> donationDaoProvider) {
    return new DonationRepositoryImpl_Factory(donationDaoProvider);
  }

  public static DonationRepositoryImpl newInstance(DonationDao donationDao) {
    return new DonationRepositoryImpl(donationDao);
  }
}
