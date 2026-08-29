package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.InvoiceOcrResult;
import com.xy6f.fiva_assistant.model.Invoice;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.mapper.InvoiceMapper;
import com.xy6f.fiva_assistant.service.OssService;
import com.xy6f.fiva_assistant.service.VoucherService;
import com.xy6f.fiva_assistant.util.FileUtil;
import com.xy6f.fiva_assistant.util.UserContext;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final OpenAiChatModel visionModel;
    private final OssService ossService;
    private final VoucherService voucherService;
    private final com.xy6f.fiva_assistant.service.tax.TaxRuleEngine taxRuleEngine;
    private final com.xy6f.fiva_assistant.service.ocr.OcrResultParser ocrParser;
    private final com.xy6f.fiva_assistant.service.AiCallLogService aiCallLogService;

    public InvoiceService(InvoiceMapper invoiceMapper,
                          @Qualifier("visionModel") OpenAiChatModel visionModel,
                          OssService ossService,
                          VoucherService voucherService,
                          com.xy6f.fiva_assistant.service.tax.TaxRuleEngine taxRuleEngine,
                          com.xy6f.fiva_assistant.service.ocr.OcrResultParser ocrParser,
                          com.xy6f.fiva_assistant.service.AiCallLogService aiCallLogService) {
        this.invoiceMapper = invoiceMapper;
        this.visionModel = visionModel;
        this.ossService = ossService;
        this.voucherService = voucherService;
        this.taxRuleEngine = taxRuleEngine;
        this.ocrParser = ocrParser;
        this.aiCallLogService = aiCallLogService;
    }

    private static final String OCR_PROMPT = com.xy6f.fiva_assistant.service.ocr.OcrResultParser.OCR_PROMPT;

    private static final String OCR_REPAIR_PROMPT = """
            上面的内容不是合法JSON。请将其中包含的发票识别结果重新整理，
            只输出一个合法的JSON对象，不要任何解释、前后缀或markdown代码块标记。
            """;

    public InvoiceOcrResult recognizeInvoice(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename();

        // Upload to OSS
        String imageUrl = ossService.uploadFile(file, "invoice");

        // Convert to base64 for AI vision
        String base64Image = FileUtil.encodeToBase64(bytes);
        String mimeType = FileUtil.getMimeType(filename);

        // Call vision AI for OCR
        UserMessage userMessage = UserMessage.from(
                TextContent.from(OCR_PROMPT),
                ImageContent.from(base64Image, mimeType)
        );

        String aiResponse;
        long ocrStartMs = System.currentTimeMillis();
        Integer[] usage = {null, null};
        try {
            dev.langchain4j.model.chat.response.ChatResponse response =
                    visionModel.chat(userMessage);
            aiResponse = response.aiMessage().text();
            try {
                if (response.metadata() != null && response.metadata().tokenUsage() != null) {
                    usage[0] = response.metadata().tokenUsage().inputTokenCount();
                    usage[1] = response.metadata().tokenUsage().outputTokenCount();
                }
            } catch (Exception ignored) {
            }
            com.xy6f.fiva_assistant.service.AiCallLogService logSvc = aiCallLogService;
            logSvc.record(UserContext.getUserId(), com.xy6f.fiva_assistant.service.AiCallLogService.TYPE_OCR,
                    usage[0], usage[1], System.currentTimeMillis() - ocrStartMs, true, null);
        } catch (Exception e) {
            aiCallLogService.record(UserContext.getUserId(), com.xy6f.fiva_assistant.service.AiCallLogService.TYPE_OCR,
                    null, null, System.currentTimeMillis() - ocrStartMs, false, e.getMessage());
            throw e;
        }
        log.info("OCR AI response: {}", aiResponse);

        // Jackson 结构化解析（失败自动带修复提示重试一次）
        InvoiceOcrResult result = parseWithRetry(userMessage, aiResponse);
        result.setRawText(aiResponse);

        // Save to database
        Invoice invoice = Invoice.builder()
                .invoiceNo(result.getInvoiceNo())
                .invoiceDate(result.getInvoiceDate() != null
                        ? LocalDate.parse(result.getInvoiceDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .sellerName(result.getSellerName())
                .buyerName(result.getBuyerName())
                .amount(result.getAmount())
                .taxAmount(result.getTaxAmount())
                .totalAmount(result.getTotalAmount())
                .category(result.getCategory())
                .imagePath(imageUrl)
                .ocrRawText(aiResponse)
                .status("RECOGNIZED")
                .userId(UserContext.getUserId())
                .build();
        invoiceMapper.insert(invoice);

        result.setImageUrl(imageUrl);
        return result;
    }

    /**
     * 解析 AI 返回内容：直接解析 → 提取JSON块解析 → 带修复提示重试一次
     */
    private InvoiceOcrResult parseWithRetry(UserMessage originalMessage, String aiResponse) {
        InvoiceOcrResult result = ocrParser.parse(aiResponse);
        if (Boolean.TRUE.equals(result.getParseSuccess())) {
            return result;
        }

        // 重试一次：要求模型修复输出为纯JSON
        try {
            String repaired = visionModel.chat(
                    originalMessage,
                    dev.langchain4j.data.message.AiMessage.from(aiResponse),
                    UserMessage.from(OCR_REPAIR_PROMPT)
            ).aiMessage().text();
            InvoiceOcrResult retry = ocrParser.parse(repaired);
            if (Boolean.TRUE.equals(retry.getParseSuccess())) {
                retry.setRawText(aiResponse);
                return retry;
            }
        } catch (Exception e) {
            log.warn("OCR repair retry failed: {}", e.getMessage());
        }

        result.setRawText(aiResponse);
        result.setParseSuccess(false);
        result.setOverallConfidence(0.0);
        return result;
    }

    public Invoice saveInvoice(Invoice invoice) {
        invoice.setUserId(UserContext.getUserId());
        invoiceMapper.insert(invoice);
        return invoice;
    }

    public Invoice updateInvoice(Long id, Invoice updated) {
        Long userId = UserContext.getUserId();
        Invoice invoice = invoiceMapper.findById(id);
        if (invoice == null || !userId.equals(invoice.getUserId())) throw new RuntimeException("发票不存在或无权访问");
        invoice.setInvoiceNo(updated.getInvoiceNo());
        invoice.setInvoiceDate(updated.getInvoiceDate());
        invoice.setSellerName(updated.getSellerName());
        invoice.setBuyerName(updated.getBuyerName());
        invoice.setAmount(updated.getAmount());
        invoice.setTaxAmount(updated.getTaxAmount());
        invoice.setTotalAmount(updated.getTotalAmount());
        invoice.setCategory(updated.getCategory());
        invoiceMapper.update(invoice);
        return invoice;
    }

    public Invoice getInvoice(Long id) {
        Long userId = UserContext.getUserId();
        Invoice invoice = invoiceMapper.findById(id);
        if (invoice == null || !userId.equals(invoice.getUserId())) throw new RuntimeException("发票不存在或无权访问");
        return invoice;
    }

    public List<Invoice> listInvoices() {
        return invoiceMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public void deleteInvoice(Long id) {
        Long userId = UserContext.getUserId();
        Invoice invoice = invoiceMapper.findById(id);
        if (invoice == null || !userId.equals(invoice.getUserId())) throw new RuntimeException("发票不存在或无权访问");
        invoiceMapper.deleteById(id);
    }

    /**
     * 从发票生成凭证
     */
    @Transactional
    public Voucher createVoucherFromInvoice(Long invoiceId, String type, String category, String description) {
        Invoice invoice = getInvoice(invoiceId);
        Voucher voucher = Voucher.builder()
                .type(type != null ? type : "EXPENSE")
                .amount(invoice.getTotalAmount() != null ? invoice.getTotalAmount() : invoice.getAmount())
                .category(category != null ? category : invoice.getCategory())
                .description(description != null ? description : "发票:" + invoice.getInvoiceNo())
                .voucherDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now())
                .counterparty(invoice.getSellerName())
                .hasInvoice(true)
                .invoiceImageUrl(invoice.getImagePath())
                .status("DRAFT")
                .userId(UserContext.getUserId())
                .build();
        return voucherService.save(voucher);
    }

    /**
     * 单张发票查验
     */
    @Transactional
    public Invoice verifyInvoice(Long id) {
        Invoice invoice = getInvoice(id);

        // AI辅助分析发票要素
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下发票信息的合理性，判断发票是否存在异常：\n");
        prompt.append("发票号码: ").append(invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : "无").append("\n");
        prompt.append("开票日期: ").append(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : "无").append("\n");
        prompt.append("销售方: ").append(invoice.getSellerName() != null ? invoice.getSellerName() : "无").append("\n");
        prompt.append("购买方: ").append(invoice.getBuyerName() != null ? invoice.getBuyerName() : "无").append("\n");
        prompt.append("金额: ").append(invoice.getAmount()).append("\n");
        prompt.append("税额: ").append(invoice.getTaxAmount()).append("\n");
        prompt.append("价税合计: ").append(invoice.getTotalAmount()).append("\n");
        prompt.append("类别: ").append(invoice.getCategory() != null ? invoice.getCategory() : "无").append("\n");
        prompt.append("\n请检查：1.金额与税额是否匹配(价税合计=金额+税额) 2.税率是否合理(常见13%/9%/6%/3%/1%) 3.发票要素是否完整 4.是否存在异常特征");
        prompt.append("\n请以简洁中文回复，给出查验结论和建议。");

        String aiResult;
        try {
            dev.langchain4j.data.message.UserMessage msg = dev.langchain4j.data.message.UserMessage.from(prompt.toString());
            aiResult = visionModel.chat(msg).aiMessage().text();
        } catch (Exception e) {
            log.warn("AI查验失败，使用本地校验: {}", e.getMessage());
            aiResult = localVerify(invoice);
        }

        invoice.setVerified(true);
        invoice.setVerifiedAt(LocalDateTime.now());
        invoice.setVerifyMessage(aiResult);
        invoiceMapper.update(invoice);

        return invoice;
    }

    /**
     * 批量查验
     */
    @Transactional
    public List<Invoice> batchVerify() {
        Long userId = UserContext.getUserId();
        List<Invoice> invoices = invoiceMapper.findByUserIdOrderByCreatedAtDesc(userId);
        List<Invoice> verified = new ArrayList<>();
        for (Invoice inv : invoices) {
            if (inv.getVerified() == null || !inv.getVerified()) {
                try {
                    verifyInvoice(inv.getId());
                    verified.add(inv);
                } catch (Exception e) {
                    log.warn("发票{}查验失败: {}", inv.getId(), e.getMessage());
                }
            }
        }
        return verified;
    }

    /**
     * 本地基础校验
     */
    private String localVerify(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本地校验结果】\n");

        // 检查金额关系
        if (invoice.getAmount() != null && invoice.getTaxAmount() != null && invoice.getTotalAmount() != null) {
            BigDecimal expected = invoice.getAmount().add(invoice.getTaxAmount());
            if (expected.compareTo(invoice.getTotalAmount()) != 0) {
                sb.append("⚠️ 金额异常：金额+").append("税额≠价税合计\n");
            } else {
                sb.append("✅ 金额关系正确\n");
            }
            // 检查税率（税率档来自财税规则表）
            if (invoice.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxRate = invoice.getTaxAmount().divide(invoice.getAmount(), 4, java.math.RoundingMode.HALF_UP);
                double rate = taxRate.doubleValue();
                List<BigDecimal> tiers = taxRuleEngine.vatRateTiers();
                boolean match = false;
                for (BigDecimal tier : tiers) {
                    if (Math.abs(rate - tier.doubleValue()) < 0.001) {
                        sb.append("✅ 税率正常: ").append(String.format("%.1f%%", rate * 100)).append("\n");
                        match = true;
                        break;
                    }
                }
                if (!match && rate > 0) {
                    String tiersStr = tiers.stream().map(t -> String.format("%.0f%%", t.doubleValue() * 100))
                            .collect(java.util.stream.Collectors.joining("/"));
                    sb.append("⚠️ 税率非常见(").append(tiersStr).append("): ").append(String.format("%.2f%%", rate * 100)).append("\n");
                }
            }
        }

        // 检查要素完整性
        if (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isEmpty()) sb.append("⚠️ 缺少发票号码\n");
        if (invoice.getSellerName() == null || invoice.getSellerName().isEmpty()) sb.append("⚠️ 缺少销售方名称\n");
        if (invoice.getBuyerName() == null || invoice.getBuyerName().isEmpty()) sb.append("⚠️ 缺少购买方名称\n");
        if (invoice.getInvoiceDate() == null) sb.append("⚠️ 缺少开票日期\n");

        sb.append("\nℹ️ 本地校验仅供参考，建议对接国家税务总局查验平台进行正式查验。");
        return sb.toString();
    }
}
