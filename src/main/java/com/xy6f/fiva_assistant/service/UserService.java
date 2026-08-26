package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.LoginRequest;
import com.xy6f.fiva_assistant.dto.LoginResponse;
import com.xy6f.fiva_assistant.dto.RegisterRequest;
import com.xy6f.fiva_assistant.model.Company;
import com.xy6f.fiva_assistant.model.Role;
import com.xy6f.fiva_assistant.model.User;
import com.xy6f.fiva_assistant.mapper.CompanyMapper;
import com.xy6f.fiva_assistant.mapper.UserMapper;
import com.xy6f.fiva_assistant.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    public LoginResponse login(LoginRequest request) {
        // 校验验证码
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer())) {
            throw new RuntimeException("验证码错误");
        }

        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) throw new RuntimeException("用户名或密码错误");
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.update(user);
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .department(user.getDepartment())
                .build();
    }

    public List<User> listAll() {
        return userMapper.findAll();
    }

    public List<User> listEnabled() {
        return userMapper.findByEnabledTrue();
    }

    public User getById(Long id) {
        User existing = userMapper.findById(id);
        if (existing == null) throw new RuntimeException("用户不存在: " + id);
        return existing;
    }

    public User create(User user) {
        if (userMapper.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.info("Creating user: {} with role: {}", user.getUsername(), user.getRole());
        userMapper.insert(user);
        return user;
    }

    public User update(Long id, User update) {
        User existing = getById(id);
        existing.setRealName(update.getRealName());
        existing.setEmail(update.getEmail());
        existing.setPhone(update.getPhone());
        existing.setDepartment(update.getDepartment());
        existing.setRole(update.getRole());
        existing.setEnabled(update.getEnabled());
        // 如果提供了新密码，则更新
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(update.getPassword()));
        }
        userMapper.update(existing);
        return existing;
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    public void toggleEnabled(Long id) {
        User user = getById(id);
        user.setEnabled(!user.getEnabled());
        userMapper.update(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = getById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    /**
     * 用户注册（含验证码校验 + 自动创建公司）
     */
    public LoginResponse register(RegisterRequest request) {
        // 校验验证码
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer())) {
            throw new RuntimeException("验证码错误");
        }

        // 校验用户名
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (request.getUsername().length() < 3 || request.getUsername().length() > 20) {
            throw new RuntimeException("用户名长度需为3-20个字符");
        }
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("密码长度至少为6个字符");
        }
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            throw new RuntimeException("公司名称不能为空");
        }

        // 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName() != null ? request.getRealName() : request.getUsername())
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userMapper.insert(user);

        // 自动创建公司
        Company company = Company.builder()
                .userId(user.getId())
                .name(request.getCompanyName())
                .build();
        companyMapper.insert(company);

        // 更新用户的公司ID
        user.setCompanyId(company.getId());
        userMapper.update(user);

        log.info("User registered: {} with company: {}", user.getUsername(), request.getCompanyName());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
    }

    /**
     * 初始化默认管理员账号
     */
    public void initDefaultAdmin() {
        if (!userMapper.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .realName("系统管理员")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userMapper.insert(admin);
            log.info("Default admin user created: admin/admin123");
        }
    }
}
