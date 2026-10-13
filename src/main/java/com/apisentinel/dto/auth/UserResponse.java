package com.apisentinel.dto.auth;

import com.apisentinel.entity.UserAccount;

import java.time.Instant;

public class UserResponse {

    private Long id;
    private String email;
    private Instant createdAt;

    public static UserResponse from(UserAccount user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.email = user.getEmail();
        response.createdAt = user.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
