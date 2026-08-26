package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    private Long id;
    private Long userId;
    private String type;

    @Builder.Default
    private String level = "NORMAL";

    private String title;
    private String content;

    @Builder.Default
    private String status = "UNREAD";

    private Long relatedId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public String getLevelText() {
        return switch (level) {
            case "URGENT" -> "紧急";
            case "IMPORTANT" -> "重要";
            default -> "一般";
        };
    }

    public String getTypeText() {
        return switch (type) {
            case "BUDGET_OVER" -> "预算超支";
            case "BUDGET_WARNING" -> "预算预警";
            case "TAX_DUE" -> "税务申报";
            default -> "系统通知";
        };
    }
}
