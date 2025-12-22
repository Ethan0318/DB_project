package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.AuthResponse;
import com.example.collab.dto.LoginRequest;
import com.example.collab.dto.RegisterRequest;
import com.example.collab.dto.ResetConfirmRequest;
import com.example.collab.dto.ResetRequest;
import com.example.collab.entity.PasswordResetCode;
import com.example.collab.entity.Role;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.entity.UserRole;
import com.example.collab.mapper.PasswordResetCodeMapper;
import com.example.collab.mapper.RoleMapper;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.mapper.UserRoleMapper;
import com.example.collab.util.JwtUtil;
import com.example.collab.util.RandomCodeUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordResetCodeMapper passwordResetCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OpLogService opLogService;

    public AuthService(UserMapper userMapper,
                       UserProfileMapper userProfileMapper,
                       UserRoleMapper userRoleMapper,
                       RoleMapper roleMapper,
                       PasswordResetCodeMapper passwordResetCodeMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OpLogService opLogService) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordResetCodeMapper = passwordResetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.opLogService = opLogService;
    }

    public AuthResponse login(LoginRequest request, String ip) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        List<String> roles = getRoles(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        Map<String, Object> userInfo = buildUserInfo(user);
        opLogService.log(user.getId(), "LOGIN", "User login", ip);
        return new AuthResponse(token, userInfo);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userMapper.selectCount(new QueryWrapper<User>().eq("email", request.getEmail())) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Email already registered");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(request.getNickname());
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);

        Role editorRole = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", "EDITOR"));
        if (editorRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(editorRole.getId());
            userRoleMapper.insert(userRole);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), List.of("EDITOR"));
        Map<String, Object> userInfo = buildUserInfo(user);
        return new AuthResponse(token, userInfo);
    }

    public Map<String, Object> requestReset(ResetRequest request) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        String code = RandomCodeUtil.numericCode(6);
        PasswordResetCode reset = new PasswordResetCode();
        reset.setUserId(user.getId());
        reset.setCode(code);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        reset.setUsed(0);
        reset.setCreatedAt(LocalDateTime.now());
        passwordResetCodeMapper.insert(reset);
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("code", code);
        response.put("expiresInMinutes", 10);
        return response;
    }

    public void confirmReset(ResetConfirmRequest request) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        PasswordResetCode reset = passwordResetCodeMapper.selectOne(new QueryWrapper<PasswordResetCode>()
                .eq("user_id", user.getId())
                .eq("code", request.getCode())
                .eq("used", 0)
                .orderByDesc("id")
                .last("limit 1"));
        if (reset == null || reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid or expired code");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        reset.setUsed(1);
        passwordResetCodeMapper.updateById(reset);
    }

    private List<String> getRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(Role::getCode).collect(Collectors.toList());
    }

    private Map<String, Object> buildUserInfo(User user) {
        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>().eq("user_id", user.getId()));
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
}
