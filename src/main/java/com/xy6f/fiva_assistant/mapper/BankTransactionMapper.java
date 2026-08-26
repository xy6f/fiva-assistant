package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.BankTransaction;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BankTransactionMapper {

    @Select("SELECT * FROM t_bank_transaction WHERE user_id = #{userId} ORDER BY transaction_date DESC, created_at DESC")
    List<BankTransaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_bank_transaction WHERE user_id = #{userId} AND status = #{status} ORDER BY transaction_date DESC")
    List<BankTransaction> findByUserIdAndStatusOrderByTransactionDateDesc(Long userId, String status);

    @Select("SELECT * FROM t_bank_transaction WHERE id = #{id}")
    BankTransaction findById(Long id);

    @Insert("INSERT INTO t_bank_transaction (user_id, bank_name, account_no, transaction_date, counterparty, summary, " +
            "debit_amount, credit_amount, balance, status, voucher_id, created_at) " +
            "VALUES (#{userId}, #{bankName}, #{accountNo}, #{transactionDate}, #{counterparty}, #{summary}, " +
            "#{debitAmount}, #{creditAmount}, #{balance}, #{status}, #{voucherId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BankTransaction bankTransaction);

    @Update("UPDATE t_bank_transaction SET bank_name=#{bankName}, account_no=#{accountNo}, transaction_date=#{transactionDate}, " +
            "counterparty=#{counterparty}, summary=#{summary}, debit_amount=#{debitAmount}, credit_amount=#{creditAmount}, " +
            "balance=#{balance}, status=#{status}, voucher_id=#{voucherId} WHERE id=#{id}")
    int update(BankTransaction bankTransaction);

    @Delete("DELETE FROM t_bank_transaction WHERE id = #{id}")
    int deleteById(Long id);
}
