package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.UserProfileUpdateRequest;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public UserService(UserMapper userMapper, UserProfileMapper userProfileMapper) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>().eq("user_id", userId));
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("email", user.getEmail());
        if (profile != null) {
            info.put("nickname", profile.getNickname());
            info.put("avatarUrl", profile.getAvatarUrl());
            info.put("phone", profile.getPhone());
            info.put("bio", profile.getBio());
        }
        return info;
    }

    public Map<String, Object> updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            long exists = userMapper.selectCount(new QueryWrapper<User>()
                    .eq("email", request.getEmail())
                    .ne("id", userId));
            if (exists > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Email already in use");
            }
            user.setEmail(request.getEmail());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>().eq("user_id", userId));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(LocalDateTime.now());
        }
        if (request.getNickname() != null) {
            profile.setNickname(request.getNickname());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }
        return getProfile(userId);
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is empty");
        }
        String filename = file.getOriginalFilename();
        String suffix = "";
        if (filename != null && filename.contains(".")) {
            suffix = filename.substring(filename.lastIndexOf("."));
        }
        String storedName = "avatar_" + userId + "_" + System.currentTimeMillis() + suffix;
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            String url = "/uploads/" + storedName;
            UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>().eq("user_id", userId));
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
                profile.setCreatedAt(LocalDateTime.now());
            }
            profile.setAvatarUrl(url);
            profile.setUpdatedAt(LocalDateTime.now());
            if (profile.getId() == null) {
                userProfileMapper.insert(profile);
            } else {
                userProfileMapper.updateById(profile);
            }
            return url;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Upload failed");
        }
    }
}
