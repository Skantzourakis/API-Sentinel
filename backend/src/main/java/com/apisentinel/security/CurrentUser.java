package com.apisentinel.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    // JwtService puts the user id in the token subject.
    public static Long idOf(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
