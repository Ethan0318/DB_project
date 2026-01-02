package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocumentAcl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentAclMapper extends BaseMapper<DocumentAcl> {
    @Select("select perm from document_acl where doc_id = #{docId} and user_id = #{userId} limit 1")
    String selectPerm(@Param("docId") Long docId, @Param("userId") Long userId);

    @Select("select doc_id from document_acl where user_id = #{userId}")
    List<Long> selectDocIdsByUser(@Param("userId") Long userId);

    @Select("select user_id from document_acl where doc_id = #{docId}")
    List<Long> selectUserIdsByDoc(@Param("docId") Long docId);
}
