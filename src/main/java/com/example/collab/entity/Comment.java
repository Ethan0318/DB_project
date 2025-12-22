package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("doc_id")
    private Long docId;

    @TableField("user_id")
    private Long userId;

    private String content;

    private String anchor;

    @TableField("parent_id")
    private Long parentId;

    @TableField("at_user_id")
    private Long atUserId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
