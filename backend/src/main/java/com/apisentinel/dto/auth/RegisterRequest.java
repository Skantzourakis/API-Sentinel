package com.apisentinel.dto.auth;

import com.apisentinel.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    // BCrypt refuses new passwords over 72 bytes, and letters outside English (such as Greek) take two bytes each,
    // so the limit is checked in bytes; a character limit let such passwords through to a server error.
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @MaxUtf8Bytes(value = 72, message = "Password is too long: at most 72 bytes, and letters such as Greek count as two")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
