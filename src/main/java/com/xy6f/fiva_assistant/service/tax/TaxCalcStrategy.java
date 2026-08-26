package com.xy6f.fiva_assistant.service.tax;

import com.xy6f.fiva_assistant.dto.TaxCalculation;

/**
 * 税费计算策略接口。实现类按 @Order 顺序依次作用于计算结果。
 */
public interface TaxCalcStrategy {

    /** 是否处理该计算（如增值税区分一般/小规模纳税人） */
    boolean supports(TaxCalculation input);

    /** 执行计算并写回 input */
    void apply(TaxCalculation input);
}
