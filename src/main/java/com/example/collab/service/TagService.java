package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.Tag;
import com.example.collab.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TagService {

    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public Tag create(Long userId, String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setCreatedBy(userId);
        tag.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        return tag;
    }

    public List<Tag> list() {
        return tagMapper.selectList(new QueryWrapper<Tag>().orderByAsc("id"));
    }
}
