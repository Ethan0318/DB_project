package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.CommentCreateRequest;
import com.example.collab.entity.Comment;
import com.example.collab.entity.DocPermission;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.CommentService;
import com.example.collab.service.OpLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs/{docId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final AclService aclService;
    private final OpLogService opLogService;

    public CommentController(CommentService commentService, AclService aclService, OpLogService opLogService) {
        this.commentService = commentService;
        this.aclService = aclService;
        this.opLogService = opLogService;
    }

    @GetMapping
    public ApiResponse<List<Comment>> list(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(commentService.listByDoc(docId));
    }

    @PostMapping
    public ApiResponse<Comment> create(@PathVariable("docId") Long docId,
                                       @Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        Comment comment = commentService.create(docId, userId, request.getContent(), request.getAnchor(),
                request.getParentId(), request.getAtUserId());
        opLogService.log(userId, "COMMENT_CREATE", "Comment doc " + docId + " id " + comment.getId(), null);
        return ApiResponse.ok(comment);
    }
}
