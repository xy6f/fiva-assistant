package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.annotation.Audit;
import com.xy6f.fiva_assistant.dto.InvoiceOcrResult;
import com.xy6f.fiva_assistant.model.Invoice;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/invoice/upload")
    @Audit(action = "INVOICE_UPLOAD", recordTargetId = false)
    public ResponseEntity<?> uploadInvoice(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请选择文件"));
            }
            InvoiceOcrResult result = invoiceService.recognizeInvoice(file);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "识别失败: " + e.getMessage()));
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> listInvoices() {
        return ResponseEntity.ok(invoiceService.listInvoices());
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @PutMapping("/invoices/{id}")
    @Audit(action = "INVOICE_UPDATE")
    public ResponseEntity<?> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoice) {
        try {
            Invoice updated = invoiceService.updateInvoice(id, invoice);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/invoices/{id}")
    @Audit(action = "INVOICE_DELETE")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/invoices/{id}/create-voucher")
    @Audit(action = "INVOICE_CREATE_VOUCHER")
    public ResponseEntity<?> createVoucherFromInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description) {
        try {
            Voucher voucher = invoiceService.createVoucherFromInvoice(id, type, category, description);
            return ResponseEntity.ok(Map.of("success", true, "data", voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 单张发票查验
     */
    @PostMapping("/invoices/{id}/verify")
    public ResponseEntity<?> verifyInvoice(@PathVariable Long id) {
        try {
            Invoice result = invoiceService.verifyInvoice(id);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "查验失败: " + e.getMessage()));
        }
    }

    /**
     * 批量查验
     */
    @PostMapping("/invoices/batch-verify")
    public ResponseEntity<?> batchVerify() {
        try {
            List<Invoice> results = invoiceService.batchVerify();
            return ResponseEntity.ok(Map.of("success", true, "message", "批量查验完成", "count", results.size(), "data", results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "批量查验失败: " + e.getMessage()));
        }
    }
}
