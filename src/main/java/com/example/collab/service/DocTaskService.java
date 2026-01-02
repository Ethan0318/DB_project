package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.TaskCreateRequest;
import com.example.collab.dto.TaskUpdateRequest;
import com.example.collab.entity.DocTask;
import com.example.collab.mapper.DocTaskMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class DocTaskService {

    private final DocTaskMapper taskMapper;
    private final NotificationService notificationService;

    public DocTaskService(DocTaskMapper taskMapper, NotificationService notificationService) {
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
    }

    public List<DocTask> listByDoc(Long docId) {
        return taskMapper.selectList(new QueryWrapper<DocTask>()
                .eq("doc_id", docId)
                .orderByAsc("status")
                .orderByAsc("due_date")
                .orderByDesc("created_at"));
    }

    public DocTask create(Long docId, Long creatorId, TaskCreateRequest request) {
        DocTask task = new DocTask();
        task.setDocId(docId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setAssigneeId(request.getAssigneeId());
        task.setStatus("TODO");
        task.setDueDate(parseDate(request.getDueDate()));
        task.setCreatedBy(creatorId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        if (request.getAssigneeId() != null) {
            notificationService.notifyUser(request.getAssigneeId(), "TASK_ASSIGN",
                    "{\"docId\":" + docId + ",\"taskId\":" + task.getId() + "}");
        }
        return task;
    }

    public DocTask update(Long docId, Long taskId, Long operatorId, TaskUpdateRequest request) {
        DocTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getDocId().equals(docId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Task not found");
        }
        String oldStatus = task.getStatus();
        Long oldAssignee = task.getAssigneeId();
        task.setStatus(request.getStatus());
        if (request.getAssigneeId() != null) {
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(parseDate(request.getDueDate()));
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(oldAssignee)) {
            notificationService.notifyUser(task.getAssigneeId(), "TASK_ASSIGN",
                    "{\"docId\":" + docId + ",\"taskId\":" + task.getId() + "}");
        }
        if (!oldStatus.equals(task.getStatus()) && "DONE".equalsIgnoreCase(task.getStatus())
                && task.getAssigneeId() != null && !task.getAssigneeId().equals(operatorId)) {
            notificationService.notifyUser(task.getAssigneeId(), "TASK_DONE",
                    "{\"docId\":" + docId + ",\"taskId\":" + task.getId() + "}");
        }
        return task;
    }

    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.endsWith("Z")) {
                return LocalDateTime.ofInstant(Instant.parse(raw), ZoneOffset.UTC);
            }
            return LocalDateTime.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}
