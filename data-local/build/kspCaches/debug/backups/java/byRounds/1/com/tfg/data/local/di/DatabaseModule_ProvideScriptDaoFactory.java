package com.tfg.data.local.di;

import com.tfg.data.local.dao.ScriptDao;
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
public final class DatabaseModule_ProvideScriptDaoFactory implements Factory<ScriptDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideScriptDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ScriptDao get() {
    return provideScriptDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideScriptDaoFactory create(Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideScriptDaoFactory(dbProvider);
  }

  public static ScriptDao provideScriptDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideScriptDao(db));
  }
}
