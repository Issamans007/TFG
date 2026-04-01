package com.tfg.security;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SecurityChecker_Factory implements Factory<SecurityChecker> {
  private final Provider<Context> contextProvider;

  public SecurityChecker_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SecurityChecker get() {
    return newInstance(contextProvider.get());
  }

  public static SecurityChecker_Factory create(Provider<Context> contextProvider) {
    return new SecurityChecker_Factory(contextProvider);
  }

  public static SecurityChecker newInstance(Context context) {
    return new SecurityChecker(context);
  }
}
