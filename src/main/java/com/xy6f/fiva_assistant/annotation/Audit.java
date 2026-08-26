package com.xy6f.fiva_assistant.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解：标注在需要记录审计的写操作方法上。
 * 切面在方法成功返回后异步落库 t_audit_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 操作类型，如 VOUCHER_CREATE / VOUCHER_DELETE / INVOICE_DELETE / BANK_IMPORT */
    String action();

    /** 是否从方法参数中取对象ID（按名匹配 id 参数），默认 true */
    boolean recordTargetId() default true;
}
