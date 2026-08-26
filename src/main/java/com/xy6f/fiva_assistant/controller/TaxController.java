package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import com.xy6f.fiva_assistant.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @PostMapping("/calculate")
    public ResponseEntity<?> calculate(@RequestBody TaxCalculation input) {
        try {
            TaxCalculation result = taxService.calculate(input);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/suggestion")
    public ResponseEntity<?> getSuggestion(@RequestBody TaxCalculation calc) {
        try {
            String suggestion = taxService.getAiSuggestion(calc);
            return ResponseEntity.ok(Map.of("success", true, "data", suggestion));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/suggestion/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSuggestion(@RequestBody TaxCalculation calc) {
        SseEmitter emitter = new SseEmitter(120_000L);
        taxService.getAiSuggestionStream(calc, emitter);
        return emitter;
    }

    @PostMapping(value = "/analysis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalysis(@RequestBody TaxCalculation calc) {
        SseEmitter emitter = new SseEmitter(180_000L);
        taxService.getTaxAnalysisStream(calc, emitter);
        return emitter;
    }
}
