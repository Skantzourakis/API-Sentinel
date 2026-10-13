package com.apisentinel.dto.auth;

public class TokenResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;

    public TokenResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    // Lifetime of the token in seconds.
    public long getExpiresIn() {
        return expiresIn;
    }
}
