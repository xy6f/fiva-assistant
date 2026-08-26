package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.annotation.Audit;
import com.xy6f.fiva_assistant.dto.VoucherSummary;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.service.VoucherService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * 创建凭证（复式记账）
     * 请求体: { voucher: {...}, entries: [...] }
     */
    @PostMapping
    @Audit(action = "VOUCHER_CREATE", recordTargetId = false)
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Voucher voucher = parseVoucher(body);
            List<VoucherEntry> entries = parseEntries(body);
            Voucher saved = voucherService.save(voucher, entries);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Audit(action = "VOUCHER_UPDATE")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Voucher voucher = parseVoucher(body);
            List<VoucherEntry> entries = parseEntries(body);
            Voucher updated = voucherService.update(id, voucher, entries);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Audit(action = "VOUCHER_DELETE")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            voucherService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getById(id));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Voucher>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (status != null && !status.isBlank()) {
                return ResponseEntity.ok(voucherService.listByDateRangeAndStatus(start, end, status));
            }
            return ResponseEntity.ok(voucherService.listByDateRange(start, end));
        }
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(voucherService.listByStatus(status));
        }
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(voucherService.listByType(type));
        }
        return ResponseEntity.ok(voucherService.listAllWithEntries());
    }

    @GetMapping("/summary")
    public ResponseEntity<VoucherSummary> summary() {
        return ResponseEntity.ok(voucherService.getSummary());
    }

    /**
     * 分页查询（统一分页规范：{list,total,page,pageSize}）
     */
    @GetMapping("/page")
    public ResponseEntity<com.xy6f.fiva_assistant.dto.PageResult<Voucher>> page(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;
        return ResponseEntity.ok(voucherService.pageByFilter(type, status, start, end, page, pageSize));
    }

    /** 提交审核 */
    @PostMapping("/{id}/submit")
    @Audit(action = "VOUCHER_SUBMIT")
    public ResponseEntity<?> submitForReview(@PathVariable Long id) {
        try {
            Voucher v = voucherService.submitForReview(id);
            return ResponseEntity.ok(Map.of("success", true, "data", v));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 审核通过 */
    @PostMapping("/{id}/approve")
    @Audit(action = "VOUCHER_APPROVE")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        try {
            Voucher v = voucherService.approve(id);
            return ResponseEntity.ok(Map.of("success", true, "data", v));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 驳回 */
    @PostMapping("/{id}/reject")
    @Audit(action = "VOUCHER_REJECT")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        try {
            Voucher v = voucherService.reject(id);
            return ResponseEntity.ok(Map.of("success", true, "data", v));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<Voucher> vouchers;
            if (type != null && !type.isBlank()) {
                vouchers = voucherService.listByType(type);
            } else if (startDate != null && endDate != null) {
                vouchers = voucherService.listByDateRange(LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else {
                vouchers = voucherService.listAllWithEntries();
            }

            List<Map<String, Object>> data = new ArrayList<>();
            for (Voucher v : vouchers) {
                if (v.getEntries() != null) {
                    for (VoucherEntry e : v.getEntries()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("凭证编号", v.getVoucherNo());
                        row.put("日期", v.getVoucherDate());
                        row.put("摘要", e.getSummary());
                        row.put("科目", e.getAccount() != null ? e.getAccount().getName() : "");
                        row.put("借方金额", e.getDebitAmount());
                        row.put("贷方金额", e.getCreditAmount());
                        row.put("状态", statusText(v.getStatus()));
                        data.add(row);
                    }
                }
            }

            byte[] excelData = ExcelExportUtil.exportTransactions(data,
                    new String[]{"凭证编号", "日期", "摘要", "科目", "借方金额", "贷方金额", "状态"}, "凭证管理");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "凭证列表.xlsx");

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ---- 辅助方法 ----

    @SuppressWarnings("unchecked")
    private Voucher parseVoucher(Map<String, Object> body) {
        Voucher v = new Voucher();
        if (body.containsKey("voucher")) {
            Map<String, Object> vMap = (Map<String, Object>) body.get("voucher");
            v.setType((String) vMap.get("type"));
            v.setAmount(vMap.get("amount") != null ? new java.math.BigDecimal(vMap.get("amount").toString()) : null);
            v.setCategory((String) vMap.get("category"));
            v.setDescription((String) vMap.get("description"));
            v.setVoucherDate(vMap.get("voucherDate") != null ? LocalDate.parse(vMap.get("voucherDate").toString()) : LocalDate.now());
            v.setCounterparty((String) vMap.get("counterparty"));
            v.setHasInvoice(vMap.get("hasInvoice") != null ? Boolean.parseBoolean(vMap.get("hasInvoice").toString()) : false);
            v.setInvoiceImageUrl((String) vMap.get("invoiceImageUrl"));
            v.setStatus(vMap.get("status") != null ? vMap.get("status").toString() : "DRAFT");
        } else {
            // 兼容旧格式：直接传 voucher 字段
            v.setType((String) body.get("type"));
            v.setAmount(body.get("amount") != null ? new java.math.BigDecimal(body.get("amount").toString()) : null);
            v.setCategory((String) body.get("category"));
            v.setDescription((String) body.get("description"));
            v.setVoucherDate(body.get("voucherDate") != null ? LocalDate.parse(body.get("voucherDate").toString()) : LocalDate.now());
            v.setCounterparty((String) body.get("counterparty"));
            v.setHasInvoice(body.get("hasInvoice") != null ? Boolean.parseBoolean(body.get("hasInvoice").toString()) : false);
            v.setInvoiceImageUrl((String) body.get("invoiceImageUrl"));
            v.setStatus(body.get("status") != null ? body.get("status").toString() : "DRAFT");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private List<VoucherEntry> parseEntries(Map<String, Object> body) {
        List<Map<String, Object>> entryList = null;
        if (body.containsKey("entries")) {
            entryList = (List<Map<String, Object>>) body.get("entries");
        }
        if (entryList == null || entryList.isEmpty()) return null;

        List<VoucherEntry> entries = new ArrayList<>();
        for (Map<String, Object> eMap : entryList) {
            VoucherEntry entry = VoucherEntry.builder()
                    .accountId(eMap.get("accountId") != null ? Long.parseLong(eMap.get("accountId").toString()) : null)
                    .summary((String) eMap.get("summary"))
                    .debitAmount(eMap.get("debitAmount") != null ? new java.math.BigDecimal(eMap.get("debitAmount").toString()) : java.math.BigDecimal.ZERO)
                    .creditAmount(eMap.get("creditAmount") != null ? new java.math.BigDecimal(eMap.get("creditAmount").toString()) : java.math.BigDecimal.ZERO)
                    .build();
            entries.add(entry);
        }
        return entries;
    }

    private String statusText(String status) {
        return switch (status) {
            case "REVIEWED" -> "已审核";
            case "POSTED" -> "已过账";
            default -> "草稿";
        };
    }
}
