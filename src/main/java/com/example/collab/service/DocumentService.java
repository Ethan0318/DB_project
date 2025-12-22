package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.DocContentSaveRequest;
import com.example.collab.dto.DocCreateRequest;
import com.example.collab.dto.DocUpdateRequest;
import com.example.collab.entity.Document;
import com.example.collab.entity.DocumentAcl;
import com.example.collab.entity.DocumentContent;
import com.example.collab.entity.DocumentSnapshot;
import com.example.collab.entity.DocPermission;
import com.example.collab.mapper.DocumentAclMapper;
import com.example.collab.mapper.DocumentContentMapper;
import com.example.collab.mapper.DocumentMapper;
import com.example.collab.mapper.DocumentSnapshotMapper;
import com.example.collab.util.HtmlUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentContentMapper documentContentMapper;
    private final DocumentAclMapper documentAclMapper;
    private final DocumentSnapshotMapper documentSnapshotMapper;
    private final OpLogService opLogService;

    public DocumentService(DocumentMapper documentMapper,
                           DocumentContentMapper documentContentMapper,
                           DocumentAclMapper documentAclMapper,
                           DocumentSnapshotMapper documentSnapshotMapper,
                           OpLogService opLogService) {
        this.documentMapper = documentMapper;
        this.documentContentMapper = documentContentMapper;
        this.documentAclMapper = documentAclMapper;
        this.documentSnapshotMapper = documentSnapshotMapper;
        this.opLogService = opLogService;
    }

    public Document createDoc(Long userId, DocCreateRequest request) {
        Document doc = new Document();
        doc.setTitle(request.getTitle());
        doc.setOwnerId(userId);
        doc.setTagId(request.getTagId());
        doc.setStatus("ACTIVE");
        doc.setDeleted(0);
        doc.setSearchText(request.getTitle());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);

        DocumentContent content = new DocumentContent();
        content.setDocId(doc.getId());
        content.setContent("<p></p>");
        content.setUpdatedAt(LocalDateTime.now());
        documentContentMapper.insert(content);

        DocumentAcl acl = new DocumentAcl();
        acl.setDocId(doc.getId());
        acl.setUserId(userId);
        acl.setPerm(DocPermission.ADMIN.name());
        acl.setCreatedAt(LocalDateTime.now());
        documentAclMapper.insert(acl);

        opLogService.log(userId, "DOC_CREATE", "Create doc " + doc.getId(), null);
        return doc;
    }

    public List<Document> listDocs(Long userId, boolean isAdmin, String keyword, Long tagId, String sort) {
        QueryWrapper<Document> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (!isAdmin) {
            List<Long> docIds = documentAclMapper.selectDocIdsByUser(userId);
            if (docIds.isEmpty()) {
                return List.of();
            }
            wrapper.in("id", docIds);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("title", keyword).or().like("search_text", keyword));
        }
        if (tagId != null) {
            wrapper.eq("tag_id", tagId);
        }
        if ("updated".equalsIgnoreCase(sort) || sort == null) {
            wrapper.orderByDesc("updated_at");
        } else {
            wrapper.orderByDesc("updated_at");
        }
        return documentMapper.selectList(wrapper);
    }

    public Map<String, Object> getDoc(Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Doc not found");
        }
        DocumentContent content = documentContentMapper.selectOne(new QueryWrapper<DocumentContent>().eq("doc_id", docId));
        Map<String, Object> result = new HashMap<>();
        result.put("doc", doc);
        result.put("content", content == null ? "" : content.getContent());
        result.put("updatedAt", content == null ? null : content.getUpdatedAt());
        return result;
    }

    public Document updateDoc(Long docId, DocUpdateRequest request) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Doc not found");
        }
        if (request.getTitle() != null) {
            doc.setTitle(request.getTitle());
        }
        if (request.getTagId() != null) {
            doc.setTagId(request.getTagId());
        }
        if (request.getStatus() != null) {
            doc.setStatus(request.getStatus());
        }
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return doc;
    }

    public void deleteDoc(Long docId, Long userId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Doc not found");
        }
        doc.setDeleted(1);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        opLogService.log(userId, "DOC_DELETE", "Delete doc " + docId, null);
    }

    public Map<String, Object> saveContent(Long docId, DocContentSaveRequest request) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Doc not found");
        }
        DocumentContent content = documentContentMapper.selectOne(new QueryWrapper<DocumentContent>().eq("doc_id", docId));
        if (content == null) {
            content = new DocumentContent();
            content.setDocId(docId);
        }
        content.setContent(request.getContent());
        content.setUpdatedAt(LocalDateTime.now());
        if (content.getId() == null) {
            documentContentMapper.insert(content);
        } else {
            documentContentMapper.updateById(content);
        }
        doc.setSearchText(HtmlUtil.stripHtml(request.getContent()));
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        saveSnapshot(docId, request.getContent());
        Map<String, Object> result = new HashMap<>();
        result.put("updatedAt", content.getUpdatedAt());
        return result;
    }

    public List<DocumentSnapshot> listSnapshots(Long docId) {
        return documentSnapshotMapper.selectList(new QueryWrapper<DocumentSnapshot>()
                .eq("doc_id", docId)
                .orderByDesc("created_at")
                .last("limit 20"));
    }

    public void restoreSnapshot(Long docId, Long snapshotId) {
        DocumentSnapshot snapshot = documentSnapshotMapper.selectById(snapshotId);
        if (snapshot == null || !snapshot.getDocId().equals(docId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Snapshot not found");
        }
        DocumentContent content = documentContentMapper.selectOne(new QueryWrapper<DocumentContent>().eq("doc_id", docId));
        if (content == null) {
            content = new DocumentContent();
            content.setDocId(docId);
        }
        content.setContent(snapshot.getContent());
        content.setUpdatedAt(LocalDateTime.now());
        if (content.getId() == null) {
            documentContentMapper.insert(content);
        } else {
            documentContentMapper.updateById(content);
        }
        Document doc = documentMapper.selectById(docId);
        doc.setSearchText(HtmlUtil.stripHtml(snapshot.getContent()));
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
    }

    private void saveSnapshot(Long docId, String content) {
        DocumentSnapshot snapshot = new DocumentSnapshot();
        snapshot.setDocId(docId);
        snapshot.setContent(content);
        snapshot.setCreatedAt(LocalDateTime.now());
        documentSnapshotMapper.insert(snapshot);
    }
}
