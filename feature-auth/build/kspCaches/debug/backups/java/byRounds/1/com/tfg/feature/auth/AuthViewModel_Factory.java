package com.tfg.feature.auth;

import com.tfg.domain.usecase.auth.LoginUseCase;
import com.tfg.domain.usecase.auth.RegisterUseCase;
import com.tfg.domain.usecase.auth.SetupTradingPinUseCase;
import com.tfg.domain.usecase.auth.VerifyOtpUseCase;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<LoginUseCase> loginUseCaseProvider;

  private final Provider<RegisterUseCase> registerUseCaseProvider;

  private final Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider;

  private final Provider<SetupTradingPinUseCase> setupTradingPinUseCaseProvider;

  public AuthViewModel_Factory(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<RegisterUseCase> registerUseCaseProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<SetupTradingPinUseCase> setupTradingPinUseCaseProvider) {
    this.loginUseCaseProvider = loginUseCaseProvider;
    this.registerUseCaseProvider = registerUseCaseProvider;
    this.verifyOtpUseCaseProvider = verifyOtpUseCaseProvider;
    this.setupTradingPinUseCaseProvider = setupTradingPinUseCaseProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(loginUseCaseProvider.get(), registerUseCaseProvider.get(), verifyOtpUseCaseProvider.get(), setupTradingPinUseCaseProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<RegisterUseCase> registerUseCaseProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<SetupTradingPinUseCase> setupTradingPinUseCaseProvider) {
    return new AuthViewModel_Factory(loginUseCaseProvider, registerUseCaseProvider, verifyOtpUseCaseProvider, setupTradingPinUseCaseProvider);
  }

  public static AuthViewModel newInstance(LoginUseCase loginUseCase,
      RegisterUseCase registerUseCase, VerifyOtpUseCase verifyOtpUseCase,
      SetupTradingPinUseCase setupTradingPinUseCase) {
    return new AuthViewModel(loginUseCase, registerUseCase, verifyOtpUseCase, setupTradingPinUseCase);
  }
}
