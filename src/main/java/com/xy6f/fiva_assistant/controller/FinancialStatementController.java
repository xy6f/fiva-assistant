package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.service.FinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/financial-statement")
@RequiredArgsConstructor
public class FinancialStatementController {

    private final FinancialStatementService financialStatementService;

    /**
     * 资产负债表
     */
    @GetMapping("/balance-sheet")
    public ResponseEntity<?> balanceSheet(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", financialStatementService.getBalanceSheet(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 利润表
     */
    @GetMapping("/income-statement")
    public ResponseEntity<?> incomeStatement(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", financialStatementService.getIncomeStatement(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 现金流量表
     */
    @GetMapping("/cash-flow")
    public ResponseEntity<?> cashFlow(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", financialStatementService.getCashFlowStatement(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
