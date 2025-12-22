package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.Notification;
import com.example.collab.mapper.NotificationMapper;
import com.example.collab.websocket.CollabWebSocketHandler;
import com.example.collab.websocket.WsMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final CollabWebSocketHandler webSocketHandler;

    public NotificationService(NotificationMapper notificationMapper, CollabWebSocketHandler webSocketHandler) {
        this.notificationMapper = notificationMapper;
        this.webSocketHandler = webSocketHandler;
    }

    public Notification notifyUser(Long userId, String type, String payload) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setPayload(payload);
        notification.setReadFlag(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        WsMessage ws = new WsMessage();
        ws.setType("notification");
        ws.setSenderId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", notification.getId());
        data.put("type", notification.getType());
        data.put("payload", notification.getPayload());
        data.put("createdAt", notification.getCreatedAt().toString());
        ws.setPayload(data);
        webSocketHandler.sendToUser(userId, ws);
        return notification;
    }

    public List<Notification> list(Long userId, String type, Integer readFlag) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<Notification>()
                .eq("user_id", userId)
                .orderByDesc("created_at");
        if (type != null && !type.isBlank()) {
            wrapper.eq("type", type);
        }
        if (readFlag != null) {
            wrapper.eq("read_flag", readFlag);
        }
        return notificationMapper.selectList(wrapper);
    }

    public void markRead(Long userId, Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return;
        }
        notification.setReadFlag(1);
        notificationMapper.updateById(notification);
    }

    public void markAllRead(Long userId) {
        List<Notification> list = notificationMapper.selectList(new QueryWrapper<Notification>().eq("user_id", userId));
        for (Notification n : list) {
            n.setReadFlag(1);
            notificationMapper.updateById(n);
        }
    }
}
