package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.AclGrantRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.DocumentAcl;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.mapper.DocumentAclMapper;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AclService {

    private final DocumentAclMapper documentAclMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    public AclService(DocumentAclMapper documentAclMapper, UserMapper userMapper, UserProfileMapper userProfileMapper) {
        this.documentAclMapper = documentAclMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    public void assertPermission(Long docId, Long userId, DocPermission required, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        String perm = documentAclMapper.selectPerm(docId, userId);
        if (perm == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "No permission");
        }
        DocPermission current = DocPermission.valueOf(perm);
        if (level(current) < level(required)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "No permission");
        }
    }

    public void grant(Long docId, AclGrantRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "UserId required");
        }
        DocPermission perm = DocPermission.valueOf(request.getPerm());
        DocumentAcl existing = documentAclMapper.selectOne(new QueryWrapper<DocumentAcl>()
                .eq("doc_id", docId)
                .eq("user_id", request.getUserId()));
        if (existing == null) {
            DocumentAcl acl = new DocumentAcl();
            acl.setDocId(docId);
            acl.setUserId(request.getUserId());
            acl.setPerm(perm.name());
            acl.setCreatedAt(LocalDateTime.now());
            documentAclMapper.insert(acl);
        } else {
            existing.setPerm(perm.name());
            documentAclMapper.updateById(existing);
        }
    }

    public void revoke(Long docId, Long userId) {
        documentAclMapper.delete(new QueryWrapper<DocumentAcl>()
                .eq("doc_id", docId)
                .eq("user_id", userId));
    }

    public List<DocumentAcl> list(Long docId) {
        return documentAclMapper.selectList(new QueryWrapper<DocumentAcl>().eq("doc_id", docId));
    }

    public List<Map<String, Object>> listDetail(Long docId) {
        List<DocumentAcl> list = list(docId);
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = list.stream().map(DocumentAcl::getUserId).filter(id -> id != null)
                .collect(HashSet::new, Set::add, Set::addAll);
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, UserProfile> profileMap = userIds.isEmpty() ? Map.of()
                : userProfileMapper.selectList(new QueryWrapper<UserProfile>().in("user_id", userIds)).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        return list.stream().map(acl -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", acl.getUserId());
            item.put("perm", acl.getPerm());
            item.put("docId", acl.getDocId());
            User user = acl.getUserId() == null ? null : userMap.get(acl.getUserId());
            UserProfile profile = acl.getUserId() == null ? null : profileMap.get(acl.getUserId());
            String userName = profile != null && profile.getNickname() != null && !profile.getNickname().isBlank()
                    ? profile.getNickname()
                    : user != null ? user.getEmail() : null;
            item.put("userName", userName);
            item.put("email", user != null ? user.getEmail() : null);
            item.put("avatarUrl", profile != null ? profile.getAvatarUrl() : null);
            return item;
        }).toList();
    }

    public Map<String, Object> buildPermInfo(Long docId, Long userId, boolean isAdmin) {
        Map<String, Object> info = new HashMap<>();
        info.put("docId", docId);
        info.put("userId", userId);
        if (isAdmin) {
            info.put("perm", DocPermission.ADMIN.name());
        } else {
            info.put("perm", documentAclMapper.selectPerm(docId, userId));
        }
        return info;
    }

    private int level(DocPermission perm) {
        return switch (perm) {
            case VIEW -> 1;
            case EDIT -> 2;
            case ADMIN -> 3;
        };
    }
}
