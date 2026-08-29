package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.SubjectTemplate;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.SubjectTemplateMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountMapper accountMapper;
    private final SubjectTemplateMapper subjectTemplateMapper;
    private final VoucherEntryMapper voucherEntryMapper;

    /**
     * 获取当前用户的所有科目（若为空则自动初始化默认科目表）
     */
    public List<Account> listAll() {
        Long userId = UserContext.getUserId();
        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        if (accounts.isEmpty()) {
            accounts = initDefaultAccounts(userId);
        }
        return accounts;
    }

    /**
     * 获取所有科目（含禁用）
     */
    public List<Account> listAllIncludingDisabled() {
        return accountMapper.findByUserIdOrderByCodeAsc(UserContext.getUserId());
    }

    /**
     * 按类型筛选
     */
    public List<Account> listByType(String type) {
        return accountMapper.findByUserIdAndTypeOrderByCodeAsc(UserContext.getUserId(), type);
    }

    public Account getById(Long id) {
        Long userId = UserContext.getUserId();
        Account account = accountMapper.findByIdAndUserId(id, userId);
        if (account == null) throw new RuntimeException("科目不存在或无权访问");
        return account;
    }

    @Transactional
    public Account create(Account account) {
        Long userId = UserContext.getUserId();
        // 校验编码唯一性
        if (accountMapper.existsByUserIdAndCode(userId, account.getCode())) {
            throw new RuntimeException("科目编码已存在: " + account.getCode());
        }
        account.setId(null);
        account.setUserId(userId);
        // 根据科目类型自动设置余额方向
        if (account.getBalanceDirection() == null || account.getBalanceDirection().isBlank()) {
            String type = account.getType();
            if ("ASSET".equals(type) || "EXPENSE".equals(type)) {
                account.setBalanceDirection("DEBIT");
            } else {
                account.setBalanceDirection("CREDIT");
            }
        }
        accountMapper.insert(account);
        return account;
    }

    @Transactional
    public Account update(Long id, Account updated) {
        Long userId = UserContext.getUserId();
        Account account = accountMapper.findByIdAndUserId(id, userId);
        if (account == null) throw new RuntimeException("科目不存在或无权访问");
        // 如果编码变了，校验新编码唯一性
        if (!account.getCode().equals(updated.getCode())) {
            if (accountMapper.existsByUserIdAndCode(userId, updated.getCode())) {
                throw new RuntimeException("科目编码已存在: " + updated.getCode());
            }
        }
        account.setCode(updated.getCode());
        account.setName(updated.getName());
        account.setType(updated.getType());
        account.setBalanceDirection(updated.getBalanceDirection());
        account.setEnabled(updated.getEnabled());
        accountMapper.update(account);
        return account;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Account account = accountMapper.findByIdAndUserId(id, userId);
        if (account == null) throw new RuntimeException("科目不存在或无权访问");
        if (voucherEntryMapper.existsByAccountId(id)) {
            throw new RuntimeException("该科目已有凭证记录，无法删除");
        }
        accountMapper.deleteById(id);
    }

    /**
     * 初始化科目表：从科目模板表读取（模板由 DataInitializer 启动时播种）
     */
    @Transactional
    public List<Account> initDefaultAccounts(Long userId) {
        List<SubjectTemplate> templates = subjectTemplateMapper.findAllOrderByCodeAsc();
        List<Account> defaults = new ArrayList<>();
        for (SubjectTemplate t : templates) {
            // 幂等：已有同编码科目的跳过，避免重复初始化产生重复科目
            if (accountMapper.existsByUserIdAndCode(userId, t.getCode())) {
                continue;
            }
            defaults.add(buildAccount(userId, t.getCode(), t.getName(), t.getType(), t.getBalanceDirection()));
        }
        for (Account acc : defaults) {
            accountMapper.insert(acc);
        }
        log.info("为用户 {} 从科目模板初始化 {} 个默认科目", userId, defaults.size());
        return defaults;
    }

    private Account buildAccount(Long userId, String code, String name, String type, String direction) {
        return Account.builder()
                .userId(userId)
                .code(code)
                .name(name)
                .type(type)
                .balanceDirection(direction)
                .enabled(true)
                .build();
    }
}
