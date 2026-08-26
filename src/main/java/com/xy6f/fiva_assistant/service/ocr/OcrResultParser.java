package com.xy6f.fiva_assistant.service.ocr;

import com.xy6f.fiva_assistant.dto.InvoiceOcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OCR 结果解析器：AI 文本 → 结构化结果（纯函数，便于单测与评测集复用）
 */
@Component
@Slf4j
public class OcrResultParser {

    public static final String OCR_PROMPT = """
            你是一个专业的发票识别助手。请仔细分析这张发票图片，提取以下信息并以JSON格式返回。

            要求：
            1. 识别发票号码(invoiceNo)
            2. 识别开票日期(invoiceDate)，格式yyyy-MM-dd
            3. 识别销售方名称(sellerName)
            4. 识别购买方名称(buyerName)
            5. 识别金额(amount，不含税)
            6. 识别税额(taxAmount)
            7. 识别价税合计(totalAmount)
            8. 判断发票类别(category)，如：增值税专用发票、增值税普通发票、电子发票等

            同时请为每个字段给出识别置信度(0~1的小数)，放在confidences对象中。
            图片模糊或字段缺失时给出较低的置信度。

            请只返回JSON，不要包含任何其他内容、解释或markdown代码块标记。格式如下：
            {
              "invoiceNo": "xxx",
              "invoiceDate": "2024-01-01",
              "sellerName": "xxx公司",
              "buyerName": "xxx公司",
              "amount": 1000.00,
              "taxAmount": 130.00,
              "totalAmount": 1130.00,
              "category": "增值税专用发票",
              "confidences": {
                "invoiceNo": 0.98,
                "invoiceDate": 0.95,
                "sellerName": 0.90,
                "buyerName": 0.92,
                "amount": 0.96,
                "taxAmount": 0.94,
                "totalAmount": 0.97,
                "category": 0.85
              }
            }

            如果某项信息无法识别，请填写null并在confidences中给0。
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 解析 AI 输出文本；解析失败时 parseSuccess=false
     */
    public InvoiceOcrResult parse(String text) {
        InvoiceOcrResult result = new InvoiceOcrResult();
        try {
            JsonNode root = mapper.readTree(extractCandidateJson(text));
            result.setInvoiceNo(textOrNull(root, "invoiceNo"));
            result.setInvoiceDate(normalizeDate(textOrNull(root, "invoiceDate")));
            result.setSellerName(textOrNull(root, "sellerName"));
            result.setBuyerName(textOrNull(root, "buyerName"));
            result.setAmount(decimalOrNull(root, "amount"));
            result.setTaxAmount(decimalOrNull(root, "taxAmount"));
            result.setTotalAmount(decimalOrNull(root, "totalAmount"));
            result.setCategory(textOrNull(root, "category"));

            // 字段级置信度（模型自报）
            Map<String, Double> conf = new LinkedHashMap<>();
            JsonNode confNode = root.get("confidences");
            if (confNode != null && confNode.isObject()) {
                confNode.propertyNames().forEach(name -> conf.put(name, confNode.get(name).asDouble(0.0)));
            }
            result.setFieldConfidence(conf.isEmpty() ? null : conf);

            // 金额勾稽验算：价税合计 ≈ 金额 + 税额
            result.setAmountConsistent(checkAmountConsistency(result));

            // 整体置信度 = 字段置信度均值 × 勾稽系数
            result.setOverallConfidence(computeOverallConfidence(result));
            result.setParseSuccess(true);
        } catch (Exception e) {
            log.warn("OCR JSON parse failed: {}", e.getMessage());
            result.setParseSuccess(false);
        }
        return result;
    }

    /**
     * 从 AI 文本中定位候选 JSON：优先整体解析，其次剥离 markdown 代码块，最后截取首尾大括号
     */
    public String extractCandidateJson(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("{")) return t;
        int fenceStart = t.indexOf("```");
        while (fenceStart >= 0) {
            int objStart = t.indexOf('{', fenceStart);
            int fenceEnd = t.indexOf("```", fenceStart + 3);
            int objEnd = fenceEnd > 0 ? t.lastIndexOf('}', fenceEnd) : t.lastIndexOf('}');
            if (objStart >= 0 && objEnd > objStart) {
                return t.substring(objStart, objEnd + 1);
            }
            fenceStart = fenceEnd >= 0 ? t.indexOf("```", fenceEnd + 3) : -1;
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        return (start >= 0 && end > start) ? t.substring(start, end + 1) : t;
    }

    boolean checkAmountConsistency(InvoiceOcrResult r) {
        if (r.getAmount() == null || r.getTaxAmount() == null || r.getTotalAmount() == null) {
            return false;
        }
        return r.getAmount().add(r.getTaxAmount()).compareTo(r.getTotalAmount()) == 0;
    }

    double computeOverallConfidence(InvoiceOcrResult r) {
        double avg = 0.5;
        if (r.getFieldConfidence() != null && !r.getFieldConfidence().isEmpty()) {
            avg = r.getFieldConfidence().values().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.5);
        }
        double score = Boolean.TRUE.equals(r.getAmountConsistent()) ? avg : avg * 0.7;
        if (r.getAmount() == null && r.getTotalAmount() == null) {
            score = Math.min(score, 0.6);
        }
        return Math.round(score * 100) / 100.0;
    }

    private String textOrNull(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) return null;
        String v = n.asText();
        return v.isBlank() || "null".equalsIgnoreCase(v) ? null : v;
    }

    private BigDecimal decimalOrNull(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) return null;
        try {
            return new BigDecimal(n.asText().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 兼容 yyyy-MM-dd 与 yyyy年MM月dd日 / yyyy/MM/dd / yyyy.MM.dd / 非零填充 */
    public String normalizeDate(String date) {
        if (date == null) return null;
        String d = date.trim()
                .replace("年", "-").replace("月", "-").replace("日", "")
                .replace("/", "-").replace(".", "-");
        String[] parts = d.split("-");
        if (parts.length == 3) {
            try {
                return LocalDate.of(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
            }
        }
        return date;
    }
}
