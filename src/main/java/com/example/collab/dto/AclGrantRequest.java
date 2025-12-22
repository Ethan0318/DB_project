package com.example.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class AclGrantRequest {
    private Long userId;

    @NotBlank
    private String perm;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPerm() {
        return perm;
    }

    public void setPerm(String perm) {
        this.perm = perm;
    }
}
