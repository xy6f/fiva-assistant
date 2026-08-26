package com.xy6f.fiva_assistant.ocr;

import com.xy6f.fiva_assistant.dto.InvoiceOcrResult;
import com.xy6f.fiva_assistant.service.ocr.OcrResultParser;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR 识别质量评测集（等待真实样本，样本就绪后移除 @Disabled 运行）。
 *
 * 使用方式：
 * 1. 将真实发票图片放入 src/test/resources/ocr-samples/
 * 2. 在 samples.csv 中登记每张图的期望值
 * 3. 执行 .\mvnw.cmd test -Dtest=OcrEvaluationTest
 *
 * 输出：逐字段准确率 + 平均整体置信度 + 勾稽一致率报告
 */
@Disabled("等待用户提供真实发票样本")
class OcrEvaluationTest {

    private static final String[] FIELDS = {
            "invoiceNo", "invoiceDate", "sellerName", "buyerName",
            "amount", "taxAmount", "totalAmount", "category"
    };

    private static OpenAiChatModel visionModel;
    private static final OcrResultParser parser = new OcrResultParser();

    @BeforeAll
    static void setUp() {
        String baseUrl = envOrDefault("AI_VISION_BASE_URL", envOrDefault("AI_DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
        String apiKey = System.getenv().getOrDefault("AI_VISION_API_KEY",
                System.getenv().getOrDefault("AI_DEEPSEEK_API_KEY", "test-key"));
        String model = envOrDefault("AI_VISION_MODEL", envOrDefault("AI_DEEPSEEK_MODEL", "gpt-4o-mini"));
        visionModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .build();
    }

    @Test
    void evaluateRealSamples() throws Exception {
        Path samplesCsv = new ClassPathResource("ocr-samples/samples.csv").getFile().toPath();
        List<String[]> rows = Files.readAllLines(samplesCsv, java.nio.charset.StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                .map(l -> l.split(","))
                .toList();

        if (rows.isEmpty()) {
            System.out.println("[OCR-EVAL] samples.csv 中没有样本行，跳过评测");
            return;
        }

        Map<String, Integer> fieldHits = new LinkedHashMap<>();
        Map<String, Integer> fieldTotals = new LinkedHashMap<>();
        int parseOk = 0, consistent = 0;
        double confidenceSum = 0;
        List<String> failures = new ArrayList<>();

        for (String[] row : rows) {
            String imageFile = row[0];
            InvoiceOcrResult expected = expectedOf(row);
            InvoiceOcrResult actual = recognize(imageFile);

            if (!Boolean.TRUE.equals(actual.getParseSuccess())) {
                failures.add(imageFile + ": 解析失败");
                continue;
            }
            parseOk++;
            confidenceSum += nvl(actual.getOverallConfidence());
            if (Boolean.TRUE.equals(actual.getAmountConsistent())) consistent++;

            compare(fieldHits, fieldTotals, "invoiceNo", expected.getInvoiceNo(), actual.getInvoiceNo());
            compare(fieldHits, fieldTotals, "invoiceDate", expected.getInvoiceDate(), actual.getInvoiceDate());
            compare(fieldHits, fieldTotals, "sellerName", expected.getSellerName(), actual.getSellerName());
            compare(fieldHits, fieldTotals, "buyerName", expected.getBuyerName(), actual.getBuyerName());
            compare(fieldHits, fieldTotals, "amount", expected.getAmount(), actual.getAmount());
            compare(fieldHits, fieldTotals, "taxAmount", expected.getTaxAmount(), actual.getTaxAmount());
            compare(fieldHits, fieldTotals, "totalAmount", expected.getTotalAmount(), actual.getTotalAmount());
            compare(fieldHits, fieldTotals, "category", expected.getCategory(), actual.getCategory());

            if (expected.getInvoiceNo() != null && !expected.getInvoiceNo().equals(actual.getInvoiceNo())) {
                failures.add(imageFile + ": 发票号 期望=" + expected.getInvoiceNo() + " 实际=" + actual.getInvoiceNo());
            }
        }

        System.out.println("========== OCR 评测报告 ==========");
        System.out.printf("样本数: %d, 解析成功: %d (%.0f%%)%n",
                rows.size(), parseOk, 100.0 * parseOk / rows.size());
        for (String f : FIELDS) {
            int total = fieldTotals.getOrDefault(f, 0);
            int hit = fieldHits.getOrDefault(f, 0);
            if (total > 0) {
                System.out.printf("%-14s 准确率: %.0f%% (%d/%d)%n", f, 100.0 * hit / total, hit, total);
            }
        }
        System.out.printf("金额勾稽一致率: %.0f%% (%d/%d)%n",
                parseOk > 0 ? 100.0 * consistent / parseOk : 0, consistent, parseOk);
        System.out.printf("平均整体置信度: %.2f%n", parseOk > 0 ? confidenceSum / parseOk : 0);
        failures.forEach(f -> System.out.println("❌ " + f));
    }

    private InvoiceOcrResult recognize(String imageFile) throws Exception {
        byte[] bytes = new ClassPathResource("ocr-samples/" + imageFile).getInputStream().readAllBytes();
        String mime = imageFile.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        UserMessage msg = UserMessage.from(
                TextContent.from(OcrResultParser.OCR_PROMPT),
                ImageContent.from(Base64.getEncoder().encodeToString(bytes), mime));
        String aiResponse = visionModel.chat(msg).aiMessage().text();
        return parser.parse(aiResponse);
    }

    private InvoiceOcrResult expectedOf(String[] row) {
        return InvoiceOcrResult.builder()
                .invoiceNo(strOrNull(row, 1))
                .invoiceDate(strOrNull(row, 2))
                .amount(decimalOrNull(row, 3))
                .taxAmount(decimalOrNull(row, 4))
                .totalAmount(decimalOrNull(row, 5))
                .build();
    }

    private void compare(Map<String, Integer> hits, Map<String, Integer> totals,
                         String field, Object expected, Object actual) {
        if (expected == null || expected.toString().isBlank()) return;
        totals.merge(field, 1, Integer::sum);
        if (expected.equals(actual)) hits.merge(field, 1, Integer::sum);
    }

    private static String strOrNull(String[] row, int idx) {
        return row.length > idx && !row[idx].isBlank() ? row[idx].trim() : null;
    }

    private static BigDecimal decimalOrNull(String[] row, int idx) {
        String v = strOrNull(row, idx);
        try {
            return v != null ? new BigDecimal(v) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double nvl(Double d) {
        return d != null ? d : 0.0;
    }

    private static String envOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
