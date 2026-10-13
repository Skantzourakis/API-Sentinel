package com.apisentinel.service;

import com.apisentinel.dto.auth.LoginRequest;
import com.apisentinel.dto.auth.RegisterRequest;
import com.apisentinel.dto.auth.TokenResponse;
import com.apisentinel.dto.auth.UserResponse;
import com.apisentinel.entity.UserAccount;
import com.apisentinel.exception.ConflictException;
import com.apisentinel.exception.UnauthorizedException;
import com.apisentinel.repository.UserAccountRepository;
import com.apisentinel.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        UserAccount user = new UserAccount(email, passwordEncoder.encode(request.getPassword()));
        return UserResponse.from(userAccountRepository.save(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // The same message for an unknown email and a wrong password, so attackers cannot discover accounts.
        UserAccount user = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return new TokenResponse(jwtService.issueToken(user), jwtService.getExpirationSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        // A valid token whose user was removed is treated like a missing login.
        return userAccountRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
