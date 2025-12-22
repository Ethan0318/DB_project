package com.example.collab.security;

import java.util.List;

public class UserPrincipal {
    private final Long userId;
    private final String email;
    private final List<String> roles;

    public UserPrincipal(Long userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}
