package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.AclGrantRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.DocumentAcl;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.NotificationService;
import com.example.collab.service.OpLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docs/{docId}/acl")
public class AclController {

    private final AclService aclService;
    private final NotificationService notificationService;
    private final OpLogService opLogService;

    public AclController(AclService aclService, NotificationService notificationService, OpLogService opLogService) {
        this.aclService = aclService;
        this.notificationService = notificationService;
        this.opLogService = opLogService;
    }

    @GetMapping
    public ApiResponse<List<DocumentAcl>> list(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.ADMIN, SecurityUtil.isAdmin());
        return ApiResponse.ok(aclService.list(docId));
    }

    @PostMapping
    public ApiResponse<Void> grant(@PathVariable("docId") Long docId,
                                   @Valid @RequestBody AclGrantRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, userId, DocPermission.ADMIN, SecurityUtil.isAdmin());
        aclService.grant(docId, request);
        opLogService.log(userId, "ACL_GRANT", "Grant doc " + docId + " to user " + request.getUserId(), null);
        if (request.getUserId() != null) {
            notificationService.notifyUser(request.getUserId(), "DOC_SHARE",
                    "{\"docId\":" + docId + ",\"perm\":\"" + request.getPerm() + "\"}");
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> revoke(@PathVariable("docId") Long docId, @PathVariable("userId") Long userId) {
        Long operator = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(docId, operator, DocPermission.ADMIN, SecurityUtil.isAdmin());
        aclService.revoke(docId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> myPerm(@PathVariable("docId") Long docId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(aclService.buildPermInfo(docId, userId, SecurityUtil.isAdmin()));
    }
}
