package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.dto.NotificationSettingsRequest;
import com.example.collab.entity.NotificationSettings;
import com.example.collab.mapper.NotificationSettingsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationSettingsService {

    private final NotificationSettingsMapper settingsMapper;

    public NotificationSettingsService(NotificationSettingsMapper settingsMapper) {
        this.settingsMapper = settingsMapper;
    }

    public NotificationSettings getOrCreate(Long userId) {
        NotificationSettings settings = settingsMapper.selectOne(new QueryWrapper<NotificationSettings>().eq("user_id", userId));
        if (settings != null) {
            return settings;
        }
        NotificationSettings created = new NotificationSettings();
        created.setUserId(userId);
        created.setEditEnabled(1);
        created.setCommentEnabled(1);
        created.setTaskEnabled(1);
        created.setShareEnabled(1);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        settingsMapper.insert(created);
        return created;
    }

    public NotificationSettings update(Long userId, NotificationSettingsRequest request) {
        NotificationSettings settings = getOrCreate(userId);
        if (request.getEditEnabled() != null) {
            settings.setEditEnabled(request.getEditEnabled() ? 1 : 0);
        }
        if (request.getCommentEnabled() != null) {
            settings.setCommentEnabled(request.getCommentEnabled() ? 1 : 0);
        }
        if (request.getTaskEnabled() != null) {
            settings.setTaskEnabled(request.getTaskEnabled() ? 1 : 0);
        }
        if (request.getShareEnabled() != null) {
            settings.setShareEnabled(request.getShareEnabled() ? 1 : 0);
        }
        settings.setUpdatedAt(LocalDateTime.now());
        settingsMapper.updateById(settings);
        return settings;
    }

    public boolean enabled(Long userId, String type) {
        NotificationSettings settings = getOrCreate(userId);
        return switch (type) {
            case "DOC_EDIT" -> settings.getEditEnabled() != null && settings.getEditEnabled() == 1;
            case "MENTION", "REPLY", "COMMENT" ->
                    settings.getCommentEnabled() != null && settings.getCommentEnabled() == 1;
            case "TASK_ASSIGN", "TASK_DONE" ->
                    settings.getTaskEnabled() != null && settings.getTaskEnabled() == 1;
            case "DOC_SHARE" -> settings.getShareEnabled() != null && settings.getShareEnabled() == 1;
            default -> true;
        };
    }
}
