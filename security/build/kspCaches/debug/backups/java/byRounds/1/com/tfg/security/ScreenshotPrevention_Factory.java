package com.tfg.security;

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
public final class ScreenshotPrevention_Factory implements Factory<ScreenshotPrevention> {
  @Override
  public ScreenshotPrevention get() {
    return newInstance();
  }

  public static ScreenshotPrevention_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScreenshotPrevention newInstance() {
    return new ScreenshotPrevention();
  }

  private static final class InstanceHolder {
    private static final ScreenshotPrevention_Factory INSTANCE = new ScreenshotPrevention_Factory();
  }
}
