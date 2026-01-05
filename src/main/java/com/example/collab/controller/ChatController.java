package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.entity.ChatMessage;
import com.example.collab.entity.DocPermission;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.ChatService;
import com.example.collab.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docs/{docId}/chat")
public class ChatController {

    private final ChatService chatService;
    private final AclService aclService;
    private final UserService userService;

    public ChatController(ChatService chatService, AclService aclService, UserService userService) {
        this.chatService = chatService;
        this.aclService = aclService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<ChatMessage>> list(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(chatService.listByDoc(docId));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@PathVariable("docId") Long docId,
                                                   @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        String url = userService.uploadFile(file);
        return ApiResponse.ok(Map.of("url", url, "name", file.getOriginalFilename()));
    }
}
