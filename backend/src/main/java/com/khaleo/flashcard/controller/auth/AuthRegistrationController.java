package com.khaleo.flashcard.controller.auth;

import com.khaleo.flashcard.controller.auth.dto.RegisterRequest;
import com.khaleo.flashcard.controller.auth.dto.RegisterResponse;
import com.khaleo.flashcard.controller.auth.dto.VerifyEmailResponse;
import com.khaleo.flashcard.service.auth.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationService.RegisterResult result = registrationService.registerGuest(request.email(), request.password());
        return new RegisterResponse(result.userId(), result.email(), result.verificationRequired());
    }

    @GetMapping("/verify")
    public VerifyEmailResponse verify(@RequestParam("token") String token) {
        registrationService.verifyEmailToken(token);
        return new VerifyEmailResponse(true);
    }
}
