package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Alert;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AlertMapper {

    @Select("SELECT * FROM t_alert WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<Alert> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    @Select("SELECT * FROM t_alert WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT COUNT(*) FROM t_alert WHERE status = #{status} AND user_id = #{userId}")
    long countByStatusAndUserId(String status, Long userId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_alert WHERE status IN " +
            "<foreach item='status' collection='statuses' open='(' separator=',' close=')'>" +
            "#{status}" +
            "</foreach>" +
            " AND user_id = #{userId}" +
            "</script>")
    long countByStatusInAndUserId(List<String> statuses, Long userId);

    @Select("SELECT * FROM t_alert WHERE user_id = #{userId} AND type = #{type} AND status = #{status} ORDER BY created_at DESC")
    List<Alert> findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(Long userId, String type, String status);

    @Select("SELECT * FROM t_alert WHERE id = #{id}")
    Alert findById(Long id);

    @Insert("INSERT INTO t_alert (user_id, type, level, title, content, status, related_id, created_at, read_at) " +
            "VALUES (#{userId}, #{type}, #{level}, #{title}, #{content}, #{status}, #{relatedId}, #{createdAt}, #{readAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Alert alert);

    @Update("UPDATE t_alert SET type=#{type}, level=#{level}, title=#{title}, content=#{content}, " +
            "status=#{status}, related_id=#{relatedId}, read_at=#{readAt} WHERE id=#{id}")
    int update(Alert alert);

    @Delete("DELETE FROM t_alert WHERE id = #{id}")
    int deleteById(Long id);
}
