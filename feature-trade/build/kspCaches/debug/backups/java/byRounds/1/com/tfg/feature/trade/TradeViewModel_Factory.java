package com.tfg.feature.trade;

import androidx.lifecycle.SavedStateHandle;
import com.tfg.domain.repository.MarketRepository;
import com.tfg.domain.usecase.trading.CancelOrderUseCase;
import com.tfg.domain.usecase.trading.GetOpenOrdersUseCase;
import com.tfg.domain.usecase.trading.GetOrderHistoryUseCase;
import com.tfg.domain.usecase.trading.PlaceOrderUseCase;
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
public final class TradeViewModel_Factory implements Factory<TradeViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<PlaceOrderUseCase> placeOrderUseCaseProvider;

  private final Provider<CancelOrderUseCase> cancelOrderUseCaseProvider;

  private final Provider<GetOpenOrdersUseCase> getOpenOrdersUseCaseProvider;

  private final Provider<GetOrderHistoryUseCase> getOrderHistoryUseCaseProvider;

  private final Provider<MarketRepository> marketRepositoryProvider;

  public TradeViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<PlaceOrderUseCase> placeOrderUseCaseProvider,
      Provider<CancelOrderUseCase> cancelOrderUseCaseProvider,
      Provider<GetOpenOrdersUseCase> getOpenOrdersUseCaseProvider,
      Provider<GetOrderHistoryUseCase> getOrderHistoryUseCaseProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.placeOrderUseCaseProvider = placeOrderUseCaseProvider;
    this.cancelOrderUseCaseProvider = cancelOrderUseCaseProvider;
    this.getOpenOrdersUseCaseProvider = getOpenOrdersUseCaseProvider;
    this.getOrderHistoryUseCaseProvider = getOrderHistoryUseCaseProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
  }

  @Override
  public TradeViewModel get() {
    return newInstance(savedStateHandleProvider.get(), placeOrderUseCaseProvider.get(), cancelOrderUseCaseProvider.get(), getOpenOrdersUseCaseProvider.get(), getOrderHistoryUseCaseProvider.get(), marketRepositoryProvider.get());
  }

  public static TradeViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<PlaceOrderUseCase> placeOrderUseCaseProvider,
      Provider<CancelOrderUseCase> cancelOrderUseCaseProvider,
      Provider<GetOpenOrdersUseCase> getOpenOrdersUseCaseProvider,
      Provider<GetOrderHistoryUseCase> getOrderHistoryUseCaseProvider,
      Provider<MarketRepository> marketRepositoryProvider) {
    return new TradeViewModel_Factory(savedStateHandleProvider, placeOrderUseCaseProvider, cancelOrderUseCaseProvider, getOpenOrdersUseCaseProvider, getOrderHistoryUseCaseProvider, marketRepositoryProvider);
  }

  public static TradeViewModel newInstance(SavedStateHandle savedStateHandle,
      PlaceOrderUseCase placeOrderUseCase, CancelOrderUseCase cancelOrderUseCase,
      GetOpenOrdersUseCase getOpenOrdersUseCase, GetOrderHistoryUseCase getOrderHistoryUseCase,
      MarketRepository marketRepository) {
    return new TradeViewModel(savedStateHandle, placeOrderUseCase, cancelOrderUseCase, getOpenOrdersUseCase, getOrderHistoryUseCase, marketRepository);
  }
}
