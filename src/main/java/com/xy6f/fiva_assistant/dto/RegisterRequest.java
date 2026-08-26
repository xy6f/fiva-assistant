package com.xy6f.fiva_assistant.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String companyName;
    private String captchaId;
    private String captchaAnswer;
}
