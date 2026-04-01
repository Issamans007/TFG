package com.tfg.data.remote.repository;

import com.google.gson.Gson;
import com.tfg.data.local.dao.OfflineQueueDao;
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
public final class OfflineQueueRepositoryImpl_Factory implements Factory<OfflineQueueRepositoryImpl> {
  private final Provider<OfflineQueueDao> offlineQueueDaoProvider;

  private final Provider<Gson> gsonProvider;

  public OfflineQueueRepositoryImpl_Factory(Provider<OfflineQueueDao> offlineQueueDaoProvider,
      Provider<Gson> gsonProvider) {
    this.offlineQueueDaoProvider = offlineQueueDaoProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public OfflineQueueRepositoryImpl get() {
    return newInstance(offlineQueueDaoProvider.get(), gsonProvider.get());
  }

  public static OfflineQueueRepositoryImpl_Factory create(
      Provider<OfflineQueueDao> offlineQueueDaoProvider, Provider<Gson> gsonProvider) {
    return new OfflineQueueRepositoryImpl_Factory(offlineQueueDaoProvider, gsonProvider);
  }

  public static OfflineQueueRepositoryImpl newInstance(OfflineQueueDao offlineQueueDao, Gson gson) {
    return new OfflineQueueRepositoryImpl(offlineQueueDao, gson);
  }
}
