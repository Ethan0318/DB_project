package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.DocumentTemplate;
import com.example.collab.mapper.DocumentTemplateMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentTemplateService {

    private final DocumentTemplateMapper templateMapper;

    public DocumentTemplateService(DocumentTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<DocumentTemplate> list() {
        ensureDefaults();
        return templateMapper.selectList(new QueryWrapper<DocumentTemplate>().orderByAsc("id"));
    }

    public DocumentTemplate findById(Long id) {
        return templateMapper.selectById(id);
    }

    private void ensureDefaults() {
        if (templateMapper.selectCount(null) > 0) {
            return;
        }
        createTemplate("Meeting Notes", "<h2>Meeting Notes</h2><p>Attendees:</p><ul><li></li></ul><p>Agenda:</p><ol><li></li></ol><p>Decisions:</p><p></p>");
        createTemplate("Product Spec", "<h2>Product Spec</h2><p>Summary:</p><p></p><h3>Goals</h3><ul><li></li></ul><h3>Requirements</h3><ul><li></li></ul>");
        createTemplate("Research Doc", "<h2>Research</h2><p>Background:</p><p></p><h3>Findings</h3><ul><li></li></ul><h3>Next steps</h3><p></p>");
    }

    private void createTemplate(String name, String content) {
        DocumentTemplate tpl = new DocumentTemplate();
        tpl.setName(name);
        tpl.setContent(content);
        tpl.setCreatedAt(LocalDateTime.now());
        templateMapper.insert(tpl);
    }
}
