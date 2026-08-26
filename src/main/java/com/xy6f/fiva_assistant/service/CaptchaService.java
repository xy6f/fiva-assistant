package com.xy6f.fiva_assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单数学验证码服务
 */
@Service
@Slf4j
public class CaptchaService {

    // captchaId -> CaptchaEntry(答案+过期时间)
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final long EXPIRE_MS = 5 * 60 * 1000; // 5分钟过期

    public record CaptchaEntry(String answer, long expireAt) {}

    public record CaptchaResult(String captchaId, String question) {}

    /**
     * 生成数学验证码
     */
    public CaptchaResult generate() {
        int a = random.nextInt(1, 20);
        int b = random.nextInt(1, 20);
        int op = random.nextInt(0, 2); // 0=加法, 1=乘法
        String question;
        int answer;
        if (op == 0) {
            question = a + " + " + b + " = ?";
            answer = a + b;
        } else {
            // 用小数避免大数
            int x = random.nextInt(2, 10);
            int y = random.nextInt(2, 10);
            question = x + " × " + y + " = ?";
            answer = x * y;
        }

        String captchaId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        captchaStore.put(captchaId, new CaptchaEntry(String.valueOf(answer), System.currentTimeMillis() + EXPIRE_MS));

        // 清理过期验证码
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(e -> e.getValue().expireAt() < now);

        return new CaptchaResult(captchaId, question);
    }

    /**
     * 校验验证码
     */
    public boolean verify(String captchaId, String answer) {
        if (captchaId == null || answer == null) return false;
        CaptchaEntry entry = captchaStore.remove(captchaId);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt()) return false;
        return entry.answer().equals(answer.trim());
    }
}
