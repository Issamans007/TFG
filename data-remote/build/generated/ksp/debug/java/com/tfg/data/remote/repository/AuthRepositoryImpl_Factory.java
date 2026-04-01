package com.tfg.data.remote.repository;

import android.content.SharedPreferences;
import com.tfg.data.remote.api.TfgServerApi;
import com.tfg.security.KeystoreManager;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<TfgServerApi> tfgServerApiProvider;

  private final Provider<SharedPreferences> prefsProvider;

  private final Provider<KeystoreManager> keystoreManagerProvider;

  public AuthRepositoryImpl_Factory(Provider<TfgServerApi> tfgServerApiProvider,
      Provider<SharedPreferences> prefsProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    this.tfgServerApiProvider = tfgServerApiProvider;
    this.prefsProvider = prefsProvider;
    this.keystoreManagerProvider = keystoreManagerProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(tfgServerApiProvider.get(), prefsProvider.get(), keystoreManagerProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<TfgServerApi> tfgServerApiProvider,
      Provider<SharedPreferences> prefsProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    return new AuthRepositoryImpl_Factory(tfgServerApiProvider, prefsProvider, keystoreManagerProvider);
  }

  public static AuthRepositoryImpl newInstance(TfgServerApi tfgServerApi, SharedPreferences prefs,
      KeystoreManager keystoreManager) {
    return new AuthRepositoryImpl(tfgServerApi, prefs, keystoreManager);
  }
}
