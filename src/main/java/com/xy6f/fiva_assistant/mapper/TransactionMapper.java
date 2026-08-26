package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Transaction;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionMapper {

    @Select("SELECT * FROM t_transaction WHERE user_id = #{userId} ORDER BY transaction_date DESC, created_at DESC")
    List<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_transaction WHERE user_id = #{userId} AND type = #{type} ORDER BY transaction_date DESC")
    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDesc(Long userId, String type);

    @Select("SELECT * FROM t_transaction WHERE user_id = #{userId} AND transaction_date BETWEEN #{start} AND #{end} ORDER BY transaction_date DESC")
    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(Long userId, LocalDate start, LocalDate end);

    @Select("SELECT * FROM t_transaction WHERE id = #{id}")
    Transaction findById(Long id);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_transaction WHERE type = #{type} AND transaction_date >= #{startDate} AND user_id = #{userId}")
    BigDecimal sumByTypeSince(String type, LocalDate startDate, Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_transaction WHERE type = #{type} AND transaction_date BETWEEN #{start} AND #{end} AND user_id = #{userId}")
    BigDecimal sumByTypeBetween(String type, LocalDate start, LocalDate end, Long userId);

    @Select("SELECT category, COALESCE(SUM(amount), 0) AS amt FROM t_transaction WHERE type = #{type} AND transaction_date >= #{startDate} AND user_id = #{userId} GROUP BY category")
    List<Map<String, Object>> categorySumByTypeSince(String type, LocalDate startDate, Long userId);

    @Select("SELECT m, income, expense FROM (" +
            "SELECT YEAR(transaction_date) * 100 + MONTH(transaction_date) AS m, " +
            "COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END), 0) AS expense " +
            "FROM t_transaction WHERE transaction_date BETWEEN #{start} AND #{end} AND user_id = #{userId} " +
            "GROUP BY YEAR(transaction_date) * 100 + MONTH(transaction_date)) t ORDER BY m")
    List<Map<String, Object>> monthlyIncomeExpense(LocalDate start, LocalDate end, Long userId);

    @Insert("INSERT INTO t_transaction (user_id, type, amount, category, description, transaction_date, counterparty, invoice_id, created_at) " +
            "VALUES (#{userId}, #{type}, #{amount}, #{category}, #{description}, #{transactionDate}, #{counterparty}, #{invoiceId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Transaction transaction);

    @Update("UPDATE t_transaction SET type=#{type}, amount=#{amount}, category=#{category}, description=#{description}, " +
            "transaction_date=#{transactionDate}, counterparty=#{counterparty}, invoice_id=#{invoiceId} WHERE id=#{id}")
    int update(Transaction transaction);

    @Delete("DELETE FROM t_transaction WHERE id = #{id}")
    int deleteById(Long id);
}
