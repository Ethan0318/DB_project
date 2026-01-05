package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.UserProfileUpdateRequest;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.entity.Role;
import com.example.collab.entity.UserRole;
import com.example.collab.mapper.RoleMapper;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.mapper.UserRoleMapper;
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
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public UserService(UserMapper userMapper, UserProfileMapper userProfileMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
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
        info.put("roles", getRoles(user.getId()));
        return info;
    }

    public Map<String, Object> updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        if (email != null && !email.isBlank()) {
            long exists = userMapper.selectCount(new QueryWrapper<User>()
                    .eq("email", email)
                    .ne("id", userId));
            if (exists > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Email already in use");
            }
            user.setEmail(email);
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
        if (phone != null && !phone.isBlank()) {
            long exists = userProfileMapper.selectCount(new QueryWrapper<UserProfile>()
                    .eq("phone", phone)
                    .ne("user_id", userId));
            if (exists > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Phone already in use");
            }
            profile.setPhone(phone);
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
            String url = saveFile(file, storedName);
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

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is empty");
        }
        String filename = file.getOriginalFilename();
        String suffix = "";
        if (filename != null && filename.contains(".")) {
            suffix = filename.substring(filename.lastIndexOf("."));
        }
        String storedName = "file_" + System.currentTimeMillis() + suffix;
        try {
            return saveFile(file, storedName);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Upload failed");
        }
    }

    private String saveFile(MultipartFile file, String storedName) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        file.transferTo(target);
        return "/uploads/" + storedName;
    }

    private java.util.List<String> getRoles(Long userId) {
        java.util.List<UserRole> userRoles = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", userId));
        if (userRoles.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        java.util.List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(Role::getCode).toList();
    }
}
