package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.entity.ChatMessage;
import com.example.collab.entity.DocPermission;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs/{docId}/chat")
public class ChatController {

    private final ChatService chatService;
    private final AclService aclService;

    public ChatController(ChatService chatService, AclService aclService) {
        this.chatService = chatService;
        this.aclService = aclService;
    }

    @GetMapping
    public ApiResponse<List<ChatMessage>> list(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(chatService.listByDoc(docId));
    }
}
