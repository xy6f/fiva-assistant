package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.VoucherEntry;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface VoucherEntryMapper {

    @Select("SELECT e.account_id AS accountId, COALESCE(SUM(e.debit_amount),0) AS debit, COALESCE(SUM(e.credit_amount),0) AS credit " +
            "FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE v.user_id = #{userId} AND v.voucher_date <= #{end} GROUP BY e.account_id")
    List<Map<String, Object>> sumGroupByAccount(Long userId, LocalDate end);

    @Select("SELECT e.account_id AS accountId, COALESCE(SUM(e.debit_amount),0) AS debit, COALESCE(SUM(e.credit_amount),0) AS credit, " +
            "COALESCE(SUM(e.debit_amount),0) - COALESCE(SUM(e.credit_amount),0) AS netDebit " +
            "FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE v.user_id = #{userId} AND v.voucher_date BETWEEN #{start} AND #{end} GROUP BY e.account_id")
    List<Map<String, Object>> sumGroupByAccountBetween(Long userId, LocalDate start, LocalDate end);

    @Select("SELECT * FROM t_voucher_entry WHERE voucher_id = #{voucherId} ORDER BY sort_order ASC")
    List<VoucherEntry> findByVoucherIdOrderBySortOrderAsc(Long voucherId);

    @Select("<script>" +
            "SELECT * FROM t_voucher_entry WHERE voucher_id IN " +
            "<foreach item='id' collection='voucherIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<VoucherEntry> findByVoucherIdIn(List<Long> voucherIds);

    @Delete("DELETE FROM t_voucher_entry WHERE voucher_id = #{voucherId}")
    int deleteByVoucherId(Long voucherId);

    @Select("SELECT e.* FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE e.account_id = #{accountId} AND v.user_id = #{userId} " +
            "AND v.voucher_date BETWEEN #{start} AND #{end} " +
            "ORDER BY v.voucher_date ASC, v.voucher_no ASC, e.sort_order ASC")
    List<VoucherEntry> findDetailLedger(Long accountId, Long userId, LocalDate start, LocalDate end);

    @Select("SELECT e.* FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE v.user_id = #{userId} AND v.voucher_date BETWEEN #{start} AND #{end} " +
            "ORDER BY e.account_id, v.voucher_date ASC")
    List<VoucherEntry> findAllInPeriod(Long userId, LocalDate start, LocalDate end);

    @Select("SELECT e.* FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE v.user_id = #{userId} AND v.voucher_date < #{start} " +
            "ORDER BY e.account_id")
    List<VoucherEntry> findAllBeforePeriod(Long userId, LocalDate start);

    @Select("SELECT e.* FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id " +
            "WHERE e.account_id = #{accountId} AND v.user_id = #{userId} AND v.voucher_date < #{start}")
    List<VoucherEntry> findBeforePeriodByAccount(Long accountId, Long userId, LocalDate start);

    @Insert("INSERT INTO t_voucher_entry (voucher_id, account_id, summary, debit_amount, credit_amount, sort_order) " +
            "VALUES (#{voucherId}, #{accountId}, #{summary}, #{debitAmount}, #{creditAmount}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VoucherEntry entry);

    @Update("UPDATE t_voucher_entry SET voucher_id=#{voucherId}, account_id=#{accountId}, summary=#{summary}, " +
            "debit_amount=#{debitAmount}, credit_amount=#{creditAmount}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(VoucherEntry entry);

    @Delete("DELETE FROM t_voucher_entry WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) > 0 FROM t_voucher_entry WHERE account_id = #{accountId}")
    boolean existsByAccountId(Long accountId);
}
