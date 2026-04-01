package com.tfg.data.remote.repository;

import com.tfg.data.local.dao.SignalDao;
import com.tfg.security.SignalVerifier;
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
public final class SignalRepositoryImpl_Factory implements Factory<SignalRepositoryImpl> {
  private final Provider<SignalDao> signalDaoProvider;

  private final Provider<SignalVerifier> signalVerifierProvider;

  public SignalRepositoryImpl_Factory(Provider<SignalDao> signalDaoProvider,
      Provider<SignalVerifier> signalVerifierProvider) {
    this.signalDaoProvider = signalDaoProvider;
    this.signalVerifierProvider = signalVerifierProvider;
  }

  @Override
  public SignalRepositoryImpl get() {
    return newInstance(signalDaoProvider.get(), signalVerifierProvider.get());
  }

  public static SignalRepositoryImpl_Factory create(Provider<SignalDao> signalDaoProvider,
      Provider<SignalVerifier> signalVerifierProvider) {
    return new SignalRepositoryImpl_Factory(signalDaoProvider, signalVerifierProvider);
  }

  public static SignalRepositoryImpl newInstance(SignalDao signalDao,
      SignalVerifier signalVerifier) {
    return new SignalRepositoryImpl(signalDao, signalVerifier);
  }
}
