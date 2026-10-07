package com.leviberga.bafafa.identity.web;

import com.leviberga.bafafa.identity.application.LoginService;
import com.leviberga.bafafa.identity.application.RegisterAccount;
import com.leviberga.bafafa.identity.application.TokenIssuer;
import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import com.leviberga.bafafa.identity.web.AuthDtos.AccountResponse;
import com.leviberga.bafafa.identity.web.AuthDtos.AuthResponse;
import com.leviberga.bafafa.identity.web.AuthDtos.LoginRequest;
import com.leviberga.bafafa.identity.web.AuthDtos.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private static final String REFRESH_COOKIE = "bafafa_refresh";

    private final RegisterAccount registerAccount;
    private final LoginService loginService;
    private final AccountRepository accounts;
    private final Clock clock;
    private final boolean cookieSecure;

    AuthController(RegisterAccount registerAccount, LoginService loginService,
                   AccountRepository accounts, Clock clock,
                   @Value("${bafafa.auth.refresh-cookie-secure:true}") boolean cookieSecure) {
        this.registerAccount = registerAccount;
        this.loginService = loginService;
        this.accounts = accounts;
        this.clock = clock;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        Account account = registerAccount.execute(
                request.email(), request.password(), request.handle(), request.displayName());
        return AccountResponse.from(account);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginService.Result result = loginService.execute(request.identifier(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString());
        return AuthResponse.of(result.accessToken().value(), result.accessToken().expiresAt(), clock.instant());
    }

    @GetMapping("/me")
    AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        return accounts.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ResponseCookie refreshCookie(TokenIssuer.RefreshTokenGrant grant) {
        return ResponseCookie.from(REFRESH_COOKIE, grant.value())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.between(clock.instant(), grant.expiresAt()))
                .build();
    }
}