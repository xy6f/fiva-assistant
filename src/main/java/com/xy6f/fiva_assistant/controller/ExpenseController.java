package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.Expense;
import com.xy6f.fiva_assistant.service.ExpenseService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(Map.of("success", true, "data", expenseService.search(keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", expenseService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Expense expense) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", expenseService.create(expense)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Expense expense) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", expenseService.update(id, expense)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String approver = (String) body.getOrDefault("approver", "管理员");
            String opinion = (String) body.get("opinion");
            boolean approved = Boolean.TRUE.equals(body.get("approved"));
            return ResponseEntity.ok(Map.of("success", true, "data", expenseService.approve(id, approver, opinion, approved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            expenseService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        return ResponseEntity.ok(Map.of("success", true, "data", expenseService.getStatistics()));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String keyword) {
        try {
            List<Expense> expenses = expenseService.search(keyword);
            List<Map<String, Object>> data = new ArrayList<>();
            for (Expense e : expenses) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("标题", e.getTitle());
                row.put("申请人", e.getApplicant());
                row.put("类别", e.getCategory());
                row.put("金额", e.getAmount());
                row.put("日期", e.getExpenseDate());
                row.put("状态", e.getStatus());
                row.put("审批人", e.getApprover());
                data.add(row);
            }
            
            byte[] excelData = ExcelExportUtil.exportTransactions(data, 
                new String[]{"标题", "申请人", "类别", "金额", "日期", "状态", "审批人"}, "费用报销");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "费用报销列表.xlsx");
            
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
