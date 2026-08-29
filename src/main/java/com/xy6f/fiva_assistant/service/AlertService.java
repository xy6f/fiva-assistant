package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Alert;
import com.xy6f.fiva_assistant.model.Budget;
import com.xy6f.fiva_assistant.model.User;
import com.xy6f.fiva_assistant.mapper.AlertMapper;
import com.xy6f.fiva_assistant.mapper.BudgetMapper;
import com.xy6f.fiva_assistant.mapper.UserMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final BudgetMapper budgetMapper;
    private final UserMapper userMapper;

    private final AtomicBoolean budgetCheckRunning = new AtomicBoolean(false);
    private final AtomicBoolean taxCheckRunning = new AtomicBoolean(false);

    public List<Alert> listAll() {
        return alertMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public List<Alert> listUnread() {
        return alertMapper.findByUserIdAndStatusOrderByCreatedAtDesc(UserContext.getUserId(), "UNREAD");
    }

    public long getUnreadCount() {
        return alertMapper.countByStatusAndUserId("UNREAD", UserContext.getUserId());
    }

    public void markAsRead(Long id) {
        Long userId = UserContext.getUserId();
        Alert alert = alertMapper.findById(id);
        if (alert == null || !userId.equals(alert.getUserId())) throw new RuntimeException("预警不存在或无权访问");
        alert.setStatus("READ");
        alert.setReadAt(LocalDateTime.now());
        alertMapper.update(alert);
    }

    public void markAllAsRead() {
        List<Alert> unread = alertMapper.findByUserIdAndStatusOrderByCreatedAtDesc(UserContext.getUserId(), "UNREAD");
        for (Alert alert : unread) {
            alert.setStatus("READ");
            alert.setReadAt(LocalDateTime.now());
            alertMapper.update(alert);
        }
    }

    public void markAsHandled(Long id) {
        Long userId = UserContext.getUserId();
        Alert alert = alertMapper.findById(id);
        if (alert == null || !userId.equals(alert.getUserId())) throw new RuntimeException("预警不存在或无权访问");
        alert.setStatus("HANDLED");
        alert.setReadAt(LocalDateTime.now());
        alertMapper.update(alert);
    }

    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Alert alert = alertMapper.findById(id);
        if (alert == null || !userId.equals(alert.getUserId())) throw new RuntimeException("预警不存在或无权访问");
        alertMapper.deleteById(id);
    }

    public Alert create(String type, String level, String title, String content, Long relatedId) {
        Alert alert = Alert.builder()
                .type(type)
                .level(level)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .userId(UserContext.getUserId())
                .build();
        alertMapper.insert(alert);
        return alert;
    }

    /**
     * 检查预算预警（支持定时任务调用，userId为null时遍历所有用户）
     */
    public void checkBudgetAlerts() {
        if (!budgetCheckRunning.compareAndSet(false, true)) {
            log.debug("Budget check already running, skipping");
            return;
        }
        try {
            YearMonth current = YearMonth.now();
            Long userId = UserContext.getUserId();

            if (userId == null) {
                // 定时任务场景：分页遍历所有用户，避免一次性加载全部
                int page = 0;
                int pageSize = 100;
                while (true) {
                    List<User> users = userMapper.findPage(page, pageSize);
                    if (users.isEmpty()) break;
                    for (User user : users) {
                        checkBudgetAlertsForUser(user.getId(), current);
                    }
                    if (users.size() < pageSize) break;
                    page++;
                }
            } else {
                checkBudgetAlertsForUser(userId, current);
            }
        } finally {
            budgetCheckRunning.set(false);
        }
    }

    private void checkBudgetAlertsForUser(Long userId, YearMonth current) {
        List<Budget> budgets = budgetMapper.findByUserIdAndYearAndMonthOrderByCreatedAtDesc(
                userId, current.getYear(), current.getMonthValue());
        
        for (Budget budget : budgets) {
            if (budget.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal usedAmount = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
            BigDecimal usagePercent = usedAmount
                    .multiply(new BigDecimal("100"))
                    .divide(budget.getBudgetAmount(), 2, java.math.RoundingMode.HALF_UP);
            
            int threshold = budget.getAlertThreshold() != null ? budget.getAlertThreshold() : 80;
            
            // 超预算
            if (usedAmount.compareTo(budget.getBudgetAmount()) > 0) {
                // 去重：同用户同预算同类型已有未读告警则跳过
                List<Alert> existing = alertMapper.findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, "BUDGET_OVER", "UNREAD");
                boolean dup = false;
                for (Alert a : existing) {
                    if (a.getRelatedId() != null && a.getRelatedId().equals(budget.getId())) { dup = true; break; }
                }
                if (dup) continue;
                String title = String.format("【%s】%d年%d月预算已超支", 
                        budget.getDepartment(), budget.getYear(), budget.getMonth());
                String content = String.format("预算金额: ¥%.2f, 已使用: ¥%.2f, 超支: ¥%.2f",
                        budget.getBudgetAmount(), usedAmount, 
                        usedAmount.subtract(budget.getBudgetAmount()));
                createForUser(userId, "BUDGET_OVER", "URGENT", title, content, budget.getId());
                log.info("Budget over alert created for: {}", budget.getDepartment());
            }
            // 达到预警阈值
            else if (usagePercent.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
                List<Alert> existing = alertMapper.findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, "BUDGET_WARNING", "UNREAD");
                boolean dup = false;
                for (Alert a : existing) {
                    if (a.getRelatedId() != null && a.getRelatedId().equals(budget.getId())) { dup = true; break; }
                }
                if (dup) continue;
                String title = String.format("【%s】%d年%d月预算使用率已达%d%%", 
                        budget.getDepartment(), budget.getYear(), budget.getMonth(),
                        usagePercent.intValue());
                String content = String.format("预算金额: ¥%.2f, 已使用: ¥%.2f, 使用率: %.1f%%",
                        budget.getBudgetAmount(), usedAmount, usagePercent);
                createForUser(userId, "BUDGET_WARNING", "IMPORTANT", title, content, budget.getId());
                log.info("Budget warning alert created for: {}", budget.getDepartment());
            }
        }
    }

    /**
     * 检查税务申报提醒（每月1-15日）
     */
    public void checkTaxDueAlert() {
        if (!taxCheckRunning.compareAndSet(false, true)) {
            log.debug("Tax check already running, skipping");
            return;
        }
        try {
            LocalDate today = LocalDate.now();
            if (today.getDayOfMonth() <= 15) {
                Long userId = UserContext.getUserId();
                String title = "税务申报提醒";
                String content = String.format("本月税务申报截止日期为%d日，请及时完成申报。", 
                        today.withDayOfMonth(today.lengthOfMonth()).getDayOfMonth());

                if (userId == null) {
                    // 定时任务场景：分页遍历所有用户
                    int page = 0;
                    int pageSize = 100;
                    while (true) {
                        List<User> users = userMapper.findPage(page, pageSize);
                        if (users.isEmpty()) break;
                        for (User user : users) {
                            createTaxAlertForUser(user.getId(), title, content);
                        }
                        if (users.size() < pageSize) break;
                        page++;
                    }
                } else {
                    createTaxAlertForUser(userId, title, content);
                }
            }
        } finally {
            taxCheckRunning.set(false);
        }
    }

    private void createTaxAlertForUser(Long userId, String title, String content) {
        List<Alert> existing = alertMapper.findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, "TAX_DUE", "UNREAD");
        if (existing.isEmpty()) {
            createForUser(userId, "TAX_DUE", "IMPORTANT", title, content, null);
            log.info("Tax due alert created for user: {}", userId);
        }
    }

    private void createForUser(Long userId, String type, String level, String title, String content, Long relatedId) {
        Alert alert = Alert.builder()
                .type(type)
                .level(level)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .userId(userId)
                .build();
        alertMapper.insert(alert);
    }
}
