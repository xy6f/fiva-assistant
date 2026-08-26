package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.SalaryRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SalaryRecordMapper {

    @Select("SELECT * FROM t_salary_record WHERE user_id = #{userId} AND month = #{month} ORDER BY department ASC, employee_name ASC")
    List<SalaryRecord> findByUserIdAndMonthOrderByDepartmentAscEmployeeNameAsc(Long userId, String month);

    @Select("SELECT * FROM t_salary_record WHERE user_id = #{userId} ORDER BY month DESC, created_at DESC")
    List<SalaryRecord> findByUserIdOrderByMonthDescCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_salary_record WHERE id = #{id}")
    SalaryRecord findById(Long id);

    @Select("SELECT * FROM t_salary_record WHERE user_id = #{userId} AND employee_no = #{employeeNo} AND month = #{month} LIMIT 1")
    SalaryRecord findByUserIdAndEmployeeNoAndMonth(Long userId, String employeeNo, String month);

    @Insert("INSERT INTO t_salary_record (user_id, employee_name, employee_no, department, position, " +
            "base_salary, bonus, allowance, overtime, gross_pay, social_insurance, housing_fund, tax, " +
            "other_deduction, net_pay, month, status, created_at) " +
            "VALUES (#{userId}, #{employeeName}, #{employeeNo}, #{department}, #{position}, " +
            "#{baseSalary}, #{bonus}, #{allowance}, #{overtime}, #{grossPay}, #{socialInsurance}, #{housingFund}, #{tax}, " +
            "#{otherDeduction}, #{netPay}, #{month}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SalaryRecord salaryRecord);

    @Update("UPDATE t_salary_record SET employee_name=#{employeeName}, employee_no=#{employeeNo}, department=#{department}, " +
            "position=#{position}, base_salary=#{baseSalary}, bonus=#{bonus}, allowance=#{allowance}, overtime=#{overtime}, " +
            "gross_pay=#{grossPay}, social_insurance=#{socialInsurance}, housing_fund=#{housingFund}, tax=#{tax}, " +
            "other_deduction=#{otherDeduction}, net_pay=#{netPay}, month=#{month}, status=#{status} WHERE id=#{id}")
    int update(SalaryRecord salaryRecord);

    @Delete("DELETE FROM t_salary_record WHERE id = #{id}")
    int deleteById(Long id);
}
