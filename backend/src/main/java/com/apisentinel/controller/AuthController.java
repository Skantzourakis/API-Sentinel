package com.apisentinel.controller;

import com.apisentinel.dto.auth.LoginRequest;
import com.apisentinel.dto.auth.RegisterRequest;
import com.apisentinel.dto.auth.TokenResponse;
import com.apisentinel.dto.auth.UserResponse;
import com.apisentinel.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirements // Public endpoints: overrides the global bearer requirement in the OpenAPI document.
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // getRemoteAddr() is the address of the direct caller. X-Forwarded-For is deliberately not read:
    // any client can set it to a new value on every request and so escape the per-address limits.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr());
    }
}
