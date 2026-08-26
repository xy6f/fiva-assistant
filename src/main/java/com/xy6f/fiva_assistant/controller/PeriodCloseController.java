package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.service.PeriodCloseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/period-close")
@RequiredArgsConstructor
public class PeriodCloseController {

    private final PeriodCloseService periodCloseService;

    @GetMapping("/status")
    public ResponseEntity<?> status(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", periodCloseService.getCloseStatus(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/close")
    public ResponseEntity<?> close(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", periodCloseService.close(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reopen")
    public ResponseEntity<?> reopen(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(periodCloseService.reopen(period));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
