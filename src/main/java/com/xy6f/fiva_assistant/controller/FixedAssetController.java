package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.FixedAsset;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.service.FixedAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    @GetMapping("/api/fixed-asset")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("data", fixedAssetService.listAll()));
    }

    @GetMapping("/api/fixed-asset/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("data", fixedAssetService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/fixed-asset")
    public ResponseEntity<?> create(@RequestBody FixedAsset asset) {
        try {
            return ResponseEntity.ok(Map.of("data", fixedAssetService.create(asset), "message", "创建成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "创建失败: " + e.getMessage()));
        }
    }

    @PutMapping("/api/fixed-asset/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FixedAsset asset) {
        try {
            return ResponseEntity.ok(Map.of("data", fixedAssetService.update(id, asset), "message", "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/fixed-asset/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            fixedAssetService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 计提折旧 period=yyyy-MM
     */
    @PostMapping("/api/fixed-asset/depreciate")
    public ResponseEntity<?> depreciate(@RequestParam String period) {
        try {
            List<Voucher> vouchers = fixedAssetService.generateDepreciationVoucher(period);
            return ResponseEntity.ok(Map.of("message", "折旧计提成功", "data", vouchers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
