package com.xy6f.fiva_assistant.aspect;

import com.xy6f.fiva_assistant.annotation.Audit;
import com.xy6f.fiva_assistant.mapper.UserMapper;
import com.xy6f.fiva_assistant.model.AuditLog;
import com.xy6f.fiva_assistant.mapper.AuditLogMapper;
import com.xy6f.fiva_assistant.model.User;
import com.xy6f.fiva_assistant.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 审计日志切面：拦截 @Audit 标注的方法，成功/失败均落库，且审计失败不影响主流程。
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    @Around("@annotation(com.xy6f.fiva_assistant.annotation.Audit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audit audit = method.getAnnotation(Audit.class);

        Long userId = UserContext.getUserId();
        String targetId = null;
        if (audit.recordTargetId()) {
            targetId = extractTargetId(signature.getParameterNames(), joinPoint.getArgs());
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            record(userId, audit.action(), targetId, "失败: " + ex.getMessage(), false);
            throw ex;
        }
        record(userId, audit.action(), targetId, null, true);
        return result;
    }

    private void record(Long userId, String action, String targetId, String errorDetail, boolean success) {
        try {
            String username = null;
            if (userId != null) {
                try {
                    User u = userMapper.findById(userId);
                    if (u != null) username = u.getUsername();
                } catch (Exception ignored) {
                }
            }
            String detail = buildDetail(errorDetail);
            auditLogMapper.insert(AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .targetId(targetId)
                    .detail(detail)
                    .ip(currentIp())
                    .success(success)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    private String extractTargetId(String[] paramNames, Object[] args) {
        for (int i = 0; i < paramNames.length; i++) {
            if ("id".equalsIgnoreCase(paramNames[i]) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    private String buildDetail(String errorDetail) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String uri = attrs != null ? attrs.getRequest().getRequestURI() : null;
        String detail = uri != null ? "URI: " + uri : "";
        if (errorDetail != null && !errorDetail.isBlank()) {
            detail = detail + (detail.isEmpty() ? "" : ", ") + errorDetail;
        }
        return detail.isEmpty() ? null : truncate(detail, 1000);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
