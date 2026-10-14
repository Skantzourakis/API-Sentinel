package com.apisentinel.service;

import com.apisentinel.dto.auth.RegisterRequest;
import com.apisentinel.exception.ConflictException;
import com.apisentinel.repository.UserAccountRepository;
import com.apisentinel.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final AuthService authService =
            new AuthService(userAccountRepository, new BCryptPasswordEncoder(4), mock(JwtService.class));

    // Two registrations at the same moment: both see no account, and the database's unique index rejects the second.
    @Test
    void registrationThatLosesTheRaceIsAConflictNotAServerError() {
        when(userAccountRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(userAccountRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate email"));

        RegisterRequest request = new RegisterRequest();
        request.setEmail("race@example.com");
        request.setPassword("supersecret1");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ConflictException.class);
    }
}
