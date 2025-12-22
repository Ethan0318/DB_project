package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.ChatMessage;
import com.example.collab.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    public void save(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    public List<ChatMessage> listByDoc(Long docId) {
        return chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .eq("doc_id", docId)
                .orderByAsc("created_at")
                .last("limit 50"));
    }
}
