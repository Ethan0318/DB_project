package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.SurveySubmitRequest;
import com.example.collab.entity.SurveyResponse;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping
    public ApiResponse<SurveyResponse> submit(@Valid @RequestBody SurveySubmitRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(surveyService.submit(userId, request));
    }
}
