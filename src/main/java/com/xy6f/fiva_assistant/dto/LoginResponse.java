package com.xy6f.fiva_assistant.dto;

import com.xy6f.fiva_assistant.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String realName;
    private Role role;
    private String department;
}
