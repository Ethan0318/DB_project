package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.TaskCreateRequest;
import com.example.collab.dto.TaskUpdateRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.DocTask;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.DocTaskService;
import com.example.collab.service.OpLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs/{docId}/tasks")
public class DocTaskController {

    private final DocTaskService docTaskService;
    private final AclService aclService;
    private final OpLogService opLogService;

    public DocTaskController(DocTaskService docTaskService, AclService aclService, OpLogService opLogService) {
        this.docTaskService = docTaskService;
        this.aclService = aclService;
        this.opLogService = opLogService;
    }

    @GetMapping
    public ApiResponse<List<DocTask>> list(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(docTaskService.listByDoc(docId));
    }

    @PostMapping
    public ApiResponse<DocTask> create(@PathVariable("docId") Long docId,
                                       @Valid @RequestBody TaskCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        DocTask task = docTaskService.create(docId, userId, request);
        opLogService.log(userId, "TASK_CREATE", "Task " + task.getId() + " for doc " + docId, null);
        return ApiResponse.ok(task);
    }

    @PutMapping("/{taskId}")
    public ApiResponse<DocTask> update(@PathVariable("docId") Long docId,
                                       @PathVariable("taskId") Long taskId,
                                       @Valid @RequestBody TaskUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        DocTask task = docTaskService.update(docId, taskId, userId, request);
        opLogService.log(userId, "TASK_UPDATE", "Task " + taskId + " -> " + request.getStatus(), null);
        return ApiResponse.ok(task);
    }
}
