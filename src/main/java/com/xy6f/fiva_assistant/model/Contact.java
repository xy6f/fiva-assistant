package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
