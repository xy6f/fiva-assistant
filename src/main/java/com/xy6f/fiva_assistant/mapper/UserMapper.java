package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Role;
import com.xy6f.fiva_assistant.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    boolean existsByUsername(String username);

    @Select("SELECT * FROM t_user WHERE enabled = 1")
    List<User> findByEnabledTrue();

    @Select("SELECT * FROM t_user WHERE role = #{role}")
    List<User> findByRole(Role role);

    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM t_user")
    List<User> findAll();

    @Insert("INSERT INTO t_user (username, password, real_name, email, phone, department, company_id, role, enabled, last_login_at, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{realName}, #{email}, #{phone}, #{department}, #{companyId}, #{role}, #{enabled}, #{lastLoginAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET username=#{username}, password=#{password}, real_name=#{realName}, email=#{email}, " +
            "phone=#{phone}, department=#{department}, company_id=#{companyId}, role=#{role}, enabled=#{enabled}, " +
            "last_login_at=#{lastLoginAt}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM t_user WHERE id = #{id}")
    int deleteById(Long id);
}
