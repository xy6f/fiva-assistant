package com.xy6f.fiva_assistant.service.tax;

import com.xy6f.fiva_assistant.mapper.TaxRuleMapper;
import com.xy6f.fiva_assistant.model.TaxRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 财税规则引擎：从 t_tax_rule 加载税率档与门槛参数，带 5 分钟缓存。
 * 未来替换为 Drools 等规则引擎时仅需重写本类与策略实现。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaxRuleEngine {

    private final TaxRuleMapper taxRuleMapper;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private volatile Map<String, TaxRule> cache = new ConcurrentHashMap<>();
    private volatile long cacheLoadedAt = 0;

    /** 取比率（含生效期校验），无规则时返回 fallback */
    public BigDecimal rate(String ruleKey, BigDecimal fallback) {
        TaxRule rule = active(ruleKey);
        return rule != null && rule.getRateValue() != null ? rule.getRateValue() : fallback;
    }

    /** 取门槛金额，无规则时返回 fallback */
    public BigDecimal threshold(String ruleKey, BigDecimal fallback) {
        TaxRule rule = active(ruleKey);
        return rule != null && rule.getThresholdValue() != null ? rule.getThresholdValue() : fallback;
    }

    /** 增值税有效税率档列表（用于发票查验） */
    public List<BigDecimal> vatRateTiers() {
        LocalDate today = LocalDate.now();
        return load().values().stream()
                .filter(r -> "VAT_RATE_TIER".equals(r.getTaxType()))
                .filter(r -> effective(r, today))
                .map(TaxRule::getRateValue)
                .filter(v -> v != null)
                .sorted()
                .collect(Collectors.toList());
    }

    private TaxRule active(String ruleKey) {
        TaxRule rule = load().get(ruleKey);
        if (rule == null) return null;
        return effective(rule, LocalDate.now()) ? rule : null;
    }

    private boolean effective(TaxRule rule, LocalDate date) {
        if (rule.getEffectiveFrom() != null && date.isBefore(rule.getEffectiveFrom())) return false;
        if (rule.getEffectiveTo() != null && date.isAfter(rule.getEffectiveTo())) return false;
        return true;
    }

    private Map<String, TaxRule> load() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - cacheLoadedAt > CACHE_TTL_MS) {
                    try {
                        cache = taxRuleMapper.findAll().stream()
                                .collect(Collectors.toMap(TaxRule::getRuleKey, Function.identity(), (a, b) -> a));
                        cacheLoadedAt = now;
                    } catch (Exception e) {
                        log.error("Failed to load tax rules, keep stale cache: {}", e.getMessage());
                    }
                }
            }
        }
        return cache;
    }
}
