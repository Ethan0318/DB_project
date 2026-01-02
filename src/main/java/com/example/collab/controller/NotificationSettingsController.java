package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.NotificationSettingsRequest;
import com.example.collab.entity.NotificationSettings;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.NotificationSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    public NotificationSettingsController(NotificationSettingsService notificationSettingsService) {
        this.notificationSettingsService = notificationSettingsService;
    }

    @GetMapping
    public ApiResponse<NotificationSettings> get() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(notificationSettingsService.getOrCreate(userId));
    }

    @PutMapping
    public ApiResponse<NotificationSettings> update(@RequestBody NotificationSettingsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(notificationSettingsService.update(userId, request));
    }
}
