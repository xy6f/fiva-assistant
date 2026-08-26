package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.SubjectTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SubjectTemplateMapper {

    @Select("SELECT * FROM t_subject_template ORDER BY code ASC")
    List<SubjectTemplate> findAllOrderByCodeAsc();

    @Select("SELECT COUNT(*) FROM t_subject_template")
    long countAll();

    @Insert("INSERT INTO t_subject_template (code, name, type, balance_direction, created_at) " +
            "VALUES (#{code}, #{name}, #{type}, #{balanceDirection}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SubjectTemplate template);
}
