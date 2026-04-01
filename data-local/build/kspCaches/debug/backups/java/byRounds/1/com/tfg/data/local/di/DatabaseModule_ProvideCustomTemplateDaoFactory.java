package com.tfg.data.local.di;

import com.tfg.data.local.dao.CustomTemplateDao;
import com.tfg.data.local.db.TfgDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideCustomTemplateDaoFactory implements Factory<CustomTemplateDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideCustomTemplateDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CustomTemplateDao get() {
    return provideCustomTemplateDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCustomTemplateDaoFactory create(
      Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideCustomTemplateDaoFactory(dbProvider);
  }

  public static CustomTemplateDao provideCustomTemplateDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCustomTemplateDao(db));
  }
}
