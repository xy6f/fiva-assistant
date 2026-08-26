package com.xy6f.fiva_assistant.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;
    private String captchaAnswer;
}
