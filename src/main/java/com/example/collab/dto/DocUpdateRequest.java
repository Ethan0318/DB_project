package com.example.collab.dto;

import jakarta.validation.constraints.Size;

public class DocUpdateRequest {
    @Size(max = 80)
    private String title;

    private Long tagId;

    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
