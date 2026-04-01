package com.tfg.data.remote.interceptor;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlin.jvm.functions.Function0;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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
public final class BinanceAuthInterceptor_Factory implements Factory<BinanceAuthInterceptor> {
  private final Provider<Function0<String>> apiKeyProvider;

  public BinanceAuthInterceptor_Factory(Provider<Function0<String>> apiKeyProvider) {
    this.apiKeyProvider = apiKeyProvider;
  }

  @Override
  public BinanceAuthInterceptor get() {
    return newInstance(apiKeyProvider.get());
  }

  public static BinanceAuthInterceptor_Factory create(Provider<Function0<String>> apiKeyProvider) {
    return new BinanceAuthInterceptor_Factory(apiKeyProvider);
  }

  public static BinanceAuthInterceptor newInstance(Function0<String> apiKeyProvider) {
    return new BinanceAuthInterceptor(apiKeyProvider);
  }
}
