package com.khaleo.flashcard.controller.auth;

import com.khaleo.flashcard.controller.auth.dto.LoginRequest;
import com.khaleo.flashcard.controller.auth.dto.LoginResponse;
import com.khaleo.flashcard.controller.auth.dto.LogoutRequest;
import com.khaleo.flashcard.controller.auth.dto.RefreshTokenRequest;
import com.khaleo.flashcard.service.auth.AuthenticationService;
import com.khaleo.flashcard.service.auth.LogoutService;
import com.khaleo.flashcard.service.auth.TokenRefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AuthenticationService authenticationService;
    private final TokenRefreshService tokenRefreshService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.LoginResult result =
                authenticationService.login(request.email(), request.password());
        return new LoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshService.RefreshResult result = tokenRefreshService.refreshAccessToken(request.refreshToken());
        return new LoginResponse(result.accessToken(), null, result.expiresIn());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        logoutService.logoutByRefreshToken(request.refreshToken());
    }
}