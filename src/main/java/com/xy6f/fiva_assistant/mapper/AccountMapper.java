package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Account;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AccountMapper {

    @Select("SELECT * FROM t_account WHERE user_id = #{userId} ORDER BY code ASC")
    List<Account> findByUserIdOrderByCodeAsc(Long userId);

    @Select("SELECT * FROM t_account WHERE user_id = #{userId} AND type = #{type} ORDER BY code ASC")
    List<Account> findByUserIdAndTypeOrderByCodeAsc(Long userId, String type);

    @Select("SELECT * FROM t_account WHERE user_id = #{userId} AND enabled = 1 ORDER BY code ASC")
    List<Account> findByUserIdAndEnabledTrueOrderByCodeAsc(Long userId);

    @Select("SELECT * FROM t_account WHERE user_id = #{userId} AND code = #{code}")
    Account findByUserIdAndCode(Long userId, String code);

    @Select("SELECT COUNT(*) FROM t_account WHERE user_id = #{userId} AND code = #{code}")
    boolean existsByUserIdAndCode(Long userId, String code);

    @Select("SELECT COUNT(*) FROM t_account WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    @Select("SELECT * FROM t_account WHERE id = #{id}")
    Account findById(Long id);

    @Select("SELECT * FROM t_account WHERE id = #{id} AND user_id = #{userId}")
    Account findByIdAndUserId(Long id, Long userId);

    @Insert("INSERT INTO t_account (user_id, code, name, type, parent_id, balance_direction, enabled, created_at) " +
            "VALUES (#{userId}, #{code}, #{name}, #{type}, #{parentId}, #{balanceDirection}, #{enabled}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account account);

    @Update("UPDATE t_account SET code=#{code}, name=#{name}, type=#{type}, parent_id=#{parentId}, " +
            "balance_direction=#{balanceDirection}, enabled=#{enabled} WHERE id=#{id}")
    int update(Account account);

    @Delete("DELETE FROM t_account WHERE id = #{id}")
    int deleteById(Long id);
}
