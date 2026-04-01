package com.tfg.engine;

import android.content.Context;
import com.tfg.data.remote.websocket.WebSocketManager;
import com.tfg.domain.repository.AlertRepository;
import com.tfg.domain.repository.AuditRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AlertMonitor_Factory implements Factory<AlertMonitor> {
  private final Provider<Context> contextProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<AuditRepository> auditRepositoryProvider;

  public AlertMonitor_Factory(Provider<Context> contextProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<AuditRepository> auditRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.auditRepositoryProvider = auditRepositoryProvider;
  }

  @Override
  public AlertMonitor get() {
    return newInstance(contextProvider.get(), alertRepositoryProvider.get(), webSocketManagerProvider.get(), auditRepositoryProvider.get());
  }

  public static AlertMonitor_Factory create(Provider<Context> contextProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<AuditRepository> auditRepositoryProvider) {
    return new AlertMonitor_Factory(contextProvider, alertRepositoryProvider, webSocketManagerProvider, auditRepositoryProvider);
  }

  public static AlertMonitor newInstance(Context context, AlertRepository alertRepository,
      WebSocketManager webSocketManager, AuditRepository auditRepository) {
    return new AlertMonitor(context, alertRepository, webSocketManager, auditRepository);
  }
}
