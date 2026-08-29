package com.xy6f.fiva_assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.model.VoucherTemplate;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.VoucherTemplateMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherTemplateService {

    private final VoucherTemplateMapper templateMapper;
    private final AccountMapper accountMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有模板
     */
    public List<VoucherTemplate> listAll() {
        return templateMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    /**
     * 按类型筛选
     */
    public List<VoucherTemplate> listByType(String type) {
        return templateMapper.findByUserIdAndTypeOrderByCreatedAtDesc(UserContext.getUserId(), type);
    }

    /**
     * 获取单个模板
     */
    public VoucherTemplate getById(Long id) {
        Long userId = UserContext.getUserId();
        VoucherTemplate template = templateMapper.findById(id);
        if (template == null || !userId.equals(template.getUserId())) throw new RuntimeException("模板不存在或无权访问");
        return template;
    }

    /**
     * 创建模板
     */
    @Transactional
    public VoucherTemplate create(VoucherTemplate template) {
        template.setId(null);
        template.setUserId(UserContext.getUserId());
        templateMapper.insert(template);
        return template;
    }

    /**
     * 更新模板
     */
    @Transactional
    public VoucherTemplate update(Long id, VoucherTemplate updated) {
        VoucherTemplate template = getById(id);
        template.setName(updated.getName());
        template.setType(updated.getType());
        template.setDescription(updated.getDescription());
        template.setEntriesJson(updated.getEntriesJson());
        templateMapper.update(template);
        return template;
    }

    /**
     * 删除模板
     */
    @Transactional
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        VoucherTemplate template = templateMapper.findById(id);
        if (template == null || !userId.equals(template.getUserId())) throw new RuntimeException("模板不存在或无权访问");
        templateMapper.deleteById(id);
    }

    /**
     * 应用模板生成凭证数据（返回预填好的凭证结构，不保存）
     * 将模板中的科目编码转换为实际的科目ID
     */
    public Map<String, Object> applyTemplate(Long templateId, BigDecimal amount, LocalDate voucherDate) {
        VoucherTemplate template = getById(templateId);
        Long userId = UserContext.getUserId();

        // 获取用户的所有科目，建立编码到ID的映射
        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<String, Long> codeToIdMap = accounts.stream()
                .collect(Collectors.toMap(Account::getCode, Account::getId, (a, b) -> a));

        // 解析模板分录JSON
        List<Map<String, Object>> templateEntries = parseEntriesJson(template.getEntriesJson());
        if (templateEntries == null || templateEntries.isEmpty()) {
            throw new RuntimeException("模板分录为空");
        }

        // 转换分录：科目编码 -> 科目ID，金额按比例调整
        List<Map<String, Object>> convertedEntries = new ArrayList<>();
        BigDecimal templateTotal = calculateTemplateTotal(templateEntries);

        for (Map<String, Object> entry : templateEntries) {
            String accountCode = (String) entry.get("accountCode");
            Long accountId = codeToIdMap.get(accountCode);
            if (accountId == null) {
                throw new RuntimeException("找不到科目编码: " + accountCode + "，请先在科目表中添加该科目");
            }

            Map<String, Object> converted = new LinkedHashMap<>();
            converted.put("accountId", accountId);
            converted.put("accountCode", accountCode);
            converted.put("summary", entry.getOrDefault("summary", ""));

            // 如果有指定金额则使用指定金额，否则按比例计算
            BigDecimal debitAmount = toBigDecimal(entry.get("debitAmount"));
            BigDecimal creditAmount = toBigDecimal(entry.get("creditAmount"));

            if (amount != null && templateTotal.compareTo(BigDecimal.ZERO) > 0) {
                // 按比例调整
                BigDecimal ratio = amount.divide(templateTotal, 4, java.math.RoundingMode.HALF_UP);
                if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
                    converted.put("debitAmount", debitAmount.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP));
                    converted.put("creditAmount", BigDecimal.ZERO);
                } else {
                    converted.put("debitAmount", BigDecimal.ZERO);
                    converted.put("creditAmount", creditAmount != null ? creditAmount.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
                }
            } else {
                converted.put("debitAmount", debitAmount != null ? debitAmount : BigDecimal.ZERO);
                converted.put("creditAmount", creditAmount != null ? creditAmount : BigDecimal.ZERO);
            }

            convertedEntries.add(converted);
        }

        // 构建返回结构
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> voucher = new LinkedHashMap<>();
        voucher.put("type", template.getType());
        voucher.put("description", template.getDescription());
        voucher.put("voucherDate", voucherDate != null ? voucherDate.toString() : LocalDate.now().toString());
        voucher.put("status", "DRAFT");
        result.put("voucher", voucher);
        result.put("entries", convertedEntries);
        result.put("templateName", template.getName());

        return result;
    }

    /**
     * 初始化默认模板（首次使用时）
     */
    @Transactional
    public List<VoucherTemplate> initDefaultTemplates(Long userId) {
        List<VoucherTemplate> defaults = new ArrayList<>();

        // 1. 收款模板
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("收款-银行收款")
                .type("INCOME")
                .description("收到客户款项存入银行")
                .entriesJson("[{\"accountCode\":\"1002\",\"summary\":\"收到货款\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"5001\",\"summary\":\"销售收入\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        // 2. 付款模板
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("付款-银行付款")
                .type("EXPENSE")
                .description("从银行支付费用")
                .entriesJson("[{\"accountCode\":\"6602\",\"summary\":\"支付费用\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"1002\",\"summary\":\"银行付款\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        // 3. 现金收款
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("收款-现金收款")
                .type("INCOME")
                .description("收到现金款项")
                .entriesJson("[{\"accountCode\":\"1001\",\"summary\":\"收到现金\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"5001\",\"summary\":\"销售收入\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        // 4. 现金付款
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("付款-现金付款")
                .type("EXPENSE")
                .description("支付现金费用")
                .entriesJson("[{\"accountCode\":\"6602\",\"summary\":\"管理费用\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"1001\",\"summary\":\"现金付款\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        // 5. 应收账款
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("应收-赊销")
                .type("INCOME")
                .description("赊销商品，形成应收账款")
                .entriesJson("[{\"accountCode\":\"1122\",\"summary\":\"应收货款\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"5001\",\"summary\":\"销售收入\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        // 6. 应付账款
        defaults.add(VoucherTemplate.builder()
                .userId(userId)
                .name("应付-赊购")
                .type("EXPENSE")
                .description("赊购商品，形成应付账款")
                .entriesJson("[{\"accountCode\":\"1401\",\"summary\":\"购入商品\",\"debitAmount\":10000,\"creditAmount\":0},{\"accountCode\":\"2202\",\"summary\":\"应付货款\",\"debitAmount\":0,\"creditAmount\":10000}]")
                .build());

        for (VoucherTemplate t : defaults) {
            templateMapper.insert(t);
        }
        log.info("为用户 {} 初始化 {} 个默认凭证模板", userId, defaults.size());
        return defaults;
    }

    // ---- 辅助方法 ----

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseEntriesJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析模板分录JSON失败: {}", json, e);
            return null;
        }
    }

    private BigDecimal calculateTemplateTotal(List<Map<String, Object>> entries) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> entry : entries) {
            BigDecimal debit = toBigDecimal(entry.get("debitAmount"));
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(debit);
            }
        }
        return total;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
