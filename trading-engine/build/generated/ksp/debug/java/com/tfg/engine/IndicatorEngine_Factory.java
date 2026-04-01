package com.tfg.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class IndicatorEngine_Factory implements Factory<IndicatorEngine> {
  @Override
  public IndicatorEngine get() {
    return newInstance();
  }

  public static IndicatorEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static IndicatorEngine newInstance() {
    return new IndicatorEngine();
  }

  private static final class InstanceHolder {
    private static final IndicatorEngine_Factory INSTANCE = new IndicatorEngine_Factory();
  }
}
