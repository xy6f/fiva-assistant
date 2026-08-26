package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContactMapper {

    @Select("SELECT * FROM t_contact WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC")
    List<Contact> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    @Select("SELECT * FROM t_contact WHERE user_id = #{userId} AND (name LIKE CONCAT('%', #{keyword}, '%') OR contact_person LIKE CONCAT('%', #{keyword}, '%')) ORDER BY created_at DESC")
    List<Contact> searchByKeyword(Long userId, String keyword);

    @Select("SELECT * FROM t_contact WHERE user_id = #{userId} AND type = #{type} AND name LIKE CONCAT('%', #{name}, '%') ORDER BY created_at DESC")
    List<Contact> findByUserIdAndTypeAndNameContainingOrderByCreatedAtDesc(Long userId, String type, String name);

    @Select("SELECT * FROM t_contact WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Contact> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_contact WHERE id = #{id}")
    Contact findById(Long id);

    @Insert("INSERT INTO t_contact (user_id, name, type, contact_person, phone, email, address, tax_no, bank_name, bank_account, remark, created_at, updated_at) " +
            "VALUES (#{userId}, #{name}, #{type}, #{contactPerson}, #{phone}, #{email}, #{address}, #{taxNo}, #{bankName}, #{bankAccount}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Contact contact);

    @Update("UPDATE t_contact SET name=#{name}, type=#{type}, contact_person=#{contactPerson}, phone=#{phone}, " +
            "email=#{email}, address=#{address}, tax_no=#{taxNo}, bank_name=#{bankName}, bank_account=#{bankAccount}, " +
            "remark=#{remark}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(Contact contact);

    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteById(Long id);
}
