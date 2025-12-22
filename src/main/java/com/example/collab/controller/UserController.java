package com.example.collab.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.ApiResponse;
import com.example.collab.dto.UserProfileUpdateRequest;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    public UserController(UserService userService, UserMapper userMapper, UserProfileMapper userProfileMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> update(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }

    @PostMapping("/me/avatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.getCurrentUserId();
        String url = userService.uploadAvatar(userId, file);
        return ApiResponse.ok(Map.of("avatarUrl", url));
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam(value = "keyword", required = false) String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("email", keyword);
        }
        List<User> users = userMapper.selectList(wrapper.last("limit 20"));
        List<Map<String, Object>> result = users.stream().map(user -> {
            UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>().eq("user_id", user.getId()));
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", user.getId());
            info.put("email", user.getEmail());
            info.put("nickname", profile == null ? null : profile.getNickname());
            info.put("avatarUrl", profile == null ? null : profile.getAvatarUrl());
            return info;
        }).toList();
        return ApiResponse.ok(result);
    }
}
