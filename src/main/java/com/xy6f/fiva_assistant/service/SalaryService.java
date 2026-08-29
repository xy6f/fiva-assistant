package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.SalaryRecord;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.SalaryRecordMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryService {

    private final SalaryRecordMapper salaryRecordMapper;
    private final AccountMapper accountMapper;
    private final VoucherService voucherService;
    private final AccountResolver accountResolver;

    /**
     * 批量保存工资记录
     * 同一员工同一月份只允许一条记录：草稿覆盖更新，已过账则拒绝
     */
    @Transactional
    public List<SalaryRecord> batchSave(List<SalaryRecord> records) {
        Long userId = UserContext.getUserId();
        for (SalaryRecord r : records) {
            r.setId(null);
            r.setUserId(userId);
            if (r.getStatus() == null) r.setStatus("DRAFT");
            if (r.getMonth() == null || r.getMonth().isBlank()) {
                throw new IllegalArgumentException("工资记录缺少月份");
            }
            if (r.getEmployeeNo() == null) {
                r.setEmployeeNo("");
            }
            // 计算应发/实发并兜底各金额字段（JSON 反序列化不会应用 @Builder.Default）
            r.calculatePay();

            SalaryRecord existing = salaryRecordMapper.findByUserIdAndEmployeeNoAndMonth(
                    userId, r.getEmployeeNo(), r.getMonth());
            if (existing != null) {
                if ("POSTED".equals(existing.getStatus()) || "PAID".equals(existing.getStatus())) {
                    throw new IllegalArgumentException("员工[" + r.getEmployeeName() + "] "
                            + r.getMonth() + " 的工资已过账，不可重复提交");
                }
                // 覆盖未过账的旧记录
                r.setId(existing.getId());
                salaryRecordMapper.update(r);
            } else {
                try {
                    salaryRecordMapper.insert(r);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 并发插入时的唯一约束冲突：重新查询并更新
                    SalaryRecord raceExisting = salaryRecordMapper.findByUserIdAndEmployeeNoAndMonth(
                            userId, r.getEmployeeNo(), r.getMonth());
                    if (raceExisting != null && ("POSTED".equals(raceExisting.getStatus()) || "PAID".equals(raceExisting.getStatus()))) {
                        throw new IllegalArgumentException("员工[" + r.getEmployeeName() + "] "
                                + r.getMonth() + " 的工资已过账，不可重复提交");
                    }
                    if (raceExisting != null) {
                        r.setId(raceExisting.getId());
                        salaryRecordMapper.update(r);
                    }
                }
            }
        }
        log.info("用户{}批量保存{}条工资记录", userId, records.size());
        return records;
    }

    /**
     * 按月份查询
     */
    public List<SalaryRecord> listByMonth(String month) {
        return salaryRecordMapper.findByUserIdAndMonthOrderByDepartmentAscEmployeeNameAsc(
                UserContext.getUserId(), month);
    }

    /**
     * 删除单条
     */
    @Transactional
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        SalaryRecord r = salaryRecordMapper.findById(id);
        if (r == null || !userId.equals(r.getUserId())) throw new RuntimeException("记录不存在或无权访问");
        if ("POSTED".equals(r.getStatus()) || "PAID".equals(r.getStatus())) {
            throw new RuntimeException("已过账的记录不可删除");
        }
        salaryRecordMapper.deleteById(id);
    }

    /**
     * 生成工资计提凭证
     * 借: 管理费用-工资 / 贷: 应付职工薪酬
     */
    @Transactional
    public Voucher generateSalaryVoucher(String month) {
        Long userId = UserContext.getUserId();
        List<SalaryRecord> records = listByMonth(month);
        if (records.isEmpty()) {
            throw new RuntimeException("该月无工资记录");
        }
        if (records.stream().anyMatch(r -> "POSTED".equals(r.getStatus()) || "PAID".equals(r.getStatus()))) {
            throw new RuntimeException(month + " 的工资计提凭证已生成，请勿重复操作");
        }

        // 按部门汇总
        Map<String, BigDecimal> deptSum = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDepartment() != null ? r.getDepartment() : "管理部",
                        Collectors.reducing(BigDecimal.ZERO, SalaryRecord::getGrossPay, BigDecimal::add)));

        Account salaryPayable = accountResolver.ensureAccount(userId, "2211", "应付职工薪酬", "LIABILITY", "CREDIT");

        BigDecimal totalGross = BigDecimal.ZERO;
        List<VoucherEntry> entries = new ArrayList<>();

        // 按部门选择对应费用科目
        for (Map.Entry<String, BigDecimal> entry : deptSum.entrySet()) {
            String dept = entry.getKey();
            BigDecimal deptAmount = entry.getValue();
            totalGross = totalGross.add(deptAmount);

            Account expenseAccount;
            if (dept.contains("销售")) {
                expenseAccount = accountResolver.ensureAccount(userId, "6602", "销售费用", "EXPENSE", "DEBIT");
            } else if (dept.contains("生产")) {
                expenseAccount = accountResolver.ensureAccount(userId, "5001", "生产成本", "EXPENSE", "DEBIT");
            } else {
                expenseAccount = accountResolver.ensureAccount(userId, "6601", "管理费用", "EXPENSE", "DEBIT");
            }

            entries.add(VoucherEntry.builder()
                    .accountId(expenseAccount.getId())
                    .summary(month + " 工资-" + dept)
                    .debitAmount(deptAmount)
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        // 贷方：应付职工薪酬
        entries.add(VoucherEntry.builder()
                .accountId(salaryPayable.getId())
                .summary(month + " 计提工资")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(totalGross)
                .build());

        Voucher voucher = Voucher.builder()
                .type("EXPENSE")
                .amount(totalGross)
                .description(month + " 工资计提")
                .voucherDate(LocalDate.parse(month + "-01"))
                .category("工资")
                .build();

        Voucher saved = voucherService.save(voucher, entries);

        // 标记为已过账
        for (SalaryRecord r : records) {
            r.setStatus("POSTED");
            salaryRecordMapper.update(r);
        }

        log.info("用户{}生成{}月工资计提凭证，金额:{}", userId, month, totalGross);
        return saved;
    }

    /**
     * 生成工资发放凭证（分录）
     * 借: 应付职工薪酬 / 贷: 银行存款(实发) + 其他应付款(社保/公积金) + 应交税费(个税)
     */
    @Transactional
    public Voucher generatePaymentVoucher(String month) {
        Long userId = UserContext.getUserId();
        List<SalaryRecord> records = listByMonth(month);
        if (records.isEmpty()) {
            throw new RuntimeException("该月无工资记录");
        }
        if (records.stream().noneMatch(r -> "POSTED".equals(r.getStatus()))) {
            throw new RuntimeException("请先生成 " + month + " 的工资计提凭证，再进行发放");
        }
        if (records.stream().anyMatch(r -> "PAID".equals(r.getStatus()))) {
            throw new RuntimeException(month + " 的工资已发放，请勿重复操作");
        }

        Account salaryPayable = accountResolver.ensureAccount(userId, "2211", "应付职工薪酬", "LIABILITY", "CREDIT");
        Account bankAccount = accountResolver.ensureAccount(userId, "1002", "银行存款", "ASSET", "DEBIT");
        Account otherPayable = accountResolver.ensureAccount(userId, "2241", "其他应付款", "LIABILITY", "CREDIT");
        Account taxPayable = accountResolver.ensureAccount(userId, "2221", "应交税费", "LIABILITY", "CREDIT");

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;
        BigDecimal totalSocialInsurance = BigDecimal.ZERO;
        BigDecimal totalHousingFund = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SalaryRecord r : records) {
            totalGross = totalGross.add(r.getGrossPay());
            totalNetPay = totalNetPay.add(r.getNetPay());
            totalSocialInsurance = totalSocialInsurance.add(r.getSocialInsurance());
            totalHousingFund = totalHousingFund.add(r.getHousingFund());
            totalTax = totalTax.add(r.getTax());
        }

        List<VoucherEntry> entries = new ArrayList<>();

        // 借：应付职工薪酬（应发总额）
        entries.add(VoucherEntry.builder()
                .accountId(salaryPayable.getId())
                .summary(month + " 发放工资")
                .debitAmount(totalGross)
                .creditAmount(BigDecimal.ZERO)
                .build());

        // 贷：银行存款（实发）
        if (totalNetPay.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(VoucherEntry.builder()
                    .accountId(bankAccount.getId())
                    .summary(month + " 发放工资")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(totalNetPay)
                    .build());
        }

        // 贷：其他应付款-社保/公积金（个人部分）
        BigDecimal otherAmount = totalSocialInsurance.add(totalHousingFund);
        if (otherAmount.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(VoucherEntry.builder()
                    .accountId(otherPayable.getId())
                    .summary(month + " 代扣社保公积金")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(otherAmount)
                    .build());
        }

        // 贷：应交税费-个税
        if (totalTax.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(VoucherEntry.builder()
                    .accountId(taxPayable.getId())
                    .summary(month + " 代扣个税")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(totalTax)
                    .build());
        }

        Voucher voucher = Voucher.builder()
                .type("EXPENSE")
                .amount(totalGross)
                .description(month + " 工资发放")
                .voucherDate(LocalDate.parse(month + "-01"))
                .category("工资")
                .build();

        Voucher saved = voucherService.save(voucher, entries);

        // 标记为已发放，防止重复发放
        for (SalaryRecord r : records) {
            r.setStatus("PAID");
            salaryRecordMapper.update(r);
        }

        log.info("用户{}生成{}月工资发放凭证，金额:{}", userId, month, totalGross);
        return saved;
    }
}
