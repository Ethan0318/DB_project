package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.entity.Notification;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Notification>> list(@RequestParam(value = "type", required = false) String type,
                                                @RequestParam(value = "readFlag", required = false) Integer readFlag) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(notificationService.list(userId, type, readFlag));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markRead(userId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllRead(userId);
        return ApiResponse.ok(null);
    }
}
