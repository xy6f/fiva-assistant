package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * 明细账
     */
    @GetMapping("/detail")
    public ResponseEntity<?> detail(
            @RequestParam Long accountId,
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getDetailLedger(accountId, period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 总账
     */
    @GetMapping("/general")
    public ResponseEntity<?> general(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getGeneralLedger(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 科目余额表
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<?> trialBalance(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getTrialBalance(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
