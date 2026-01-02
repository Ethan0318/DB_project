package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_settings")
public class NotificationSettings {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("edit_enabled")
    private Integer editEnabled;

    @TableField("comment_enabled")
    private Integer commentEnabled;

    @TableField("task_enabled")
    private Integer taskEnabled;

    @TableField("share_enabled")
    private Integer shareEnabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
