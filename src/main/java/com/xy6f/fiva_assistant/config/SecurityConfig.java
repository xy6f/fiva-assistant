package com.xy6f.fiva_assistant.config;

import com.xy6f.fiva_assistant.util.JwtUtil;
import com.xy6f.fiva_assistant.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] WRITE_ROLES = {"ADMIN", "MANAGER", "ACCOUNTANT"};

    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录和注册接口不需要认证
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/captcha").permitAll()
                // 用户管理仅 ADMIN
                .requestMatchers("/api/users/**", "/api/auth/users/**").hasRole("ADMIN")
                // 公司设置：ADMIN / MANAGER
                .requestMatchers("/api/auth/company").hasAnyRole("ADMIN", "MANAGER")
                // 工资属高度敏感数据：仅 ADMIN / MANAGER，VIEWER 与 ACCOUNTANT 均不可访问
                .requestMatchers("/api/salary/**").hasAnyRole("ADMIN", "MANAGER")
                // AI 问答对所有登录角色开放（含 VIEWER）
                .requestMatchers("/api/chat/**").authenticated()
                // 税务测算为纯计算/AI 咨询，不改动数据，VIEWER 可用
                .requestMatchers("/api/tax/calculate", "/api/tax/suggestion",
                        "/api/tax/suggestion/stream", "/api/tax/analysis").authenticated()
                // 费用、预算、银行流水、固定资产为敏感业务数据，VIEWER 不可读
                .requestMatchers(HttpMethod.GET, "/api/expense/**", "/api/budget/**",
                        "/api/bank-transaction/**", "/api/fixed-asset/**")
                        .hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                // 其余写操作（POST/PUT/DELETE）需要业务角色，VIEWER 只读
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole(WRITE_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole(WRITE_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole(WRITE_ROLES)
                // 其他所有 API 需要认证
                .requestMatchers("/api/**").authenticated()
                // 前端页面不需要 Spring Security 认证（由前端路由守卫处理）
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                           FilterChain filterChain) throws ServletException, IOException {
                String header = request.getHeader("Authorization");
                
                if (header != null && header.startsWith("Bearer ")) {
                    String token = header.substring(7);
                    if (jwtUtil.validateToken(token)) {
                        String username = jwtUtil.getUsernameFromToken(token);
                        String role = jwtUtil.getRoleFromToken(token);
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        
                        // 设置用户上下文（数据隔离）
                        if (userId != null) {
                            UserContext.setUserId(userId);
                        }
                        
                        // 创建认证信息
                        org.springframework.security.core.authority.SimpleGrantedAuthority authority = 
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role);
                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                username, null, List.of(authority)));
                    }
                }
                
                filterChain.doFilter(request, response);
                // 清理用户上下文
                UserContext.clear();
            }
        };
    }
}
