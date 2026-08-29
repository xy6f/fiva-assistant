package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.SalaryRecord;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.service.SalaryService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    /**
     * 导出工资表
     */
    @GetMapping("/api/salary/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam String month) {
        try {
            List<SalaryRecord> records = salaryService.listByMonth(month);

            List<Map<String, Object>> rows = new ArrayList<>();
            for (SalaryRecord r : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("工号", r.getEmployeeNo());
                row.put("姓名", r.getEmployeeName());
                row.put("部门", r.getDepartment());
                row.put("职位", r.getPosition());
                row.put("基本工资", r.getBaseSalary());
                row.put("奖金", r.getBonus());
                row.put("津贴", r.getAllowance());
                row.put("加班费", r.getOvertime());
                row.put("应发", r.getGrossPay());
                row.put("社保", r.getSocialInsurance());
                row.put("公积金", r.getHousingFund());
                row.put("个税", r.getTax());
                row.put("其他扣款", r.getOtherDeduction());
                row.put("实发", r.getNetPay());
                rows.add(row);
            }

            byte[] excelData = ExcelExportUtil.exportTransactions(rows,
                    new String[]{"工号", "姓名", "部门", "职位", "基本工资", "奖金", "津贴", "加班费", "应发", "社保", "公积金", "个税", "其他扣款", "实发"},
                    "工资表_" + month);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "工资表_" + month + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
