package com.example.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class TagCreateRequest {
    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
