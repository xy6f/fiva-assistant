package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Expense;
import com.xy6f.fiva_assistant.mapper.ExpenseMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseMapper expenseMapper;

    public List<Expense> listAll() {
        return expenseMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public List<Expense> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listAll();
        }
        return expenseMapper.searchByKeyword(UserContext.getUserId(), keyword);
    }

    public Expense getById(Long id) {
        Expense existing = expenseMapper.findById(id);
        if (existing == null) throw new RuntimeException("报销单不存在: " + id);
        if (!existing.getUserId().equals(UserContext.getUserId())) {
            throw new RuntimeException("无权访问该报销单");
        }
        return existing;
    }

    public Expense create(Expense expense) {
        if (expense.getTitle() == null || expense.getTitle().isBlank()) {
            throw new RuntimeException("报销标题不能为空");
        }
        if (expense.getAmount() == null || expense.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("报销金额必须大于零");
        }
        expense.setStatus("PENDING");
        expense.setUserId(UserContext.getUserId());
        log.info("Creating expense: {}", expense.getTitle());
        expenseMapper.insert(expense);
        return expense;
    }

    public Expense update(Long id, Expense update) {
        Expense existing = getById(id);
        if (!"PENDING".equals(existing.getStatus())) {
            throw new RuntimeException("只能修改待审批的报销单");
        }
        existing.setTitle(update.getTitle());
        existing.setApplicant(update.getApplicant());
        existing.setCategory(update.getCategory());
        existing.setAmount(update.getAmount());
        existing.setExpenseDate(update.getExpenseDate());
        existing.setDescription(update.getDescription());
        expenseMapper.update(existing);
        return existing;
    }

    public Expense approve(Long id, String opinion, boolean approved) {
        Expense expense = getById(id);
        if (!"PENDING".equals(expense.getStatus())) {
            throw new RuntimeException("该报销单已审批");
        }
        String approver = String.valueOf(UserContext.getUserId());
        expense.setApprover(approver);
        expense.setApprovalOpinion(opinion);
        expense.setStatus(approved ? "APPROVED" : "REJECTED");
        expense.setApprovedAt(LocalDateTime.now());
        log.info("Expense {} {} by {}", id, approved ? "approved" : "rejected", approver);
        expenseMapper.update(expense);
        return expense;
    }

    public void delete(Long id) {
        Expense expense = getById(id);
        if (!"PENDING".equals(expense.getStatus())) {
            throw new RuntimeException("只能删除待审批的报销单");
        }
        expenseMapper.deleteById(id);
    }

    public Map<String, Object> getStatistics() {
        Long userId = UserContext.getUserId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", expenseMapper.countByStatusAndUserId("PENDING", userId));
        stats.put("approvedCount", expenseMapper.countByStatusAndUserId("APPROVED", userId));
        stats.put("rejectedCount", expenseMapper.countByStatusAndUserId("REJECTED", userId));
        stats.put("categoryStats", expenseMapper.categoryStats(userId));
        stats.put("totalApproved", expenseMapper.sumApprovedAmountSince(LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0), userId));
        return stats;
    }
}
