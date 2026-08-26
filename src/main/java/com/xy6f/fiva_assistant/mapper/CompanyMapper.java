package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Company;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CompanyMapper {

    @Select("SELECT * FROM t_company WHERE user_id = #{userId}")
    Company findByUserId(Long userId);

    @Select("SELECT * FROM t_company WHERE id = #{id}")
    Company findById(Long id);

    @Insert("INSERT INTO t_company (user_id, name, tax_no, address, phone, bank_name, bank_account, legal_person, created_at, updated_at) " +
            "VALUES (#{userId}, #{name}, #{taxNo}, #{address}, #{phone}, #{bankName}, #{bankAccount}, #{legalPerson}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Company company);

    @Update("UPDATE t_company SET name=#{name}, tax_no=#{taxNo}, address=#{address}, phone=#{phone}, " +
            "bank_name=#{bankName}, bank_account=#{bankAccount}, legal_person=#{legalPerson}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(Company company);

    @Delete("DELETE FROM t_company WHERE id = #{id}")
    int deleteById(Long id);
}
