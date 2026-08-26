package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.TaxRuleMapper;
import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.AccountMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关键词 → 会计科目映射服务（数据来自 t_account_mapping，按优先级匹配）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMappingService {

    private final TaxRuleMapper taxRuleMapper;
    private final AccountMapper accountMapper;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private volatile List<AccountMapping> cache;
    private volatile long cacheLoadedAt = 0;

    /**
     * 按文本（摘要/类别/描述）匹配科目编码；未命中返回 null。
     * direction: DEBIT=费用/资产方向, CREDIT=收入/负债方向
     */
    public String resolveAccountCode(String text, String direction) {
        if (text == null || text.isBlank()) return null;
        for (AccountMapping m : mappings()) {
            if (direction != null && !direction.equals(m.getDirection())) continue;
            if (text.contains(m.getKeyword())) {
                return m.getAccountCode();
            }
        }
        return null;
    }

    /**
     * 匹配并返回当前用户的科目实体；用户无该科目时返回 null
     */
    public Account resolveAccount(Long userId, String text, String direction) {
        String code = resolveAccountCode(text, direction);
        if (code == null) return null;
        return accountMapper.findByUserIdAndCode(userId, code);
    }

    private List<AccountMapping> mappings() {
        long now = System.currentTimeMillis();
        if (cache == null || now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (cache == null || now - cacheLoadedAt > CACHE_TTL_MS) {
                    try {
                        cache = taxRuleMapper.findAllMappingsOrderByPriority();
                        cacheLoadedAt = now;
                    } catch (Exception e) {
                        log.error("Failed to load account mappings: {}", e.getMessage());
                        return cache != null ? cache : List.of();
                    }
                }
            }
        }
        return cache;
    }
}
