package com.tfg.feature.news.di;

import com.tfg.feature.news.data.NewsFeedRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NewsModule_ProvideNewsFeedRepositoryFactory implements Factory<NewsFeedRepository> {
  @Override
  public NewsFeedRepository get() {
    return provideNewsFeedRepository();
  }

  public static NewsModule_ProvideNewsFeedRepositoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static NewsFeedRepository provideNewsFeedRepository() {
    return Preconditions.checkNotNullFromProvides(NewsModule.INSTANCE.provideNewsFeedRepository());
  }

  private static final class InstanceHolder {
    private static final NewsModule_ProvideNewsFeedRepositoryFactory INSTANCE = new NewsModule_ProvideNewsFeedRepositoryFactory();
  }
}
