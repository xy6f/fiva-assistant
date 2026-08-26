package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.AuditLog;
import org.apache.ibatis.annotations.Insert;

public interface AuditLogMapper {

    @Insert("INSERT INTO t_audit_log(user_id, username, action, target_id, detail, ip, success) " +
            "VALUES(#{userId}, #{username}, #{action}, #{targetId}, #{detail}, #{ip}, #{success})")
    int insert(AuditLog log);
}
