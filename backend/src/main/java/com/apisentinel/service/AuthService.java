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
import com.apisentinel.security.RateLimits;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimits rateLimits;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RateLimits rateLimits
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimits = rateLimits;
    }

    @Transactional
    public UserResponse register(RegisterRequest request, String clientAddress) {
        rateLimits.checkRegistration(clientAddress);
        String email = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        UserAccount user = new UserAccount(email, passwordEncoder.encode(request.getPassword()));
        try {
            return UserResponse.from(userAccountRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same email can both pass the check above at the same moment;
            // the unique index on email stops the second one, which gets the same answer.
            throw new ConflictException("An account with this email already exists");
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request, String clientAddress) {
        String email = normalizeEmail(request.getEmail());
        // Checked before the password, so a blocked attacker learns nothing and costs no BCrypt work.
        rateLimits.checkLogin(clientAddress, email);

        // The same message for an unknown email and a wrong password, so attackers cannot discover accounts.
        UserAccount user = userAccountRepository.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        rateLimits.loginSucceeded(email);
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
