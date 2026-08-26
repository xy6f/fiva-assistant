-- ============================================================
-- FIVA Assistant 优化方案新增表（手动执行一次）
-- ============================================================

-- #4 科目体系模板：注册/首次使用时按此初始化用户科目表
CREATE TABLE IF NOT EXISTS `t_subject_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL COMMENT '科目编码',
  `name` varchar(100) NOT NULL COMMENT '科目名称',
  `type` varchar(20) NOT NULL COMMENT 'ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE',
  `balance_direction` varchar(10) NOT NULL DEFAULT 'DEBIT' COMMENT 'DEBIT/CREDIT',
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_template_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计科目模板';

-- #5 财税规则表（税率档、门槛等参数，程序运行时读取）
CREATE TABLE IF NOT EXISTS `t_tax_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_key` varchar(64) NOT NULL COMMENT '规则键',
  `tax_type` varchar(30) NOT NULL COMMENT 'VAT/SURCHARGE/CIT/VAT_RATE_TIER',
  `rate_value` decimal(10,6) DEFAULT NULL COMMENT '比率值',
  `threshold_value` decimal(15,2) DEFAULT NULL COMMENT '门槛金额',
  `description` varchar(200) DEFAULT NULL COMMENT '说明',
  `effective_from` date DEFAULT NULL,
  `effective_to` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tax_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财税规则表';

-- #5 发票/摘要关键词 → 会计科目映射
CREATE TABLE IF NOT EXISTS `t_account_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keyword` varchar(100) NOT NULL COMMENT '匹配关键词',
  `account_code` varchar(20) NOT NULL COMMENT '映射科目编码',
  `direction` varchar(10) NOT NULL DEFAULT 'DEBIT' COMMENT 'DEBIT/CREDIT',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，越小越先匹配',
  PRIMARY KEY (`id`),
  KEY `idx_account_mapping_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关键词科目映射表';

-- 财税规则初始数据
INSERT IGNORE INTO t_tax_rule (rule_key, tax_type, rate_value, threshold_value, description) VALUES
('VAT_SMALL_SCALE_RATE',       'VAT',       0.030000, NULL,      '小规模纳税人征收率3%'),
('VAT_GENERAL_RATE',           'VAT',       0.130000, NULL,      '一般纳税人默认增值税税率13%'),
('VAT_SMALL_SCALE_EXEMPT_ANNUAL','VAT',     NULL,     1200000.00,'小规模纳税人年销售额免征额度（月10万×12）'),
('SURCHARGE_URBAN_RATE',       'SURCHARGE', 0.070000, NULL,      '城市维护建设税7%（市区）'),
('SURCHARGE_EDU_RATE',         'SURCHARGE', 0.030000, NULL,      '教育费附加3%'),
('SURCHARGE_LOCAL_EDU_RATE',   'SURCHARGE', 0.020000, NULL,      '地方教育附加2%'),
('CIT_SMALL_THRESHOLD',        'CIT',       NULL,     3000000.00,'小型微利企业应纳税所得额上限300万'),
('CIT_SMALL_EFFECTIVE_RATE',   'CIT',       0.050000, NULL,      '小型微利企业实际税负5%'),
('CIT_STANDARD_RATE',          'CIT',       0.250000, NULL,      '企业所得税标准税率25%');

-- 常见发票类别/银行流水摘要 → 科目映射
INSERT IGNORE INTO t_account_mapping (keyword, account_code, direction, priority) VALUES
('工资', '6601', 'DEBIT', 10),
('薪',   '6601', 'DEBIT', 11),
('房租', '6602', 'DEBIT', 20),
('租金', '6602', 'DEBIT', 21),
('水电', '6602', 'DEBIT', 22),
('电费', '6602', 'DEBIT', 23),
('水费', '6602', 'DEBIT', 24),
('办公', '6602', 'DEBIT', 25),
('差旅', '6601', 'DEBIT', 30),
('招待', '6601', 'DEBIT', 31),
('利息', '6603', 'DEBIT', 40),
('税',   '2221', 'CREDIT', 50),
('销售', '5001', 'CREDIT', 60),
('货款', '5001', 'CREDIT', 61),
('服务', '5001', 'CREDIT', 62);


-- ============================================================
-- #7 AI 调用日志（指标真实数据）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_ai_call_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  call_type VARCHAR(32) NOT NULL COMMENT 'CHAT/OCR/TAX_SUGGEST/TAX_ANALYSIS',
  prompt_tokens INT NOT NULL DEFAULT 0,
  completion_tokens INT NOT NULL DEFAULT 0,
  latency_ms INT NOT NULL DEFAULT 0,
  success TINYINT(1) NOT NULL DEFAULT 1,
  error_msg VARCHAR(512) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_user_created (user_id, created_at),
  INDEX idx_user_type (user_id, call_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志';

-- ============================================================
-- #8 审计日志
-- ============================================================
CREATE TABLE IF NOT EXISTS t_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  username VARCHAR(64) NULL,
  action VARCHAR(64) NOT NULL COMMENT '操作类型，如 VOUCHER_CREATE',
  target_id VARCHAR(64) NULL COMMENT '操作对象ID',
  detail VARCHAR(1024) NULL,
  ip VARCHAR(64) NULL,
  success TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_user_created (user_id, created_at),
  INDEX idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

-- ============================================================
-- #10 凭证编号并发安全（全局按日序列，方案A）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_sequence (
  seq_name VARCHAR(32) PRIMARY KEY,
  seq_value BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='序列表';
