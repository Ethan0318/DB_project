package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.TagCreateRequest;
import com.example.collab.entity.Tag;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.TagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<Tag>> list() {
        return ApiResponse.ok(tagService.list());
    }

    @PostMapping
    public ApiResponse<Tag> create(@Valid @RequestBody TagCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(tagService.create(userId, request.getName()));
    }
}
