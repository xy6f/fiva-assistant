package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.BankTransaction;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.BankTransactionMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransactionService {

    private final BankTransactionMapper bankTransactionMapper;
    private final AccountMapper accountMapper;
    private final VoucherService voucherService;
    private final AccountMappingService accountMappingService;

    /**
     * 导入Excel流水文件
     */
    @Transactional
    public List<BankTransaction> importExcel(MultipartFile file) throws Exception {
        Long userId = UserContext.getUserId();
        List<BankTransaction> transactions = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // 跳过表头，从第2行开始
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                BankTransaction bt = parseExcelRow(row, userId);
                if (bt != null && bt.getTransactionDate() != null) {
                    transactions.add(bt);
                }
            }
        }

        for (BankTransaction bt : transactions) {
            bankTransactionMapper.insert(bt);
        }
        log.info("用户{}导入Excel流水{}条", userId, transactions.size());
        return transactions;
    }

    /**
     * 导入CSV流水文件
     */
    @Transactional
    public List<BankTransaction> importCsv(MultipartFile file) throws Exception {
        Long userId = UserContext.getUserId();
        List<BankTransaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "GBK"))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = stripBom(line);
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                BankTransaction bt = parseCsvLine(line, userId);
                if (bt != null && bt.getTransactionDate() != null) {
                    transactions.add(bt);
                }
            }
        } catch (Exception e) {
            // 尝试UTF-8重新读取，清空已解析的部分数据防止重复
            transactions = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    line = stripBom(line);
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    BankTransaction bt = parseCsvLine(line, userId);
                    if (bt != null && bt.getTransactionDate() != null) {
                        transactions.add(bt);
                    }
                }
            }
        }

        for (BankTransaction bt : transactions) {
            bankTransactionMapper.insert(bt);
        }
        log.info("用户{}导入CSV流水{}条", userId, transactions.size());
        return transactions;
    }

    public List<BankTransaction> listAll() {
        return bankTransactionMapper.findByUserIdOrderByTransactionDateDescCreatedAtDesc(UserContext.getUserId());
    }

    public List<BankTransaction> listByStatus(String status) {
        return bankTransactionMapper.findByUserIdAndStatusOrderByTransactionDateDesc(UserContext.getUserId(), status);
    }

    /**
     * 单条流水智能匹配生成凭证
     */
    @Transactional
    public Voucher matchAndGenerate(Long id) {
        BankTransaction bt = bankTransactionMapper.findById(id);
        if (bt == null) throw new RuntimeException("流水不存在");
        if (!"UNMATCHED".equals(bt.getStatus())) {
            throw new RuntimeException("该流水已处理");
        }

        Long userId = UserContext.getUserId();
        // 银行流水惯例：借方(debit)=支出，贷方(credit)=收入
        boolean hasDebit = bt.getDebitAmount() != null && bt.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasCredit = bt.getCreditAmount() != null && bt.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal amount = hasDebit ? bt.getDebitAmount() : bt.getCreditAmount();
        boolean isIncome = !hasDebit && hasCredit;

        // 智能匹配科目
        String summary = bt.getSummary() != null ? bt.getSummary() : "";
        String counterparty = bt.getCounterparty() != null ? bt.getCounterparty() : "";
        String combined = summary + counterparty;

        Account bankAccount = accountMapper.findByUserIdAndCode(userId, "1002");
        Account cashAccount = accountMapper.findByUserIdAndCode(userId, "1001");
        Account expenseAccount = accountMapper.findByUserIdAndCode(userId, "6601");
        Account incomeAccount = accountMapper.findByUserIdAndCode(userId, "5001");

        List<VoucherEntry> entries = new ArrayList<>();

        if (isIncome) {
            // 收入类：借 银行存款 / 贷 收入科目
            Account creditAccount = matchIncomeAccount(userId, combined, incomeAccount);
            if (bankAccount != null) {
                entries.add(VoucherEntry.builder().accountId(bankAccount.getId())
                        .summary(summary).debitAmount(amount).creditAmount(BigDecimal.ZERO).build());
            }
            if (creditAccount != null) {
                entries.add(VoucherEntry.builder().accountId(creditAccount.getId())
                        .summary(summary).debitAmount(BigDecimal.ZERO).creditAmount(amount).build());
            }
        } else {
            // 支出类：借 费用科目 / 贷 银行存款
            Account debitAccount = matchExpenseAccount(userId, combined, expenseAccount);
            if (debitAccount != null) {
                entries.add(VoucherEntry.builder().accountId(debitAccount.getId())
                        .summary(summary).debitAmount(amount).creditAmount(BigDecimal.ZERO).build());
            }
            if (bankAccount != null) {
                entries.add(VoucherEntry.builder().accountId(bankAccount.getId())
                        .summary(summary).debitAmount(BigDecimal.ZERO).creditAmount(amount).build());
            }
        }

        if (entries.size() < 2) {
            throw new RuntimeException("无法匹配到足够的科目生成凭证");
        }

        Voucher voucher = Voucher.builder()
                .type(isIncome ? "INCOME" : "EXPENSE")
                .amount(amount)
                .description(summary + (counterparty.isEmpty() ? "" : " - " + counterparty))
                .voucherDate(bt.getTransactionDate())
                .counterparty(counterparty)
                .build();

        Voucher saved = voucherService.save(voucher, entries);

        bt.setStatus("MATCHED");
        bt.setVoucherId(saved.getId());
        bankTransactionMapper.update(bt);

        return saved;
    }

    /**
     * 批量生成凭证
     */
    @Transactional
    public List<Voucher> batchGenerate(List<Long> ids) {
        List<Voucher> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                Voucher v = matchAndGenerate(id);
                results.add(v);
            } catch (Exception e) {
                log.warn("流水{}匹配失败: {}", id, e.getMessage());
            }
        }
        return results;
    }

    /**
     * 标记忽略
     */
    @Transactional
    public void ignore(Long id) {
        BankTransaction bt = bankTransactionMapper.findById(id);
        if (bt == null) throw new RuntimeException("流水不存在");
        bt.setStatus("IGNORED");
        bankTransactionMapper.update(bt);
    }

    @Transactional
    public void delete(Long id) {
        BankTransaction bt = bankTransactionMapper.findById(id);
        if (bt != null && "MATCHED".equals(bt.getStatus())) {
            throw new RuntimeException("已匹配的流水不可删除，请先删除对应凭证");
        }
        bankTransactionMapper.deleteById(id);
    }

    // ---- 智能匹配辅助方法 ----

    private Account matchIncomeAccount(Long userId, String text, Account defaultAccount) {
        // 从关键词科目映射表匹配收入科目（CREDIT 方向）
        Account mapped = accountMappingService.resolveAccount(userId, text, "CREDIT");
        return mapped != null ? mapped : defaultAccount;
    }

    private Account matchExpenseAccount(Long userId, String text, Account defaultAccount) {
        // 从关键词科目映射表匹配费用科目（DEBIT 方向）
        Account mapped = accountMappingService.resolveAccount(userId, text, "DEBIT");
        return mapped != null ? mapped : defaultAccount;
    }

    // ---- 解析辅助方法 ----

    private BankTransaction parseExcelRow(Row row, Long userId) {
        try {
            BankTransaction bt = new BankTransaction();
            bt.setUserId(userId);

            // 尝试解析各列：日期 | 摘要 | 对方 | 借方 | 贷方 | 余额
            int colIdx = 0;
            // 日期列
            Cell dateCell = getCell(row, colIdx++);
            if (dateCell == null) return null;
            bt.setTransactionDate(parseDateCell(dateCell));

            // 摘要
            Cell summaryCell = getCell(row, colIdx++);
            bt.setSummary(getCellString(summaryCell));

            // 对方
            Cell cpCell = getCell(row, colIdx++);
            bt.setCounterparty(getCellString(cpCell));

            // 借方(收入)
            Cell debitCell = getCell(row, colIdx++);
            bt.setDebitAmount(parseAmountCell(debitCell));

            // 贷方(支出)
            Cell creditCell = getCell(row, colIdx++);
            bt.setCreditAmount(parseAmountCell(creditCell));

            // 余额
            Cell balanceCell = getCell(row, colIdx);
            bt.setBalance(parseAmountCell(balanceCell));

            return bt;
        } catch (Exception e) {
            log.debug("解析Excel行失败: {}", e.getMessage());
            return null;
        }
    }

    private String stripBom(String line) {
        if (line != null && line.startsWith("\uFEFF")) {
            return line.substring(1);
        }
        return line;
    }

    private BankTransaction parseCsvLine(String line, Long userId) {
        try {
            String[] parts = line.split("[,\\t]");
            if (parts.length < 4) return null;

            BankTransaction bt = new BankTransaction();
            bt.setUserId(userId);

            int idx = 0;
            bt.setTransactionDate(parseDateString(parts[idx++].trim()));
            bt.setSummary(idx < parts.length ? parts[idx++].trim() : "");
            bt.setCounterparty(idx < parts.length ? parts[idx++].trim() : "");

            BigDecimal debit = idx < parts.length ? parseAmount(parts[idx++].trim()) : BigDecimal.ZERO;
            BigDecimal credit = idx < parts.length ? parseAmount(parts[idx++].trim()) : BigDecimal.ZERO;
            BigDecimal balance = idx < parts.length ? parseAmount(parts[idx].trim()) : null;

            bt.setDebitAmount(debit);
            bt.setCreditAmount(credit);
            bt.setBalance(balance);

            return bt;
        } catch (Exception e) {
            log.debug("解析CSV行失败: {}", e.getMessage());
            return null;
        }
    }

    private Cell getCell(Row row, int col) {
        if (row == null) return null;
        return row.getCell(col);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate parseDateCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String str = cell.getStringCellValue().trim();
        return parseDateString(str);
    }

    private LocalDate parseDateString(String str) {
        if (str == null || str.isEmpty()) return null;
        str = str.trim();
        // 尝试多种日期格式
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyyMMdd", "MM/dd/yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseAmountCell(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String str = cell.getStringCellValue().trim();
        return parseAmount(str);
    }

    private BigDecimal parseAmount(String str) {
        if (str == null || str.isEmpty()) return BigDecimal.ZERO;
        str = str.replaceAll("[,，\\s¥￥]", "");
        if (str.isEmpty() || "-".equals(str)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
