package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.annotation.Audit;
import com.xy6f.fiva_assistant.model.BankTransaction;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.service.BankTransactionService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    /**
     * 导入流水文件（Excel或CSV）
     */
    @PostMapping("/api/bank-transaction/import")
    @Audit(action = "BANK_IMPORT", recordTargetId = false)
    public ResponseEntity<?> importFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            List<BankTransaction> result;
            if (filename != null && filename.toLowerCase().endsWith(".csv")) {
                result = bankTransactionService.importCsv(file);
            } else {
                result = bankTransactionService.importExcel(file);
            }
            return ResponseEntity.ok(Map.of("message", "导入成功", "count", result.size(), "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * 查询流水列表
     */
    @GetMapping("/api/bank-transaction")
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        List<BankTransaction> data;
        if (status != null && !status.isEmpty()) {
            data = bankTransactionService.listByStatus(status);
        } else {
            data = bankTransactionService.listAll();
        }
        return ResponseEntity.ok(Map.of("data", data));
    }

    /**
     * 单条匹配生成凭证
     */
    @PostMapping("/api/bank-transaction/{id}/match")
    @Audit(action = "BANK_MATCH")
    public ResponseEntity<?> match(@PathVariable Long id) {
        try {
            Voucher voucher = bankTransactionService.matchAndGenerate(id);
            return ResponseEntity.ok(Map.of("message", "匹配成功", "data", voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 批量匹配生成凭证
     */
    @PostMapping("/api/bank-transaction/batch-match")
    @Audit(action = "BANK_BATCH_MATCH", recordTargetId = false)
    public ResponseEntity<?> batchMatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择要匹配的流水"));
        }
        List<Voucher> vouchers = bankTransactionService.batchGenerate(ids);
        return ResponseEntity.ok(Map.of("message", "批量匹配完成", "count", vouchers.size(), "data", vouchers));
    }

    /**
     * 忽略流水
     */
    @PostMapping("/api/bank-transaction/{id}/ignore")
    public ResponseEntity<?> ignore(@PathVariable Long id) {
        try {
            bankTransactionService.ignore(id);
            return ResponseEntity.ok(Map.of("message", "已忽略"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除流水
     */
    @DeleteMapping("/api/bank-transaction/{id}")
    @Audit(action = "BANK_DELETE")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            bankTransactionService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 导出银行流水
     */
    @GetMapping("/api/bank-transaction/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String status) {
        try {
            List<BankTransaction> data;
            if (status != null && !status.isEmpty()) {
                data = bankTransactionService.listByStatus(status);
            } else {
                data = bankTransactionService.listAll();
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (BankTransaction t : data) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("交易日期", t.getTransactionDate());
                row.put("对方", t.getCounterparty());
                row.put("摘要", t.getSummary());
                row.put("收入", t.getCreditAmount());
                row.put("支出", t.getDebitAmount());
                row.put("余额", t.getBalance());
                row.put("状态", t.getStatus());
                rows.add(row);
            }

            byte[] excelData = ExcelExportUtil.exportTransactions(rows,
                    new String[]{"交易日期", "对方", "摘要", "收入", "支出", "余额", "状态"},
                    "银行流水");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "银行流水_" + (status != null && !status.isEmpty() ? status : "全部") + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
