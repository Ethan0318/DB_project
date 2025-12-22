package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.AclGrantRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.DocumentAcl;
import com.example.collab.mapper.DocumentAclMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AclService {

    private final DocumentAclMapper documentAclMapper;

    public AclService(DocumentAclMapper documentAclMapper) {
        this.documentAclMapper = documentAclMapper;
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
