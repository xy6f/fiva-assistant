package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    private Long id;
    private Long userId;
    private String name;
    private String taxNo;
    private String address;
    private String phone;
    private String bankName;
    private String bankAccount;
    private String legalPerson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
