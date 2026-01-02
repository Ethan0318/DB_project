package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.dto.SurveySubmitRequest;
import com.example.collab.entity.SurveyResponse;
import com.example.collab.mapper.SurveyResponseMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SurveyService {

    private final SurveyResponseMapper surveyResponseMapper;

    public SurveyService(SurveyResponseMapper surveyResponseMapper) {
        this.surveyResponseMapper = surveyResponseMapper;
    }

    public SurveyResponse submit(Long userId, SurveySubmitRequest request) {
        SurveyResponse response = new SurveyResponse();
        response.setUserId(userId);
        response.setRating(request.getRating());
        response.setFeedback(request.getFeedback());
        response.setCreatedAt(LocalDateTime.now());
        surveyResponseMapper.insert(response);
        return response;
    }

    public List<SurveyResponse> listAll() {
        return surveyResponseMapper.selectList(new QueryWrapper<SurveyResponse>().orderByDesc("created_at"));
    }
}
