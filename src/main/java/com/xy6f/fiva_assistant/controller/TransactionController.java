package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.dto.TransactionSummary;
import com.xy6f.fiva_assistant.model.Transaction;
import com.xy6f.fiva_assistant.service.TransactionService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Transaction transaction) {
        try {
            Transaction saved = transactionService.save(transaction);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Transaction transaction) {
        try {
            Transaction updated = transactionService.update(id, transaction);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Transaction>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(transactionService.listByType(type));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(transactionService.listByDateRange(
                    LocalDate.parse(startDate), LocalDate.parse(endDate)));
        }
        return ResponseEntity.ok(transactionService.listAll());
    }

    @GetMapping("/summary")
    public ResponseEntity<TransactionSummary> summary() {
        return ResponseEntity.ok(transactionService.getSummary());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<Transaction> transactions;
            if (type != null && !type.isBlank()) {
                transactions = transactionService.listByType(type);
            } else if (startDate != null && endDate != null) {
                transactions = transactionService.listByDateRange(LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else {
                transactions = transactionService.listAll();
            }
            
            List<Map<String, Object>> data = new ArrayList<>();
            for (Transaction t : transactions) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("类型", t.getType());
                row.put("金额", t.getAmount());
                row.put("类别", t.getCategory());
                row.put("描述", t.getDescription());
                row.put("日期", t.getTransactionDate());
                row.put("往来方", t.getCounterparty());
                data.add(row);
            }
            
            byte[] excelData = ExcelExportUtil.exportTransactions(data,
                new String[]{"类型", "金额", "类别", "描述", "日期", "往来方"}, "收支记账");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "收支记账列表.xlsx");
            
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
