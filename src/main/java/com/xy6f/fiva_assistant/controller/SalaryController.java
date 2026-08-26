package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.SalaryRecord;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    /**
     * 按月查询
     */
    @GetMapping("/api/salary")
    public ResponseEntity<?> list(@RequestParam String month) {
        return ResponseEntity.ok(Map.of("data", salaryService.listByMonth(month)));
    }

    /**
     * 批量导入工资
     */
    @PostMapping("/api/salary/batch")
    public ResponseEntity<?> batchSave(@RequestBody List<SalaryRecord> records) {
        try {
            List<SalaryRecord> saved = salaryService.batchSave(records);
            return ResponseEntity.ok(Map.of("message", "导入成功", "count", saved.size(), "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * 生成计提凭证
     */
    @PostMapping("/api/salary/generate-voucher")
    public ResponseEntity<?> generateVoucher(@RequestParam String month) {
        try {
            Voucher voucher = salaryService.generateSalaryVoucher(month);
            return ResponseEntity.ok(Map.of("message", "计提凭证生成成功", "data", voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 生成分录凭证（发放凭证）
     */
    @PostMapping("/api/salary/generate-payment")
    public ResponseEntity<?> generatePayment(@RequestParam String month) {
        try {
            Voucher voucher = salaryService.generatePaymentVoucher(month);
            return ResponseEntity.ok(Map.of("message", "发放凭证生成成功", "data", voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除单条
     */
    @DeleteMapping("/api/salary/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            salaryService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
