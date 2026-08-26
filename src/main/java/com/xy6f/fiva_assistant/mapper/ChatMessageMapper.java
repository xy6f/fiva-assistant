package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Select("SELECT * FROM t_chat_message WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY created_at ASC")
    List<ChatMessage> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, Long userId);

    @Delete("DELETE FROM t_chat_message WHERE session_id = #{sessionId} AND user_id = #{userId}")
    int deleteBySessionIdAndUserId(String sessionId, Long userId);

    @Insert("INSERT INTO t_chat_message (user_id, session_id, role, content, created_at) " +
            "VALUES (#{userId}, #{sessionId}, #{role}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage chatMessage);
}
