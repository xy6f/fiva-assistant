package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.dto.LoginRequest;
import com.xy6f.fiva_assistant.dto.LoginResponse;
import com.xy6f.fiva_assistant.dto.RegisterRequest;
import com.xy6f.fiva_assistant.model.Company;
import com.xy6f.fiva_assistant.model.User;
import com.xy6f.fiva_assistant.mapper.CompanyMapper;
import com.xy6f.fiva_assistant.service.CaptchaService;
import com.xy6f.fiva_assistant.service.UserService;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;
    private final CompanyMapper companyMapper;
    private final com.xy6f.fiva_assistant.util.JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            LoginResponse response = userService.register(request);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generate();
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                "captchaId", result.captchaId(),
                "question", result.question()
        )));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.listAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Map<String, Object> userInfo = Map.of(
                "username", user.getUsername(),
                "realName", user.getRealName() != null ? user.getRealName() : "",
                "role", user.getRole().name(),
                "department", user.getDepartment() != null ? user.getDepartment() : "",
                "userId", user.getId()
            );
            return ResponseEntity.ok(Map.of("success", true, "data", userInfo));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "登录已过期"));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<User> users = userService.listAll();
        // 隐藏密码
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(Map.of("success", true, "data", users));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User created = userService.create(user);
            created.setPassword(null);
            return ResponseEntity.ok(Map.of("success", true, "data", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updated = userService.update(id, user);
            updated.setPassword(null);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable Long id) {
        try {
            userService.toggleEnabled(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.getOrDefault("newPassword", "123456");
            userService.resetPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/company")
    public ResponseEntity<?> getCompany() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        Company company = companyMapper.findByUserId(userId);
        if (company == null) {
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of()));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", company));
    }

    @PutMapping("/company")
    public ResponseEntity<?> updateCompany(@RequestBody Company company) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        Company existing = companyMapper.findByUserId(userId);
        if (existing == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "公司信息不存在"));
        }
        if (company.getName() != null) existing.setName(company.getName());
        if (company.getTaxNo() != null) existing.setTaxNo(company.getTaxNo());
        if (company.getAddress() != null) existing.setAddress(company.getAddress());
        if (company.getPhone() != null) existing.setPhone(company.getPhone());
        if (company.getBankName() != null) existing.setBankName(company.getBankName());
        if (company.getBankAccount() != null) existing.setBankAccount(company.getBankAccount());
        if (company.getLegalPerson() != null) existing.setLegalPerson(company.getLegalPerson());
        companyMapper.update(existing);
        return ResponseEntity.ok(Map.of("success", true, "data", existing));
    }
}
