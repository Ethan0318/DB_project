package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_templates")
public class DocumentTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
