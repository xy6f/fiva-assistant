package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import com.xy6f.fiva_assistant.service.tax.TaxCalcStrategy;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaxService {

    @Qualifier("chatModel")
    private final OpenAiChatModel chatModel;
    @Qualifier("streamingChatModel")
    private final OpenAiStreamingChatModel streamingChatModel;
    private final List<TaxCalcStrategy> taxStrategies;
    private final com.xy6f.fiva_assistant.service.AiCallLogService aiCallLogService;

    /**
     * 计算企业税费（由策略链依次处理，参数来自财税规则表）
     */
    public TaxCalculation calculate(TaxCalculation input) {
        for (TaxCalcStrategy strategy : taxStrategies) {
            if (strategy.supports(input)) {
                strategy.apply(input);
            }
        }

        BigDecimal revenue = input.getAnnualRevenue() != null ? input.getAnnualRevenue() : BigDecimal.ZERO;

        // === 税负分析 ===
        BigDecimal vatPayable = nvl(input.getVatPayable());
        BigDecimal urbanTax = nvl(input.getUrbanMaintenanceTax());
        BigDecimal eduSurcharge = nvl(input.getEducationSurcharge());
        BigDecimal localEduSurcharge = nvl(input.getLocalEducationSurcharge());
        BigDecimal incomeTax = nvl(input.getIncomeTaxPayable());

        BigDecimal totalTax = vatPayable.add(urbanTax).add(eduSurcharge).add(localEduSurcharge).add(incomeTax);
        input.setTotalTaxBurden(totalTax);

        BigDecimal taxBurdenRate = revenue.compareTo(BigDecimal.ZERO) > 0
                ? totalTax.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        input.setTaxBurdenRate(taxBurdenRate.setScale(2, RoundingMode.HALF_UP));

        BigDecimal profitBeforeTax = input.getTaxableIncome() != null ? input.getTaxableIncome() : BigDecimal.ZERO;
        BigDecimal netProfit = profitBeforeTax.subtract(incomeTax);
        input.setNetProfit(netProfit);

        BigDecimal profitMargin = revenue.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        input.setProfitMargin(profitMargin.setScale(2, RoundingMode.HALF_UP));

        return input;
    }

    private static final String SUGGESTION_PROMPT = """
            你是一个专业的税务筹划顾问。请根据以下企业税务数据，给出3-5条合理的税务筹划建议。

            企业数据：
            - 年营业收入：%.2f 元
            - 年营业成本：%.2f 元
            - 年期间费用：%.2f 元
            - 纳税人类型：%s
            - 应纳增值税：%.2f 元
            - 应纳企业所得税：%.2f 元
            - 综合税负率：%.2f%%
            - 净利润率：%.2f%%

            请给出具体、可操作的税务筹划建议，包括：
            1. 可以享受的税收优惠政策
            2. 合理的成本费用规划建议
            3. 发票管理优化建议
            4. 其他合法节税方案

            用简洁的中文回答，每条建议不超过100字。
            """;

    /**
     * AI 税务筹划建议
     */
    public String getAiSuggestion(TaxCalculation calc) {
        String prompt = String.format(SUGGESTION_PROMPT,
                calc.getAnnualRevenue(), calc.getAnnualCost(), calc.getAnnualExpenses(),
                Boolean.TRUE.equals(calc.getSmallScale()) ? "小规模纳税人" : "一般纳税人",
                calc.getVatPayable(), calc.getIncomeTaxPayable(),
                calc.getTaxBurdenRate(), calc.getProfitMargin());

        try {
            UserMessage userMessage = UserMessage.from(TextContent.from(prompt));
            String response = chatModel.chat(userMessage).aiMessage().text();
            log.info("Tax AI suggestion: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to get AI suggestion", e);
            return "AI建议获取失败，请稍后重试。";
        }
    }

    /**
     * AI 税务筹划建议（流式输出）
     */
    public void getAiSuggestionStream(TaxCalculation calc, SseEmitter emitter) {
        String prompt = String.format(SUGGESTION_PROMPT,
                calc.getAnnualRevenue(), calc.getAnnualCost(), calc.getAnnualExpenses(),
                Boolean.TRUE.equals(calc.getSmallScale()) ? "小规模纳税人" : "一般纳税人",
                calc.getVatPayable(), calc.getIncomeTaxPayable(),
                calc.getTaxBurdenRate(), calc.getProfitMargin());

        streamToEmitter(prompt, emitter, "Failed to start AI suggestion stream", "Tax AI stream suggestion");
    }

    /**
     * AI 税务深度分析报告（流式输出）
     * 包含行业对比、趋势分析、风险预警等维度
     */
    public void getTaxAnalysisStream(TaxCalculation calc, SseEmitter emitter) {
        String prompt = String.format("""
                你是一个资深税务筹划专家，请根据以下企业税务数据，生成一份完整的税务深度分析报告。

                企业数据：
                - 年营业收入：%.2f 元
                - 年营业成本：%.2f 元
                - 年期间费用：%.2f 元
                - 员工人数：%d 人
                - 纳税人类型：%s
                - 应纳增值税：%.2f 元
                - 应纳企业所得税：%.2f 元
                - 优惠减免金额：%.2f 元
                - 综合税负率：%.2f%%
                - 净利润：%.2f 元
                - 净利润率：%.2f%%

                请从以下维度进行分析，用专业、清晰的中文回答：

                ## 一、税负健康度评估
                评估企业当前税负水平是否合理，与同行业平均水平对比分析。

                ## 二、税收优惠政策利用分析
                分析企业是否充分利用了现有的税收优惠政策，还有哪些可以享受但尚未利用的。

                ## 三、税务风险预警
                识别潜在的税务风险点，如税负率异常、成本费用比例不合理等。

                ## 四、税务筹划优化方案
                给出3-5条具体、可操作的税务优化建议，每条建议需包含：
                - 具体措施
                - 预计节税金额或比例
                - 实施难度和注意事项

                ## 五、年度税务规划建议
                从全年角度给出税务规划的整体思路和时间节点建议。

                注意：
                - 所有分析需基于真实税法规定，以最新政策为准则
                - 建议要具体可操作，不要泛泛而谈
                - 涉及金额的地方请给出估算
                """,
                calc.getAnnualRevenue(), calc.getAnnualCost(), calc.getAnnualExpenses(),
                calc.getEmployeeCount(),
                Boolean.TRUE.equals(calc.getSmallScale()) ? "小规模纳税人" : "一般纳税人",
                calc.getVatPayable(), calc.getIncomeTaxPayable(),
                calc.getIncomeTaxDiscount(), calc.getTaxBurdenRate(),
                calc.getNetProfit(), calc.getProfitMargin());

        streamToEmitter(prompt, emitter, "Failed to start tax analysis stream", "Tax AI analysis completed");
    }

    /**
     * 流式调用 AI 并通过 SSE 推送
     */
    private void streamToEmitter(String prompt, SseEmitter emitter, String startErrorMsg, String doneLogMsg) {
        final Long userId = com.xy6f.fiva_assistant.util.UserContext.getUserId();
        final String callType = doneLogMsg.contains("analysis")
                ? com.xy6f.fiva_assistant.service.AiCallLogService.TYPE_TAX_ANALYSIS
                : com.xy6f.fiva_assistant.service.AiCallLogService.TYPE_TAX_SUGGEST;
        long startMs = System.currentTimeMillis();
        try {
            UserMessage userMessage = UserMessage.from(TextContent.from(prompt));
            StringBuilder fullResponse = new StringBuilder();

            streamingChatModel.chat(List.of(userMessage), new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    fullResponse.append(partialResponse);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(partialResponse));
                    } catch (Exception e) {
                        log.error("Failed to send token", e);
                    }
                }

                @Override
                public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                    log.info("{}, length: {}", doneLogMsg, fullResponse.length());
                    Integer[] usage = {null, null};
                    try {
                        if (response != null && response.metadata() != null && response.metadata().tokenUsage() != null) {
                            usage[0] = response.metadata().tokenUsage().inputTokenCount();
                            usage[1] = response.metadata().tokenUsage().outputTokenCount();
                        }
                    } catch (Exception ignored) {
                    }
                    aiCallLogService.record(userId, callType, usage[0], usage[1],
                            System.currentTimeMillis() - startMs, true, null);
                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Tax AI stream error", error);
                    aiCallLogService.record(userId, callType, null, null,
                            System.currentTimeMillis() - startMs, false, error.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error(startErrorMsg, e);
            aiCallLogService.record(userId, callType, null, null,
                    System.currentTimeMillis() - startMs, false, e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.completeWithError(e);
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
