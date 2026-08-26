package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private String department;
    private Long companyId;

    @Builder.Default
    private Role role = Role.VIEWER;

    @Builder.Default
    private Boolean enabled = true;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
