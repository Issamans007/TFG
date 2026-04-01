package com.tfg.data.remote.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideWebSocketOkHttpFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return provideWebSocketOkHttp();
  }

  public static NetworkModule_ProvideWebSocketOkHttpFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient provideWebSocketOkHttp() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWebSocketOkHttp());
  }

  private static final class InstanceHolder {
    private static final NetworkModule_ProvideWebSocketOkHttpFactory INSTANCE = new NetworkModule_ProvideWebSocketOkHttpFactory();
  }
}
