package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    private Long id;
    private Long userId;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
