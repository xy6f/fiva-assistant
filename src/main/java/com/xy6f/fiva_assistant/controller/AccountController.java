package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<Account>> list(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(accountService.listByType(type));
        }
        return ResponseEntity.ok(accountService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Account account) {
        try {
            Account saved = accountService.create(account);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Account account) {
        try {
            Account updated = accountService.update(id, account);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            accountService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 初始化默认科目表（供前端手动触发）
     */
    @PostMapping("/init")
    public ResponseEntity<?> initDefault() {
        try {
            List<Account> accounts = accountService.initDefaultAccounts(
                    com.xy6f.fiva_assistant.util.UserContext.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "data", accounts));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
