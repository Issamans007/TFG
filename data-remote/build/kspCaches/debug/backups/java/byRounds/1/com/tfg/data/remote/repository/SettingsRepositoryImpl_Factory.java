package com.tfg.data.remote.repository;

import android.content.SharedPreferences;
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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<SharedPreferences> prefsProvider;

  private final Provider<KeystoreManager> keystoreManagerProvider;

  public SettingsRepositoryImpl_Factory(Provider<SharedPreferences> prefsProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    this.prefsProvider = prefsProvider;
    this.keystoreManagerProvider = keystoreManagerProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(prefsProvider.get(), keystoreManagerProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(Provider<SharedPreferences> prefsProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    return new SettingsRepositoryImpl_Factory(prefsProvider, keystoreManagerProvider);
  }

  public static SettingsRepositoryImpl newInstance(SharedPreferences prefs,
      KeystoreManager keystoreManager) {
    return new SettingsRepositoryImpl(prefs, keystoreManager);
  }
}
