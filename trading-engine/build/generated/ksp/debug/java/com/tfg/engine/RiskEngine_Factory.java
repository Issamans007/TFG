package com.tfg.engine;

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
public final class RiskEngine_Factory implements Factory<RiskEngine> {
  private final Provider<Context> contextProvider;

  public RiskEngine_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RiskEngine get() {
    return newInstance(contextProvider.get());
  }

  public static RiskEngine_Factory create(Provider<Context> contextProvider) {
    return new RiskEngine_Factory(contextProvider);
  }

  public static RiskEngine newInstance(Context context) {
    return new RiskEngine(context);
  }
}
