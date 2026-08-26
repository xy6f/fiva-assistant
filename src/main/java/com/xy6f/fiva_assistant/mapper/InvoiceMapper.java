package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.Invoice;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface InvoiceMapper {

    @Select("SELECT * FROM t_invoice WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<Invoice> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    @Select("SELECT * FROM t_invoice WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_invoice WHERE id = #{id}")
    Invoice findById(Long id);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_invoice WHERE created_at >= #{startDate} AND user_id = #{userId}")
    BigDecimal sumTotalAmountSince(LocalDateTime startDate, Long userId);

    @Select("SELECT COALESCE(SUM(tax_amount), 0) FROM t_invoice WHERE created_at >= #{startDate} AND user_id = #{userId}")
    BigDecimal sumTaxAmountSince(LocalDateTime startDate, Long userId);

    @Select("SELECT COUNT(*) FROM t_invoice WHERE created_at >= #{startDate} AND user_id = #{userId}")
    Long countSince(LocalDateTime startDate, Long userId);

    @Select("SELECT category, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amt FROM t_invoice WHERE user_id = #{userId} GROUP BY category")
    List<Map<String, Object>> categoryStats(Long userId);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_invoice WHERE created_at BETWEEN #{start} AND #{end} AND user_id = #{userId}")
    BigDecimal sumTotalAmountBetween(LocalDateTime start, LocalDateTime end, Long userId);

    @Select("SELECT COUNT(*) FROM t_invoice WHERE user_id = #{userId}")
    Long countByUserId(Long userId);

    @Insert("INSERT INTO t_invoice (user_id, invoice_no, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, " +
            "category, image_path, ocr_raw_text, status, invoice_code, check_code, verified, verified_at, verify_message, created_at) " +
            "VALUES (#{userId}, #{invoiceNo}, #{invoiceDate}, #{sellerName}, #{buyerName}, #{amount}, #{taxAmount}, #{totalAmount}, " +
            "#{category}, #{imagePath}, #{ocrRawText}, #{status}, #{invoiceCode}, #{checkCode}, #{verified}, #{verifiedAt}, #{verifyMessage}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Invoice invoice);

    @Update("UPDATE t_invoice SET invoice_no=#{invoiceNo}, invoice_date=#{invoiceDate}, seller_name=#{sellerName}, " +
            "buyer_name=#{buyerName}, amount=#{amount}, tax_amount=#{taxAmount}, total_amount=#{totalAmount}, " +
            "category=#{category}, image_path=#{imagePath}, ocr_raw_text=#{ocrRawText}, status=#{status}, " +
            "invoice_code=#{invoiceCode}, check_code=#{checkCode}, verified=#{verified}, verified_at=#{verifiedAt}, " +
            "verify_message=#{verifyMessage} WHERE id=#{id}")
    int update(Invoice invoice);

    @Delete("DELETE FROM t_invoice WHERE id = #{id}")
    int deleteById(Long id);
}
