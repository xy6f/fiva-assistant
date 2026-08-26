package com.xy6f.fiva_assistant.model;

public enum Role {
    ADMIN,      // 管理员 - 全部权限
    MANAGER,    // 经理 - 审批、查看
    ACCOUNTANT, // 会计 - 记账、报销、报表
    VIEWER      // 查看者 - 只读
}
