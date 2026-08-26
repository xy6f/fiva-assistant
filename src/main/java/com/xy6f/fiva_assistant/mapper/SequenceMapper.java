package com.xy6f.fiva_assistant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 序列表：基于 MySQL LAST_INSERT_ID 会话技巧实现原子自增。
 * 三个操作必须在同一事务/连接中执行（调用方方法需 @Transactional）。
 */
public interface SequenceMapper {

    @Insert("INSERT INTO t_sequence(seq_name, seq_value) VALUES(#{name}, 0) ON DUPLICATE KEY UPDATE seq_value = seq_value")
    int initSequence(@Param("name") String name);

    @Update("UPDATE t_sequence SET seq_value = LAST_INSERT_ID(seq_value + 1) WHERE seq_name = #{name}")
    int nextSequence(@Param("name") String name);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
