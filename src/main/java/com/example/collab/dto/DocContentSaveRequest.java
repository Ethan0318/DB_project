package com.example.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class DocContentSaveRequest {
    @NotBlank
    private String content;

    private Long clientUpdatedAt;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getClientUpdatedAt() {
        return clientUpdatedAt;
    }

    public void setClientUpdatedAt(Long clientUpdatedAt) {
        this.clientUpdatedAt = clientUpdatedAt;
    }
}
