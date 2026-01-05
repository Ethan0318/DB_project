package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.RoleUpdateRequest;
import com.example.collab.entity.OpLog;
import com.example.collab.entity.SurveyResponse;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AdminService;
import com.example.collab.service.OpLogService;
import com.example.collab.service.SurveyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final SurveyService surveyService;
    private final OpLogService opLogService;

    public AdminController(AdminService adminService, SurveyService surveyService, OpLogService opLogService) {
        this.adminService = adminService;
        this.surveyService = surveyService;
        this.opLogService = opLogService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        ensureAdmin();
        return ApiResponse.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable("id") Long id, @RequestBody RoleUpdateRequest request) {
        ensureAdmin();
        adminService.updateRole(id, request.getRoleCode());
        opLogService.log(SecurityUtil.getCurrentUserId(), "ROLE_UPDATE",
                "Set user " + id + " role=" + request.getRoleCode(), null);
        return ApiResponse.ok(null);
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics() {
        ensureAdmin();
        return ApiResponse.ok(adminService.analytics());
    }

    @GetMapping("/oplogs")
    public ApiResponse<List<OpLog>> logs(@RequestParam(value = "limit", defaultValue = "200") int limit) {
        ensureAdmin();
        int realLimit = Math.min(Math.max(limit, 1), 500);
        return ApiResponse.ok(adminService.recentLogs(realLimit));
    }

    @GetMapping("/survey")
    public ApiResponse<List<SurveyResponse>> surveyResponses() {
        ensureAdmin();
        return ApiResponse.ok(surveyService.listAll());
    }

    private void ensureAdmin() {
        if (!SecurityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Admin only");
        }
    }
}
