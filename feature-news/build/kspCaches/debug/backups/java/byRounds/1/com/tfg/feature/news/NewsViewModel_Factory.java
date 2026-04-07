package com.tfg.feature.news;

import com.tfg.feature.news.data.NewsFeedRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class NewsViewModel_Factory implements Factory<NewsViewModel> {
  private final Provider<NewsFeedRepository> repositoryProvider;

  public NewsViewModel_Factory(Provider<NewsFeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public NewsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static NewsViewModel_Factory create(Provider<NewsFeedRepository> repositoryProvider) {
    return new NewsViewModel_Factory(repositoryProvider);
  }

  public static NewsViewModel newInstance(NewsFeedRepository repository) {
    return new NewsViewModel(repository);
  }
}
