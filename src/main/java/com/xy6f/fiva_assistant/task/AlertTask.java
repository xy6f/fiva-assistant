package com.xy6f.fiva_assistant.task;

import com.xy6f.fiva_assistant.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertTask {

    private final AlertService alertService;

    /**
     * 每天早上9点检查预算预警
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkBudgetAlerts() {
        log.info("Running scheduled budget alert check...");
        try {
            alertService.checkBudgetAlerts();
        } catch (Exception e) {
            log.error("Budget alert check failed: {}", e.getMessage());
        }
    }

    /**
     * 每天早上9点检查税务申报提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkTaxDueAlert() {
        log.info("Running scheduled tax due alert check...");
        try {
            alertService.checkTaxDueAlert();
        } catch (Exception e) {
            log.error("Tax due alert check failed: {}", e.getMessage());
        }
    }
}
