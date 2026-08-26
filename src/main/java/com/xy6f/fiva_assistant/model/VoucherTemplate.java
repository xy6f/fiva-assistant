package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTemplate {

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String description;
    private String entriesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
