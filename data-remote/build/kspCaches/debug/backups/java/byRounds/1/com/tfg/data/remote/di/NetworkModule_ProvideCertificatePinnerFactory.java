package com.tfg.data.remote.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.CertificatePinner;

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
public final class NetworkModule_ProvideCertificatePinnerFactory implements Factory<CertificatePinner> {
  @Override
  public CertificatePinner get() {
    return provideCertificatePinner();
  }

  public static NetworkModule_ProvideCertificatePinnerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CertificatePinner provideCertificatePinner() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideCertificatePinner());
  }

  private static final class InstanceHolder {
    private static final NetworkModule_ProvideCertificatePinnerFactory INSTANCE = new NetworkModule_ProvideCertificatePinnerFactory();
  }
}
