package com.example.collab.websocket;

import com.example.collab.entity.ChatMessage;
import com.example.collab.entity.UserProfile;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserProfileMapper userProfileMapper;
    private final ChatService chatService;

    private final Map<Long, Set<WebSocketSession>> docRooms = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionDocMap = new ConcurrentHashMap<>();

    public CollabWebSocketHandler(UserProfileMapper userProfileMapper, ChatService chatService) {
        this.userProfileMapper = userProfileMapper;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessage wsMessage = objectMapper.readValue(message.getPayload(), WsMessage.class);
        if (wsMessage.getType() == null) {
            return;
        }
        switch (wsMessage.getType()) {
            case "join" -> handleJoin(session, wsMessage);
            case "content_update", "cursor_update" -> broadcastToDoc(wsMessage.getDocId(), wsMessage, session);
            case "chat_message" -> handleChatMessage(session, wsMessage);
            default -> {
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long docId = sessionDocMap.remove(session.getId());
        if (docId != null) {
            removeFromRoom(docId, session);
            broadcastPresence(docId);
        }
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
            }
        }
        super.afterConnectionClosed(session, status);
    }

    public void sendToUser(Long userId, WsMessage message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sessions.forEach(session -> sendMessage(session, message));
    }

    private void handleJoin(WebSocketSession session, WsMessage message) {
        Long docId = message.getDocId();
        if (docId == null) {
            return;
        }
        sessionDocMap.put(session.getId(), docId);
        docRooms.computeIfAbsent(docId, key -> ConcurrentHashMap.newKeySet()).add(session);
        attachProfile(session);
        broadcastPresence(docId);
    }

    private void handleChatMessage(WebSocketSession session, WsMessage message) {
        Long docId = message.getDocId();
        Long userId = (Long) session.getAttributes().get("userId");
        if (docId == null || userId == null) {
            return;
        }
        String content = message.getPayload() == null ? null : (String) message.getPayload().get("content");
        if (content == null || content.isBlank()) {
            return;
        }
        ChatMessage chat = new ChatMessage();
        chat.setDocId(docId);
        chat.setSenderId(userId);
        chat.setContent(content);
        chat.setCreatedAt(LocalDateTime.now());
        chatService.save(chat);

        WsMessage broadcast = new WsMessage();
        broadcast.setType("chat_message");
        broadcast.setDocId(docId);
        broadcast.setSenderId(userId);
        broadcast.setSenderName((String) session.getAttributes().get("nickname"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", chat.getId());
        payload.put("content", content);
        payload.put("createdAt", chat.getCreatedAt().toString());
        broadcast.setPayload(payload);
        broadcast.setTimestamp(System.currentTimeMillis());
        broadcastToDoc(docId, broadcast, null);
    }

    private void attachProfile(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        UserProfile profile = userProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserProfile>()
                        .eq("user_id", userId));
        if (profile != null) {
            session.getAttributes().put("nickname", profile.getNickname());
            session.getAttributes().put("avatarUrl", profile.getAvatarUrl());
        }
    }

    private void broadcastPresence(Long docId) {
        Set<WebSocketSession> sessions = docRooms.get(docId);
        if (sessions == null) {
            return;
        }
        List<Map<String, Object>> members = new ArrayList<>();
        for (WebSocketSession s : sessions) {
            Long userId = (Long) s.getAttributes().get("userId");
            if (userId == null) {
                continue;
            }
            Map<String, Object> info = new HashMap<>();
            info.put("userId", userId);
            info.put("nickname", s.getAttributes().getOrDefault("nickname", "User" + userId));
            info.put("avatarUrl", s.getAttributes().get("avatarUrl"));
            members.add(info);
        }
        WsMessage presence = new WsMessage();
        presence.setType("presence");
        presence.setDocId(docId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("members", members);
        presence.setPayload(payload);
        presence.setTimestamp(System.currentTimeMillis());
        broadcastToDoc(docId, presence, null);
    }

    private void broadcastToDoc(Long docId, WsMessage message, WebSocketSession exclude) {
        if (docId == null) {
            return;
        }
        Set<WebSocketSession> sessions = docRooms.get(docId);
        if (sessions == null) {
            return;
        }
        message.setTimestamp(System.currentTimeMillis());
        sessions.forEach(session -> {
            if (exclude != null && session.getId().equals(exclude.getId())) {
                return;
            }
            sendMessage(session, message);
        });
    }

    private void sendMessage(WebSocketSession session, WsMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception ignored) {
        }
    }

    private void removeFromRoom(Long docId, WebSocketSession session) {
        Set<WebSocketSession> sessions = docRooms.get(docId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }
}
