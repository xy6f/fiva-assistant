package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.FixedAsset;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.FixedAssetMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedAssetService {

    private final FixedAssetMapper fixedAssetMapper;
    private final AccountMapper accountMapper;
    private final VoucherService voucherService;
    private final AccountResolver accountResolver;

    public List<FixedAsset> listAll() {
        return fixedAssetMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public FixedAsset getById(Long id) {
        FixedAsset asset = fixedAssetMapper.findById(id);
        if (asset == null) throw new RuntimeException("资产不存在");
        return asset;
    }

    @Transactional
    public FixedAsset create(FixedAsset asset) {
        asset.setUserId(UserContext.getUserId());
        if (asset.getStatus() == null || asset.getStatus().isBlank()) {
            asset.setStatus("IN_USE");
        }
        if (asset.getAccumulatedDepreciation() == null) {
            asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        }
        if (asset.getDepreciatedMonths() == null) {
            asset.setDepreciatedMonths(0);
        }
        // 自动计算月折旧额
        calculateMonthlyDepreciation(asset);
        // 自动设置默认科目
        setDefaultAccounts(asset);
        fixedAssetMapper.insert(asset);
        return asset;
    }

    @Transactional
    public FixedAsset update(Long id, FixedAsset updated) {
        FixedAsset asset = getById(id);
        asset.setAssetCode(updated.getAssetCode());
        asset.setAssetName(updated.getAssetName());
        asset.setCategory(updated.getCategory());
        asset.setPurchaseDate(updated.getPurchaseDate());
        asset.setOriginalValue(updated.getOriginalValue());
        asset.setSalvageValue(updated.getSalvageValue());
        asset.setDepreciationMethod(updated.getDepreciationMethod());
        asset.setUsefulLifeMonths(updated.getUsefulLifeMonths());
        if (updated.getAccountId() != null) asset.setAccountId(updated.getAccountId());
        if (updated.getDepreciationAccountId() != null) asset.setDepreciationAccountId(updated.getDepreciationAccountId());
        if (updated.getExpenseAccountId() != null) asset.setExpenseAccountId(updated.getExpenseAccountId());
        // 重新计算月折旧额
        calculateMonthlyDepreciation(asset);
        fixedAssetMapper.update(asset);
        return asset;
    }

    @Transactional
    public void delete(Long id) {
        fixedAssetMapper.deleteById(id);
    }

    /**
     * 计算某资产的月折旧额
     */
    public BigDecimal calculateDepreciation(Long id) {
        FixedAsset asset = getById(id);
        return calculateMonthlyDepreciation(asset);
    }

    /**
     * 批量生成期间折旧凭证
     * period格式: yyyy-MM
     */
    @Transactional
    public List<Voucher> generateDepreciationVoucher(String period) {
        Long userId = UserContext.getUserId();
        List<FixedAsset> assets = fixedAssetMapper.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "IN_USE");
        List<Voucher> vouchers = new ArrayList<>();

        // 汇总所有资产的月折旧
        BigDecimal totalDepreciation = BigDecimal.ZERO;
        Long expenseAccountId = null;
        Long depreciationAccountId = null;

        for (FixedAsset asset : assets) {
            BigDecimal monthlyDep = calculateMonthlyDepreciation(asset);
            if (monthlyDep.compareTo(BigDecimal.ZERO) > 0) {
                totalDepreciation = totalDepreciation.add(monthlyDep);
                if (expenseAccountId == null) {
                    expenseAccountId = asset.getExpenseAccountId();
                    depreciationAccountId = asset.getDepreciationAccountId();
                }
                // 更新累计折旧和已折旧月数
                asset.setAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(monthlyDep));
                asset.setDepreciatedMonths(asset.getDepreciatedMonths() + 1);
                fixedAssetMapper.update(asset);
            }
        }

        if (totalDepreciation.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("本期无需计提折旧");
        }

        // 兜底科目：不存在时自动补建
        Account expenseAcc = accountResolver.ensureAccount(userId, "6602", "管理费用-其他", "EXPENSE", "DEBIT");
        Account depAcc = accountResolver.ensureAccount(userId, "1602", "累计折旧", "ASSET", "CREDIT");
        if (expenseAccountId == null) expenseAccountId = expenseAcc.getId();
        if (depreciationAccountId == null) depreciationAccountId = depAcc.getId();

        List<VoucherEntry> entries = new ArrayList<>();
        entries.add(VoucherEntry.builder()
                .accountId(expenseAccountId)
                .summary(period + " 计提折旧")
                .debitAmount(totalDepreciation)
                .creditAmount(BigDecimal.ZERO)
                .build());
        entries.add(VoucherEntry.builder()
                .accountId(depreciationAccountId)
                .summary(period + " 计提折旧")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(totalDepreciation)
                .build());

        Voucher voucher = Voucher.builder()
                .type("EXPENSE")
                .amount(totalDepreciation)
                .description(period + " 固定资产折旧")
                .voucherDate(LocalDate.parse(period + "-01"))
                .category("折旧")
                .build();

        Voucher saved = voucherService.save(voucher, entries);
        vouchers.add(saved);

        log.info("用户{}生成{}期间折旧凭证，金额:{}", userId, period, totalDepreciation);
        return vouchers;
    }

    // ---- 辅助方法 ----

    private BigDecimal calculateMonthlyDepreciation(FixedAsset asset) {
        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal salvageValue = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
        Integer months = asset.getUsefulLifeMonths();

        if (months == null || months <= 0 || originalValue == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal depreciable = originalValue.subtract(salvageValue);
        if (depreciable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthly;
        String method = asset.getDepreciationMethod();
        int depreciatedMonths = asset.getDepreciatedMonths() != null ? asset.getDepreciatedMonths() : 0;

        if ("DOUBLE_DECLINING".equals(method)) {
            // 双倍余额递减法：年折旧率 = 2/使用年限(年)，月折旧 = 账面净值 * 年折旧率 / 12
            int years = months / 12;
            if (years <= 0) years = 1;
            BigDecimal rate = BigDecimal.valueOf(2.0 / years);
            BigDecimal remainingValue = originalValue.subtract(asset.getAccumulatedDepreciation());
            if (remainingValue.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            monthly = remainingValue.multiply(rate).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else if ("SUM_OF_YEARS".equals(method)) {
            // 年数总和法
            int years = months / 12;
            if (years <= 0) years = 1;
            int sumOfYears = years * (years + 1) / 2;
            int currentYear = depreciatedMonths / 12;
            int remainingLife = years - currentYear;
            if (remainingLife <= 0) remainingLife = 1;
            BigDecimal yearDep = depreciable.multiply(BigDecimal.valueOf(remainingLife))
                    .divide(BigDecimal.valueOf(sumOfYears), 2, RoundingMode.HALF_UP);
            monthly = yearDep.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else {
            // 直线法（默认）
            monthly = depreciable.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        asset.setMonthlyDepreciation(monthly);
        return monthly;
    }

    private void setDefaultAccounts(FixedAsset asset) {
        Long userId = asset.getUserId();
        if (asset.getAccountId() == null) {
            asset.setAccountId(accountResolver.ensureAccount(userId, "1601", "固定资产", "ASSET", "DEBIT").getId());
        }
        if (asset.getDepreciationAccountId() == null) {
            asset.setDepreciationAccountId(accountResolver.ensureAccount(userId, "1602", "累计折旧", "ASSET", "CREDIT").getId());
        }
        if (asset.getExpenseAccountId() == null) {
            asset.setExpenseAccountId(accountResolver.ensureAccount(userId, "6602", "管理费用-其他", "EXPENSE", "DEBIT").getId());
        }
    }
}
