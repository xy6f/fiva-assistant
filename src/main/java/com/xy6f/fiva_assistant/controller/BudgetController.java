package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.Budget;
import com.xy6f.fiva_assistant.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(Map.of("success", true, "data", budgetService.listByYearMonth(year, month)));
    }

    @GetMapping("/departments")
    public ResponseEntity<?> getDepartments() {
        return ResponseEntity.ok(Map.of("success", true, "data", budgetService.getDepartments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", budgetService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Budget budget) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", budgetService.create(budget)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Budget budget) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", budgetService.update(id, budget)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/used")
    public ResponseEntity<?> updateUsedAmount(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            java.math.BigDecimal usedAmount = new java.math.BigDecimal(body.get("usedAmount").toString());
            return ResponseEntity.ok(Map.of("success", true, "data", budgetService.updateUsedAmount(id, usedAmount)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            budgetService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/over-budget")
    public ResponseEntity<?> getOverBudget(@RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(Map.of("success", true, "data", budgetService.getOverBudget(year, month)));
    }
}
