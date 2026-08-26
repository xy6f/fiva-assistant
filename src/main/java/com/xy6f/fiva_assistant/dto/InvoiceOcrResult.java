package com.xy6f.fiva_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceOcrResult {
    private String invoiceNo;
    private String invoiceDate;
    private String sellerName;
    private String buyerName;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String category;
    private String rawText;
    private String imageUrl;
    // === OCR 工程化新增 ===
    private Map<String, Double> fieldConfidence;  // 字段级置信度（模型自报）
    private Double overallConfidence;             // 整体置信度（自报 + 勾稽验算加权）
    private Boolean amountConsistent;             // 金额勾稽：价税合计 = 金额 + 税额
    private Boolean parseSuccess;                 // JSON 解析是否成功
}
