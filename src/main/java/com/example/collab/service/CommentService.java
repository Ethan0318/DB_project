package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.Comment;
import com.example.collab.entity.Notification;
import com.example.collab.mapper.CommentMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    public CommentService(CommentMapper commentMapper, NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.notificationService = notificationService;
    }

    public Comment create(Long docId, Long userId, String content, String anchor, Long parentId, Long atUserId) {
        Comment comment = new Comment();
        comment.setDocId(docId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setAnchor(anchor);
        comment.setParentId(parentId);
        comment.setAtUserId(atUserId);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        if (atUserId != null && !atUserId.equals(userId)) {
            notificationService.notifyUser(atUserId, "MENTION",
                    "{\"docId\":" + docId + ",\"commentId\":" + comment.getId() + "}");
        }
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent != null && !parent.getUserId().equals(userId)) {
                notificationService.notifyUser(parent.getUserId(), "REPLY",
                        "{\"docId\":" + docId + ",\"commentId\":" + comment.getId() + "}");
            }
        }
        return comment;
    }

    public List<Comment> listByDoc(Long docId) {
        return commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("doc_id", docId)
                .orderByAsc("created_at"));
    }
}
