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
public final class ScriptEngine_Factory implements Factory<ScriptEngine> {
  @Override
  public ScriptEngine get() {
    return newInstance();
  }

  public static ScriptEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScriptEngine newInstance() {
    return new ScriptEngine();
  }

  private static final class InstanceHolder {
    private static final ScriptEngine_Factory INSTANCE = new ScriptEngine_Factory();
  }
}
