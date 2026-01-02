package com.example.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleUpdateRequest {
    @NotBlank
    private String roleCode;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
