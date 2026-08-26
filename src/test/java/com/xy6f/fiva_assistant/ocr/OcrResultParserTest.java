package com.xy6f.fiva_assistant.ocr;

import com.xy6f.fiva_assistant.dto.InvoiceOcrResult;
import com.xy6f.fiva_assistant.service.ocr.OcrResultParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OCR 解析器单元测试（纯函数，无需 Spring 上下文/数据库）
 */
class OcrResultParserTest {

    private final OcrResultParser parser = new OcrResultParser();

    @Test
    void parsePlainJson() {
        String json = """
                {"invoiceNo":"25361100","invoiceDate":"2025-06-30","sellerName":"甲公司",
                 "buyerName":"乙公司","amount":1000.00,"taxAmount":130.00,"totalAmount":1130.00,
                 "category":"增值税专用发票",
                 "confidences":{"invoiceNo":0.98,"amount":0.96}}
                """;
        InvoiceOcrResult r = parser.parse(json);
        assertTrue(r.getParseSuccess());
        assertEquals("25361100", r.getInvoiceNo());
        assertEquals("2025-06-30", r.getInvoiceDate());
        assertEquals(new BigDecimal("1130.00").doubleValue(), r.getTotalAmount().doubleValue(), 0.0001);
        assertTrue(r.getAmountConsistent());
        assertNotNull(r.getFieldConfidence());
        assertEquals(0.98, r.getFieldConfidence().get("invoiceNo"));
        assertEquals(0.97, r.getOverallConfidence());
    }

    @Test
    void parseMarkdownFencedJson() {
        String text = """
                ```json
                {"invoiceNo":"123","amount":100,"taxAmount":13,"totalAmount":113}
                ```
                """;
        InvoiceOcrResult r = parser.parse(text);
        assertTrue(r.getParseSuccess());
        assertEquals("123", r.getInvoiceNo());
        assertTrue(r.getAmountConsistent());
    }

    @Test
    void parseJsonWithLeadingText() {
        String text = "识别结果如下：{\"invoiceNo\":\"A1\",\"totalAmount\":50} 以上。";
        InvoiceOcrResult r = parser.parse(text);
        assertTrue(r.getParseSuccess());
        assertEquals("A1", r.getInvoiceNo());
    }

    @Test
    void parseGarbageFails() {
        InvoiceOcrResult r = parser.parse("这不是JSON");
        assertFalse(r.getParseSuccess());
        assertNull(r.getInvoiceNo());
    }

    @Test
    void amountInconsistentReducesConfidence() {
        String json = """
                {"invoiceNo":"X","amount":1000.00,"taxAmount":100.00,"totalAmount":9999.00,
                 "confidences":{"amount":0.9}}
                """;
        InvoiceOcrResult r = parser.parse(json);
        assertTrue(r.getParseSuccess());
        assertFalse(r.getAmountConsistent());
        // 0.9 * 0.7 = 0.63
        assertEquals(0.63, r.getOverallConfidence());
    }

    @Test
    void missingAmountsCapConfidence() {
        String json = "{\"invoiceNo\":\"Y\",\"confidences\":{\"invoiceNo\":1.0}}";
        InvoiceOcrResult r = parser.parse(json);
        assertTrue(r.getParseSuccess());
        assertTrue(r.getOverallConfidence() <= 0.6);
    }

    @Test
    void nullFieldsHandled() {
        String json = """
                {"invoiceNo":null,"invoiceDate":"2025年07月01日","amount":null,
                 "taxAmount":"1,234.56"}
                """;
        InvoiceOcrResult r = parser.parse(json);
        assertTrue(r.getParseSuccess());
        assertNull(r.getInvoiceNo());
        assertEquals("2025-07-01", r.getInvoiceDate());
        assertNull(r.getAmount());
        assertEquals(new BigDecimal("1234.56"), r.getTaxAmount());
        assertFalse(r.getAmountConsistent());
    }

    @Test
    void normalizeDateVariants() {
        assertEquals("2025-01-02", parser.normalizeDate("2025/1/2"));
        assertEquals("2025-01-02", parser.normalizeDate("2025.01.02"));
        assertEquals("2025-01-02", parser.normalizeDate("2025年01月02日"));
        assertEquals("not-a-date", parser.normalizeDate("not-a-date"));
        assertNull(parser.normalizeDate(null));
    }
}
