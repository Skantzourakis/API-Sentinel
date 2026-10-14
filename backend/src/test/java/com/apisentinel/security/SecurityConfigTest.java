package com.apisentinel.security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    // Without a configured secret there must be no shared, guessable key: every start gets its own.
    @Test
    void withoutASecretEveryStartGetsItsOwnRandomKey() {
        SecretKey first = SecurityConfig.jwtKey("");
        SecretKey second = SecurityConfig.jwtKey(null);

        assertThat(first.getEncoded()).hasSize(32);
        assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }

    @Test
    void usesTheConfiguredSecret() {
        String secret = "a-configured-secret-of-at-least-32-bytes";

        assertThat(SecurityConfig.jwtKey(secret).getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsSecretsShorterThan256Bits() {
        assertThatThrownBy(() -> SecurityConfig.jwtKey("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }
}
