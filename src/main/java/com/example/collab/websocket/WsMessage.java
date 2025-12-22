package com.example.collab.websocket;

import lombok.Data;

import java.util.Map;

@Data
public class WsMessage {
    private String type;
    private Long docId;
    private Long senderId;
    private String senderName;
    private Map<String, Object> payload;
    private Long timestamp;
}
