package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Budget;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BudgetMapper {

    @Select("SELECT * FROM t_budget WHERE user_id = #{userId} AND year = #{year} AND month = #{month} ORDER BY created_at DESC")
    List<Budget> findByUserIdAndYearAndMonthOrderByCreatedAtDesc(Long userId, Integer year, Integer month);

    @Select("SELECT * FROM t_budget WHERE user_id = #{userId} AND year = #{year} ORDER BY created_at DESC")
    List<Budget> findByUserIdAndYearOrderByCreatedAtDesc(Long userId, Integer year);

    @Select("SELECT * FROM t_budget WHERE user_id = #{userId} AND department = #{department} ORDER BY created_at DESC")
    List<Budget> findByUserIdAndDepartmentOrderByCreatedAtDesc(Long userId, String department);

    @Select("SELECT * FROM t_budget WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Budget> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_budget WHERE id = #{id}")
    Budget findById(Long id);

    @Select("SELECT * FROM t_budget WHERE year = #{year} AND month = #{month} AND department = #{department} AND user_id = #{userId}")
    List<Budget> findByYearMonthDepartment(Integer year, Integer month, String department, Long userId);

    @Select("SELECT DISTINCT department FROM t_budget WHERE user_id = #{userId} ORDER BY department")
    List<String> findDistinctDepartments(Long userId);

    @Select("SELECT * FROM t_budget WHERE year = #{year} AND month = #{month} AND used_amount > budget_amount AND user_id = #{userId}")
    List<Budget> findOverBudget(Integer year, Integer month, Long userId);

    @Insert("INSERT INTO t_budget (user_id, department, year, month, category, budget_amount, used_amount, alert_threshold, description, created_at, updated_at) " +
            "VALUES (#{userId}, #{department}, #{year}, #{month}, #{category}, #{budgetAmount}, #{usedAmount}, #{alertThreshold}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Budget budget);

    @Update("UPDATE t_budget SET department=#{department}, year=#{year}, month=#{month}, category=#{category}, " +
            "budget_amount=#{budgetAmount}, used_amount=#{usedAmount}, alert_threshold=#{alertThreshold}, " +
            "description=#{description}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(Budget budget);

    @Delete("DELETE FROM t_budget WHERE id = #{id}")
    int deleteById(Long id);
}
