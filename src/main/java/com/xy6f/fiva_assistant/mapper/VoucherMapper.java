package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Voucher;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface VoucherMapper {

    @Select("SELECT * FROM t_voucher WHERE user_id = #{userId} ORDER BY voucher_date DESC, created_at DESC")
    List<Voucher> findByUserIdOrderByVoucherDateDescCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_voucher WHERE user_id = #{userId} AND type = #{type} ORDER BY voucher_date DESC")
    List<Voucher> findByUserIdAndTypeOrderByVoucherDateDesc(Long userId, String type);

    @Select("SELECT * FROM t_voucher WHERE user_id = #{userId} AND voucher_date BETWEEN #{start} AND #{end} ORDER BY voucher_date DESC")
    List<Voucher> findByUserIdAndVoucherDateBetweenOrderByVoucherDateDesc(Long userId, LocalDate start, LocalDate end);

    @Select("SELECT * FROM t_voucher WHERE user_id = #{userId} AND status = #{status} ORDER BY voucher_date DESC, created_at DESC")
    List<Voucher> findByUserIdAndStatusOrderByVoucherDateDescCreatedAtDesc(Long userId, String status);

    @Select("SELECT * FROM t_voucher WHERE user_id = #{userId} AND voucher_date BETWEEN #{start} AND #{end} AND status = #{status} ORDER BY voucher_date DESC")
    List<Voucher> findByUserIdAndVoucherDateBetweenAndStatusOrderByVoucherDateDesc(
            Long userId, LocalDate start, LocalDate end, String status);

    @Select("<script>" +
            "SELECT * FROM t_voucher WHERE user_id = #{userId}" +
            "<if test='type != null'> AND type = #{type}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='start != null'> AND voucher_date &gt;= #{start}</if>" +
            "<if test='end != null'> AND voucher_date &lt;= #{end}</if>" +
            " ORDER BY voucher_date DESC, created_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Voucher> findPage(@Param("userId") Long userId, @Param("type") String type,
                           @Param("status") String status, @Param("start") LocalDate start,
                           @Param("end") LocalDate end, @Param("limit") int limit, @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_voucher WHERE user_id = #{userId}" +
            "<if test='type != null'> AND type = #{type}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='start != null'> AND voucher_date &gt;= #{start}</if>" +
            "<if test='end != null'> AND voucher_date &lt;= #{end}</if>" +
            "</script>")
    long countPage(@Param("userId") Long userId, @Param("type") String type,
                   @Param("status") String status, @Param("start") LocalDate start,
                   @Param("end") LocalDate end);

    @Select("SELECT * FROM t_voucher WHERE voucher_no = #{voucherNo} AND user_id = #{userId}")
    Voucher findByVoucherNoAndUserId(String voucherNo, Long userId);

    @Select("SELECT * FROM t_voucher WHERE id = #{id}")
    Voucher findById(Long id);

    @Select("<script>" +
            "SELECT * FROM t_voucher WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Voucher> findAllById(List<Long> ids);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_voucher WHERE type = #{type} AND voucher_date >= #{startDate} AND user_id = #{userId}")
    BigDecimal sumByTypeSince(String type, LocalDate startDate, Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_voucher WHERE type = #{type} AND voucher_date BETWEEN #{start} AND #{end} AND user_id = #{userId}")
    BigDecimal sumByTypeBetween(String type, LocalDate start, LocalDate end, Long userId);

    @Select("SELECT category, COALESCE(SUM(amount), 0) AS amt FROM t_voucher WHERE type = #{type} AND voucher_date >= #{startDate} AND user_id = #{userId} GROUP BY category")
    List<Map<String, Object>> categorySumByTypeSince(String type, LocalDate startDate, Long userId);

    @Select("SELECT m, income, expense FROM (" +
            "SELECT YEAR(voucher_date) * 100 + MONTH(voucher_date) AS m, " +
            "COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END), 0) AS expense " +
            "FROM t_voucher WHERE voucher_date BETWEEN #{start} AND #{end} AND user_id = #{userId} " +
            "GROUP BY YEAR(voucher_date) * 100 + MONTH(voucher_date)) t ORDER BY m")
    List<Map<String, Object>> monthlyIncomeExpense(LocalDate start, LocalDate end, Long userId);

    @Insert("INSERT INTO t_voucher (user_id, voucher_no, type, amount, category, description, voucher_date, counterparty, has_invoice, invoice_image_url, status, created_at) " +
            "VALUES (#{userId}, #{voucherNo}, #{type}, #{amount}, #{category}, #{description}, #{voucherDate}, #{counterparty}, #{hasInvoice}, #{invoiceImageUrl}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Voucher voucher);

    @Update("UPDATE t_voucher SET voucher_no=#{voucherNo}, type=#{type}, amount=#{amount}, category=#{category}, " +
            "description=#{description}, voucher_date=#{voucherDate}, counterparty=#{counterparty}, " +
            "has_invoice=#{hasInvoice}, invoice_image_url=#{invoiceImageUrl}, status=#{status} WHERE id=#{id}")
    int update(Voucher voucher);

    @Delete("DELETE FROM t_voucher WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM t_voucher WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
