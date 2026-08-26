package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Expense;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExpenseMapper {

    @Select("SELECT * FROM t_expense WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<Expense> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    @Select("SELECT * FROM t_expense WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Expense> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_expense WHERE id = #{id}")
    Expense findById(Long id);

    @Select("SELECT * FROM t_expense WHERE user_id = #{userId} AND (applicant LIKE CONCAT('%', #{keyword}, '%') OR title LIKE CONCAT('%', #{keyword}, '%')) ORDER BY created_at DESC")
    List<Expense> searchByKeyword(Long userId, String keyword);

    @Select("SELECT category, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt FROM t_expense WHERE status = 'APPROVED' AND user_id = #{userId} GROUP BY category")
    List<Map<String, Object>> categoryStats(Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_expense WHERE status = 'APPROVED' AND created_at >= #{startDate} AND user_id = #{userId}")
    BigDecimal sumApprovedAmountSince(LocalDateTime startDate, Long userId);

    @Select("SELECT COUNT(*) FROM t_expense WHERE status = #{status} AND user_id = #{userId}")
    Long countByStatusAndUserId(String status, Long userId);

    @Insert("INSERT INTO t_expense (user_id, title, applicant, category, amount, expense_date, description, invoice_path, " +
            "status, approver, approval_opinion, approved_at, created_at, updated_at) " +
            "VALUES (#{userId}, #{title}, #{applicant}, #{category}, #{amount}, #{expenseDate}, #{description}, #{invoicePath}, " +
            "#{status}, #{approver}, #{approvalOpinion}, #{approvedAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Expense expense);

    @Update("UPDATE t_expense SET title=#{title}, applicant=#{applicant}, category=#{category}, amount=#{amount}, " +
            "expense_date=#{expenseDate}, description=#{description}, invoice_path=#{invoicePath}, status=#{status}, " +
            "approver=#{approver}, approval_opinion=#{approvalOpinion}, approved_at=#{approvedAt}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(Expense expense);

    @Delete("DELETE FROM t_expense WHERE id = #{id}")
    int deleteById(Long id);
}
