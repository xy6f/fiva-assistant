package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.VoucherTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VoucherTemplateMapper {

    @Select("SELECT * FROM t_voucher_template WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<VoucherTemplate> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_voucher_template WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC")
    List<VoucherTemplate> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    @Select("SELECT * FROM t_voucher_template WHERE id = #{id}")
    VoucherTemplate findById(Long id);

    @Insert("INSERT INTO t_voucher_template (user_id, name, type, description, entries_json, created_at, updated_at) " +
            "VALUES (#{userId}, #{name}, #{type}, #{description}, #{entriesJson}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VoucherTemplate template);

    @Update("UPDATE t_voucher_template SET name=#{name}, type=#{type}, description=#{description}, " +
            "entries_json=#{entriesJson}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(VoucherTemplate template);

    @Delete("DELETE FROM t_voucher_template WHERE id = #{id}")
    int deleteById(Long id);
}
