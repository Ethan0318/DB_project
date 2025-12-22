package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.DocContentSaveRequest;
import com.example.collab.dto.DocCreateRequest;
import com.example.collab.dto.DocUpdateRequest;
import com.example.collab.entity.DocPermission;
import com.example.collab.entity.Document;
import com.example.collab.entity.DocumentSnapshot;
import com.example.collab.security.SecurityUtil;
import com.example.collab.service.AclService;
import com.example.collab.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final DocumentService documentService;
    private final AclService aclService;

    public DocumentController(DocumentService documentService, AclService aclService) {
        this.documentService = documentService;
        this.aclService = aclService;
    }

    @PostMapping
    public ApiResponse<Document> create(@Valid @RequestBody DocCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(documentService.createDoc(userId, request));
    }

    @GetMapping
    public ApiResponse<List<Document>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "tagId", required = false) Long tagId,
                                            @RequestParam(value = "sort", required = false) String sort) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.isAdmin();
        return ApiResponse.ok(documentService.listDocs(userId, isAdmin, keyword, tagId, sort));
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
        return ApiResponse.ok(documentService.saveContent(id, request));
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
}
