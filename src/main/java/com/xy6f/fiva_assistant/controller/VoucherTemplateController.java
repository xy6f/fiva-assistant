package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.VoucherTemplate;
import com.xy6f.fiva_assistant.service.VoucherTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher-template")
@RequiredArgsConstructor
public class VoucherTemplateController {

    private final VoucherTemplateService templateService;

    /**
     * 获取模板列表
     */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(Map.of("success", true, "data", templateService.listByType(type)));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", templateService.listAll()));
    }

    /**
     * 获取单个模板
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", templateService.getById(id)));
    }

    /**
     * 创建模板
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody VoucherTemplate template) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", templateService.create(template)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VoucherTemplate template) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", templateService.update(id, template)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            templateService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 应用模板生成凭证数据
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> apply(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String voucherDate) {
        try {
            LocalDate date = voucherDate != null ? LocalDate.parse(voucherDate) : LocalDate.now();
            return ResponseEntity.ok(Map.of("success", true, "data", templateService.applyTemplate(id, amount, date)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 初始化默认模板
     */
    @PostMapping("/init")
    public ResponseEntity<?> initDefaults() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", templateService.initDefaultTemplates(
                    com.xy6f.fiva_assistant.util.UserContext.getUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
