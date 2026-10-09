package com.leviberga.bafafa.identity.web;

import com.leviberga.bafafa.identity.application.AuthSession;
import com.leviberga.bafafa.identity.application.InvalidRefreshTokenException;
import com.leviberga.bafafa.identity.application.LoginService;
import com.leviberga.bafafa.identity.application.LogoutService;
import com.leviberga.bafafa.identity.application.RefreshSessionService;
import com.leviberga.bafafa.identity.application.RegisterAccount;
import com.leviberga.bafafa.identity.application.TokenIssuer;
import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import com.leviberga.bafafa.identity.web.AuthDtos.AccountResponse;
import com.leviberga.bafafa.identity.web.AuthDtos.AuthResponse;
import com.leviberga.bafafa.identity.web.AuthDtos.LoginRequest;
import com.leviberga.bafafa.identity.web.AuthDtos.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
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
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final RegisterAccount registerAccount;
    private final LoginService loginService;
    private final RefreshSessionService refreshSessionService;
    private final LogoutService logoutService;
    private final AccountRepository accounts;
    private final Clock clock;
    private final boolean cookieSecure;

    AuthController(RegisterAccount registerAccount, LoginService loginService,
                   RefreshSessionService refreshSessionService, LogoutService logoutService,
                   AccountRepository accounts, Clock clock,
                   @Value("${bafafa.auth.refresh-cookie-secure:true}") boolean cookieSecure) {
        this.registerAccount = registerAccount;
        this.loginService = loginService;
        this.refreshSessionService = refreshSessionService;
        this.logoutService = logoutService;
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
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = loginService.execute(request.identifier(), request.password());
        return sessionResponse(session);
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        try {
            if (refreshToken == null) {
                throw new InvalidRefreshTokenException();
            }
            return sessionResponse(refreshSessionService.execute(refreshToken));
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada."));
        }
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            logoutService.execute(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        return accounts.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ResponseEntity<AuthResponse> sessionResponse(AuthSession session) {
        AuthResponse body = AuthResponse.of(
                session.accessToken().value(), session.accessToken().expiresAt(), clock.instant());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(TokenIssuer.RefreshTokenGrant grant) {
        return ResponseCookie.from(REFRESH_COOKIE, grant.value())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.between(clock.instant(), grant.expiresAt()))
                .build();
    }

    private ResponseCookie clearedRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}