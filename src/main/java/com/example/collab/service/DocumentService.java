package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.common.BusinessException;
import com.example.collab.common.ErrorCode;
import com.example.collab.dto.DocContentSaveRequest;
import com.example.collab.dto.DocCreateRequest;
import com.example.collab.dto.DocUpdateRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.Document;
import com.example.collab.entity.DocumentAcl;
import com.example.collab.entity.DocumentContent;
import com.example.collab.entity.DocumentSnapshot;
import com.example.collab.entity.DocumentTemplate;
import com.example.collab.mapper.DocumentAclMapper;
import com.example.collab.mapper.DocumentContentMapper;
import com.example.collab.mapper.DocumentMapper;
import com.example.collab.mapper.DocumentSnapshotMapper;
import com.example.collab.mapper.DocumentTemplateMapper;
import com.example.collab.util.HtmlUtil;
import com.example.collab.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentContentMapper documentContentMapper;
    private final DocumentAclMapper documentAclMapper;
    private final DocumentSnapshotMapper documentSnapshotMapper;
    private final DocumentTemplateMapper documentTemplateMapper;
    private final OpLogService opLogService;
    private final NotificationService notificationService;

    public DocumentService(DocumentMapper documentMapper,
                           DocumentContentMapper documentContentMapper,
                           DocumentAclMapper documentAclMapper,
                           DocumentSnapshotMapper documentSnapshotMapper,
                           DocumentTemplateMapper documentTemplateMapper,
                           OpLogService opLogService,
                           NotificationService notificationService) {
        this.documentMapper = documentMapper;
        this.documentContentMapper = documentContentMapper;
        this.documentAclMapper = documentAclMapper;
        this.documentSnapshotMapper = documentSnapshotMapper;
        this.documentTemplateMapper = documentTemplateMapper;
        this.opLogService = opLogService;
        this.notificationService = notificationService;
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
        String initialContent = "<p></p>";
        if (request.getTemplateId() != null) {
            DocumentTemplate template = documentTemplateMapper.selectById(request.getTemplateId());
            if (template != null) {
                initialContent = template.getContent();
            }
        }
        content.setContent(initialContent);
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

    public List<Document> listDocs(Long userId, boolean isAdmin, String keyword, Long tagId, String sort,
                                   Long authorId, String fromDate, String toDate) {
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
        if (authorId != null) {
            wrapper.eq("owner_id", authorId);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            wrapper.ge("created_at", LocalDate.parse(fromDate).atStartOfDay());
        }
        if (toDate != null && !toDate.isBlank()) {
            wrapper.le("created_at", LocalDate.parse(toDate).atTime(23, 59, 59));
        }
        if ("title".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc("title");
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

    public Map<String, Object> saveContent(Long docId, DocContentSaveRequest request, Long operatorId) {
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
        notifyEditors(docId, doc.getTitle(), operatorId, request.getClientUpdatedAt());
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

    public Document importMarkdown(Long userId, MultipartFile file, Long tagId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is empty");
        }
        String title = file.getOriginalFilename();
        if (title == null || title.isBlank()) {
            title = "Imported Doc";
        } else if (title.contains(".")) {
            title = title.substring(0, title.lastIndexOf('.'));
        }
        String md;
        try {
            md = new String(file.getBytes());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Read file failed");
        }
        String html = markdownToHtml(md);
        DocCreateRequest createRequest = new DocCreateRequest();
        createRequest.setTitle(title);
        createRequest.setTagId(tagId);
        Document doc = createDoc(userId, createRequest);
        DocumentContent content = documentContentMapper.selectOne(new QueryWrapper<DocumentContent>().eq("doc_id", doc.getId()));
        content.setContent(html);
        content.setUpdatedAt(LocalDateTime.now());
        documentContentMapper.updateById(content);
        doc.setSearchText(HtmlUtil.stripHtml(html));
        documentMapper.updateById(doc);
        return doc;
    }

    public byte[] exportDoc(Long docId, String format) {
        DocumentContent content = documentContentMapper.selectOne(new QueryWrapper<DocumentContent>().eq("doc_id", docId));
        String html = content == null ? "" : content.getContent();
        if ("markdown".equalsIgnoreCase(format)) {
            return HtmlUtil.stripHtml(html).getBytes();
        }
        return html.getBytes();
    }

    public byte[] batchExport(Set<Long> docIds, String format) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Long docId : docIds) {
                Document doc = documentMapper.selectById(docId);
                if (doc == null) {
                    continue;
                }
                String filename = doc.getTitle().replaceAll("[^a-zA-Z0-9_-]", "_");
                filename = filename.isBlank() ? "doc_" + docId : filename;
                if ("markdown".equalsIgnoreCase(format)) {
                    filename += ".md";
                } else {
                    filename += ".html";
                }
                zos.putNextEntry(new ZipEntry(filename));
                zos.write(exportDoc(docId, format));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Export failed");
        }
    }

    private String markdownToHtml(String md) {
        if (md == null || md.isBlank()) {
            return "<p></p>";
        }
        String[] lines = md.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("### ")) {
                sb.append("<h3>").append(line.substring(4)).append("</h3>");
            } else if (line.startsWith("## ")) {
                sb.append("<h2>").append(line.substring(3)).append("</h2>");
            } else if (line.startsWith("# ")) {
                sb.append("<h1>").append(line.substring(2)).append("</h1>");
            } else {
                sb.append("<p>").append(line).append("</p>");
            }
        }
        return sb.toString();
    }

    private void notifyEditors(Long docId, String title, Long operatorId, Long clientUpdatedAt) {
        List<Long> userIds = documentAclMapper.selectUserIdsByDoc(docId);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String payload = "{\"docId\":" + docId + ",\"title\":\"" + title + "\"}";
        Set<Long> unique = new HashSet<>(userIds);
        for (Long uid : unique) {
            if (operatorId != null && operatorId.equals(uid)) {
                continue;
            }
            notificationService.notifyUser(uid, "DOC_EDIT", payload, clientUpdatedAt);
        }
    }
}
