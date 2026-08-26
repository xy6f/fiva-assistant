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
        return existing;
    }

    public Budget create(Budget budget) {
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setUserId(UserContext.getUserId());
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
        budgetMapper.deleteById(id);
    }

    public List<Budget> getOverBudget(Integer year, Integer month) {
        return budgetMapper.findOverBudget(year, month, UserContext.getUserId());
    }
}
