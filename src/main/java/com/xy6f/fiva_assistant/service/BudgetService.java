package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Budget;
import com.xy6f.fiva_assistant.mapper.BudgetMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;

    public List<Budget> listAll() {
        return budgetMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public List<Budget> listByYearMonth(Integer year, Integer month) {
        Long userId = UserContext.getUserId();
        if (year != null && month != null) {
            return budgetMapper.findByUserIdAndYearAndMonthOrderByCreatedAtDesc(userId, year, month);
        }
        if (year != null) {
            return budgetMapper.findByUserIdAndYearOrderByCreatedAtDesc(userId, year);
        }
        return listAll();
    }

    public List<String> getDepartments() {
        return budgetMapper.findDistinctDepartments(UserContext.getUserId());
    }

    public Budget getById(Long id) {
        Budget existing = budgetMapper.findById(id);
        if (existing == null) throw new RuntimeException("预算不存在: " + id);
        if (!existing.getUserId().equals(UserContext.getUserId())) {
            throw new RuntimeException("无权访问该预算");
        }
        return existing;
    }

    public Budget create(Budget budget) {
        if (budget.getMonth() == null || budget.getMonth() < 1 || budget.getMonth() > 12) {
            throw new RuntimeException("月份必须在1-12之间");
        }
        if (budget.getYear() == null || budget.getYear() < 2000 || budget.getYear() > 2100) {
            throw new RuntimeException("年份不合法");
        }
        Long userId = UserContext.getUserId();
        List<Budget> existing = budgetMapper.findByUserIdAndYearAndMonthOrderByCreatedAtDesc(
                userId, budget.getYear(), budget.getMonth());
        boolean dup = existing.stream().anyMatch(b ->
                budget.getDepartment() != null && budget.getDepartment().equals(b.getDepartment()));
        if (dup) {
            throw new RuntimeException("该部门" + budget.getYear() + "年" + budget.getMonth() + "月已有预算");
        }
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setUserId(userId);
        log.info("Creating budget: {} {}-{}", budget.getDepartment(), budget.getYear(), budget.getMonth());
        budgetMapper.insert(budget);
        return budget;
    }

    public Budget update(Long id, Budget update) {
        Budget existing = getById(id);
        existing.setDepartment(update.getDepartment());
        existing.setYear(update.getYear());
        existing.setMonth(update.getMonth());
        existing.setCategory(update.getCategory());
        existing.setBudgetAmount(update.getBudgetAmount());
        existing.setAlertThreshold(update.getAlertThreshold());
        existing.setDescription(update.getDescription());
        budgetMapper.update(existing);
        return existing;
    }

    public Budget updateUsedAmount(Long id, BigDecimal usedAmount) {
        Budget existing = getById(id);
        existing.setUsedAmount(usedAmount);
        budgetMapper.update(existing);
        return existing;
    }

    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Budget existing = budgetMapper.findById(id);
        if (existing == null || !userId.equals(existing.getUserId())) throw new RuntimeException("预算不存在或无权访问");
        budgetMapper.deleteById(id);
    }

    public List<Budget> getOverBudget(Integer year, Integer month) {
        return budgetMapper.findOverBudget(year, month, UserContext.getUserId());
    }
}
