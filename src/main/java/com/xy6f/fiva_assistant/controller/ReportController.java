package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.service.ReportService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import com.xy6f.fiva_assistant.util.PdfExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<?> getReport(@RequestParam(required = false, defaultValue = "ALL") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", reportService.generateReport(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false, defaultValue = "ALL") String period) {
        try {
            Map<String, Object> report = reportService.generateReport(period);
            byte[] excelData = ExcelExportUtil.exportReport(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "财务报表_" + period + ".xlsx");
            
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false, defaultValue = "ALL") String period) {
        try {
            Map<String, Object> report = reportService.generateReport(period);
            byte[] pdfData = PdfExportUtil.exportReport(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "财务报表_" + period + ".pdf");
            
            return ResponseEntity.ok().headers(headers).body(pdfData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
