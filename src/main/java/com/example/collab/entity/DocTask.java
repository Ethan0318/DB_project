package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_tasks")
public class DocTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("doc_id")
    private Long docId;

    private String title;

    private String description;

    @TableField("assignee_id")
    private Long assigneeId;

    private String status;

    @TableField("due_date")
    private LocalDateTime dueDate;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
