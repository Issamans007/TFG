package com.tfg.data.local.di;

import com.tfg.data.local.dao.DrawingSnapshotDao;
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
public final class DatabaseModule_ProvideDrawingSnapshotDaoFactory implements Factory<DrawingSnapshotDao> {
  private final Provider<TfgDatabase> dbProvider;

  public DatabaseModule_ProvideDrawingSnapshotDaoFactory(Provider<TfgDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DrawingSnapshotDao get() {
    return provideDrawingSnapshotDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDrawingSnapshotDaoFactory create(
      Provider<TfgDatabase> dbProvider) {
    return new DatabaseModule_ProvideDrawingSnapshotDaoFactory(dbProvider);
  }

  public static DrawingSnapshotDao provideDrawingSnapshotDao(TfgDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDrawingSnapshotDao(db));
  }
}
