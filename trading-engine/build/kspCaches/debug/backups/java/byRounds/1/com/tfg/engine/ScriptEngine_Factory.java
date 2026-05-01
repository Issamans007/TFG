package com.tfg.engine;

import com.tfg.domain.service.ConsoleBus;
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
public final class ScriptEngine_Factory implements Factory<ScriptEngine> {
  private final Provider<ConsoleBus> consoleBusProvider;

  public ScriptEngine_Factory(Provider<ConsoleBus> consoleBusProvider) {
    this.consoleBusProvider = consoleBusProvider;
  }

  @Override
  public ScriptEngine get() {
    return newInstance(consoleBusProvider.get());
  }

  public static ScriptEngine_Factory create(Provider<ConsoleBus> consoleBusProvider) {
    return new ScriptEngine_Factory(consoleBusProvider);
  }

  public static ScriptEngine newInstance(ConsoleBus consoleBus) {
    return new ScriptEngine(consoleBus);
  }
}
