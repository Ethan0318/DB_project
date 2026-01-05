package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.entity.OpLog;
import com.example.collab.entity.Role;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.entity.UserRole;
import com.example.collab.mapper.ChatMessageMapper;
import com.example.collab.mapper.CommentMapper;
import com.example.collab.mapper.DocTaskMapper;
import com.example.collab.mapper.DocumentMapper;
import com.example.collab.mapper.NotificationMapper;
import com.example.collab.mapper.OpLogMapper;
import com.example.collab.mapper.RoleMapper;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DocumentMapper documentMapper;
    private final CommentMapper commentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DocTaskMapper docTaskMapper;
    private final NotificationMapper notificationMapper;
    private final OpLogMapper opLogMapper;

    public AdminService(UserMapper userMapper,
                        UserProfileMapper userProfileMapper,
                        UserRoleMapper userRoleMapper,
                        RoleMapper roleMapper,
                        DocumentMapper documentMapper,
                        CommentMapper commentMapper,
                        ChatMessageMapper chatMessageMapper,
                        DocTaskMapper docTaskMapper,
                        NotificationMapper notificationMapper,
                        OpLogMapper opLogMapper) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.documentMapper = documentMapper;
        this.commentMapper = commentMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.docTaskMapper = docTaskMapper;
        this.notificationMapper = notificationMapper;
        this.opLogMapper = opLogMapper;
    }

    public List<Map<String, Object>> listUsers() {
        List<User> users = userMapper.selectList(null);
        Map<Long, UserProfile> profiles = userProfileMapper.selectList(null)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        Map<Long, List<Long>> rolesByUser = userRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        Map<Long, Role> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getId, r -> r));
        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            UserProfile profile = profiles.get(user.getId());
            map.put("nickname", profile == null ? null : profile.getNickname());
            map.put("phone", profile == null ? null : profile.getPhone());
            map.put("avatarUrl", profile == null ? null : profile.getAvatarUrl());
            List<Long> roleIds = rolesByUser.getOrDefault(user.getId(), List.of());
            Set<String> roleCodes = roleIds.stream().map(roleMap::get).filter(r -> r != null)
                    .map(Role::getCode).collect(Collectors.toSet());
            map.put("roles", roleCodes);
            map.put("lastLoginAt", user.getLastLoginAt());
            map.put("createdAt", user.getCreatedAt());
            return map;
        }).toList();
    }

    public void updateRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", roleCode));
        if (role == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role not found");
        }
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId));
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    public Map<String, Object> analytics() {
        Map<String, Object> result = new HashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("docCount", documentMapper.selectCount(null));
        result.put("commentCount", commentMapper.selectCount(null));
        result.put("chatCount", chatMessageMapper.selectCount(null));
        result.put("taskCount", docTaskMapper.selectCount(null));
        result.put("notificationCount", notificationMapper.selectCount(null));
        result.put("activeUsers", opLogMapper.selectCount(new QueryWrapper<OpLog>().isNotNull("user_id")));
        return result;
    }

    public List<OpLog> recentLogs(int limit) {
        return opLogMapper.selectList(new QueryWrapper<OpLog>()
                .orderByDesc("created_at")
                .last("limit " + limit));
    }
}
