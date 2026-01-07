package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.BatchExportRequest;
import com.example.collab.dto.DocContentSaveRequest;
import com.example.collab.dto.DocCreateRequest;
import com.example.collab.dto.DocUpdateRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.Document;
import com.example.collab.entity.DocumentSnapshot;
import com.example.collab.entity.DocumentTemplate;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.DocumentService;
import com.example.collab.service.DocumentTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final DocumentService documentService;
    private final AclService aclService;
    private final DocumentTemplateService documentTemplateService;

    public DocumentController(DocumentService documentService, AclService aclService, DocumentTemplateService documentTemplateService) {
        this.documentService = documentService;
        this.aclService = aclService;
        this.documentTemplateService = documentTemplateService;
    }

    @PostMapping
    public ApiResponse<Document> create(@Valid @RequestBody DocCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(documentService.createDoc(userId, request));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                       @RequestParam(value = "tagId", required = false) Long tagId,
                                                       @RequestParam(value = "sort", required = false) String sort,
                                                       @RequestParam(value = "authorId", required = false) Long authorId,
                                                       @RequestParam(value = "fromDate", required = false) String fromDate,
                                                       @RequestParam(value = "toDate", required = false) String toDate) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return ApiResponse.ok(documentService.listDocSummaries(userId, isAdmin, keyword, tagId, sort, authorId, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(documentService.getDoc(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Document> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody DocUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        return ApiResponse.ok(documentService.updateDoc(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.ADMIN, SecurityUtil.isAdmin());
        documentService.deleteDoc(id, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/content")
    public ApiResponse<Map<String, Object>> content(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        return ApiResponse.ok(documentService.getDoc(id));
    }

    @PutMapping("/{id}/content")
    public ApiResponse<Map<String, Object>> saveContent(@PathVariable("id") Long id,
                                                        @Valid @RequestBody DocContentSaveRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        return ApiResponse.ok(documentService.saveContent(id, request, userId));
    }

    @GetMapping("/{id}/snapshots")
    public ApiResponse<List<DocumentSnapshot>> snapshots(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        return ApiResponse.ok(documentService.listSnapshots(id));
    }

    @PostMapping("/{id}/snapshots/{sid}/restore")
    public ApiResponse<Void> restore(@PathVariable("id") Long id, @PathVariable("sid") Long sid) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.EDIT, SecurityUtil.isAdmin());
        documentService.restoreSnapshot(id, sid);
        return ApiResponse.ok(null);
    }

    @GetMapping("/templates")
    public ApiResponse<List<DocumentTemplate>> templates() {
        return ApiResponse.ok(documentTemplateService.list());
    }

    @PostMapping("/import")
    public ApiResponse<Document> importMarkdown(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "tagId", required = false) Long tagId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(documentService.importMarkdown(userId, file, tagId));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable("id") Long id,
                                         @RequestParam(value = "format", defaultValue = "html") String format) {
        Long userId = SecurityUtil.getCurrentUserId();
        aclService.assertPermission(id, userId, DocPermission.VIEW, SecurityUtil.isAdmin());
        byte[] bytes = documentService.exportDoc(id, format);
        String filename = "doc_" + id + ("markdown".equalsIgnoreCase(format) ? ".md" : ".html");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType("markdown".equalsIgnoreCase(format) ? MediaType.TEXT_PLAIN : MediaType.TEXT_HTML)
                .body(bytes);
    }

    @PostMapping("/export/batch")
    public ResponseEntity<byte[]> batchExport(@Valid @RequestBody BatchExportRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        // basic access check: ensure current user has access to all requested docs if not admin
        if (!isAdmin) {
            List<Long> allowed = documentService.listDocs(userId, false, null, null, null, null, null, null)
                    .stream().map(Document::getId).toList();
            if (!allowed.containsAll(request.getDocIds())) {
                return ResponseEntity.status(403).build();
            }
        }
        byte[] bytes = documentService.batchExport(Set.copyOf(request.getDocIds()), request.getFormat());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=docs.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
