package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import com.xy6f.fiva_assistant.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 税务测算接口。
 * AI 税负建议/深度分析（/suggestion*、/analysis）已迁移至 Python LangChain 服务（ai-agent，端口 8001）。
 */
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
}
