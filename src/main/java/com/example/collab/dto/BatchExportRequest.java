package com.example.collab.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BatchExportRequest {
    @NotEmpty
    private List<Long> docIds;

    @NotNull
    private String format;

    public List<Long> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<Long> docIds) {
        this.docIds = docIds;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
