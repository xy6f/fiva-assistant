package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 科目解析：按编码查找或自动补建（幂等），消除各服务硬编码兜底逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountResolver {

    private final AccountMapper accountMapper;

    /**
     * 按编码查找，不存在则创建并返回
     */
    @Transactional
    public Account ensureAccount(Long userId, String code, String name, String type, String direction) {
        Account acc = accountMapper.findByUserIdAndCode(userId, code);
        if (acc != null) return acc;
        try {
            Account newAcc = Account.builder()
                    .userId(userId)
                    .code(code)
                    .name(name)
                    .type(type)
                    .balanceDirection(direction)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            accountMapper.insert(newAcc);
            log.info("自动补建科目 {} {} - {}", code, name, userId);
            return newAcc;
        } catch (DuplicateKeyException e) {
            // 并发下被其他线程抢先创建，直接重查
            return accountMapper.findByUserIdAndCode(userId, code);
        }
    }
}
