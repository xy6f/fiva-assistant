-- ============================================================
-- 智能财税AI助手 - 数据库初始化脚本（完整版）
-- 包含：16张表建表语句 + admin(user_id=1) + accountant(user_id=2) 全部测试数据
-- 使用方式：source 本文件（会自动建库）
-- ============================================================

DROP DATABASE IF EXISTS fiva_assistant;
CREATE DATABASE fiva_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fiva_assistant;

-- ============================================================
-- 第一部分：建表（16张表）
-- ============================================================

-- 1. t_user 用户表
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `password` varchar(200) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `real_name` varchar(50) DEFAULT NULL,
  `role` enum('ACCOUNTANT','ADMIN','MANAGER','VIEWER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. t_company 公司表
CREATE TABLE `t_company` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `bank_account` varchar(50) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `legal_person` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `tax_no` varchar(50) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. t_account 会计科目表
CREATE TABLE `t_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `balance_direction` varchar(10) NOT NULL,
  `code` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. t_contact 联系人表
CREATE TABLE `t_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `bank_account` varchar(50) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `contact_person` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `tax_no` varchar(50) DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. t_voucher 凭证表
CREATE TABLE `t_voucher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `counterparty` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `has_invoice` bit(1) DEFAULT NULL,
  `invoice_image_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `type` varchar(10) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `voucher_date` date NOT NULL,
  `voucher_no` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_voucher_no` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. t_voucher_entry 凭证分录表
CREATE TABLE `t_voucher_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `credit_amount` decimal(15,2) DEFAULT NULL,
  `debit_amount` decimal(15,2) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `voucher_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. t_voucher_template 凭证模板表
CREATE TABLE `t_voucher_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `entries_json` text,
  `name` varchar(100) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. t_invoice 发票表
CREATE TABLE `t_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) DEFAULT NULL,
  `buyer_name` varchar(200) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `check_code` varchar(20) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `invoice_code` varchar(50) DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `invoice_no` varchar(50) DEFAULT NULL,
  `ocr_raw_text` text,
  `seller_name` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `tax_amount` decimal(15,2) DEFAULT NULL,
  `total_amount` decimal(15,2) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `verified` bit(1) DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `verify_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. t_bank_transaction 银行流水表
CREATE TABLE `t_bank_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_no` varchar(50) DEFAULT NULL,
  `balance` decimal(15,2) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `counterparty` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `credit_amount` decimal(15,2) DEFAULT NULL,
  `debit_amount` decimal(15,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `transaction_date` date DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `voucher_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. t_fixed_asset 固定资产表
CREATE TABLE `t_fixed_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint DEFAULT NULL,
  `accumulated_depreciation` decimal(15,2) DEFAULT NULL,
  `asset_code` varchar(50) DEFAULT NULL,
  `asset_name` varchar(200) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `depreciated_months` int DEFAULT NULL,
  `depreciation_account_id` bigint DEFAULT NULL,
  `depreciation_method` varchar(30) DEFAULT NULL,
  `expense_account_id` bigint DEFAULT NULL,
  `monthly_depreciation` decimal(15,2) DEFAULT NULL,
  `original_value` decimal(15,2) NOT NULL,
  `purchase_date` date DEFAULT NULL,
  `salvage_value` decimal(15,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `useful_life_months` int DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. t_salary_record 工资记录表
CREATE TABLE `t_salary_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `allowance` decimal(15,2) DEFAULT NULL,
  `base_salary` decimal(15,2) DEFAULT NULL,
  `bonus` decimal(15,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `employee_name` varchar(100) NOT NULL,
  `employee_no` varchar(50) DEFAULT NULL,
  `gross_pay` decimal(15,2) DEFAULT NULL,
  `housing_fund` decimal(15,2) DEFAULT NULL,
  `month` varchar(10) NOT NULL,
  `net_pay` decimal(15,2) DEFAULT NULL,
  `other_deduction` decimal(15,2) DEFAULT NULL,
  `overtime` decimal(15,2) DEFAULT NULL,
  `position` varchar(100) DEFAULT NULL,
  `social_insurance` decimal(15,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `tax` decimal(15,2) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. t_transaction 交易记录表
CREATE TABLE `t_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `counterparty` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `invoice_id` bigint DEFAULT NULL,
  `transaction_date` date NOT NULL,
  `type` varchar(10) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. t_budget 预算表
CREATE TABLE `t_budget` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alert_threshold` int DEFAULT NULL,
  `budget_amount` decimal(15,2) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `month` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `used_amount` decimal(15,2) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `year` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. t_expense 报销表
CREATE TABLE `t_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `applicant` varchar(100) NOT NULL,
  `approval_opinion` varchar(500) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `approver` varchar(100) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `expense_date` date DEFAULT NULL,
  `invoice_path` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. t_alert 预警表
CREATE TABLE `t_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `level` varchar(20) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `type` varchar(30) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. t_chat_message 聊天消息表
CREATE TABLE `t_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 第二部分：admin 账号数据（user_id=1）
-- ============================================================

-- 用户（密码均为 admin123）
INSERT INTO t_user (id, username, password, real_name, role, enabled, email, phone, department, company_id, created_at) VALUES
(1, 'admin', '$2a$10$4kWPeFZpTkvudqUGpAjkDO5RWltKu8fANRYO7uv24BOludRumjyjW', '系统管理员', 'ADMIN', b'1', 'admin@fiva.com', '13800000001', '财务部', 1, '2025-01-01 09:00:00'),
(2, 'accountant', '$2a$10$4kWPeFZpTkvudqUGpAjkDO5RWltKu8fANRYO7uv24BOludRumjyjW', '张会计', 'ACCOUNTANT', b'1', 'zhang@fiva.com', '13800000002', '财务部', 1, '2025-01-01 09:00:00');

-- 公司
INSERT INTO t_company (id, name, legal_person, tax_no, address, phone, bank_name, bank_account, user_id, created_at) VALUES
(1, '深圳星辰科技有限公司', '李明', '91440300MA5F1234X6', '深圳市南山区科技园路88号', '0755-88886666', '中国工商银行深圳南山支行', '4000123456789012345', 1, '2025-01-01 09:00:00');

-- 会计科目（user_id=1，10个基础科目）
INSERT INTO t_account (id, code, name, type, balance_direction, enabled, user_id, created_at) VALUES
(1, '1001', '库存现金',     'ASSET',      'DEBIT',  b'1', 1, '2025-01-01 09:00:00'),
(2, '1002', '银行存款',     'ASSET',      'DEBIT',  b'1', 1, '2025-01-01 09:00:00'),
(3, '1122', '应收账款',     'ASSET',      'DEBIT',  b'1', 1, '2025-01-01 09:00:00'),
(4, '1601', '固定资产',     'ASSET',      'DEBIT',  b'1', 1, '2025-01-01 09:00:00'),
(5, '1602', '累计折旧',     'ASSET',      'CREDIT', b'1', 1, '2025-01-01 09:00:00'),
(6, '2202', '应付账款',     'LIABILITY',  'CREDIT', b'1', 1, '2025-01-01 09:00:00'),
(7, '2211', '应付职工薪酬', 'LIABILITY',  'CREDIT', b'1', 1, '2025-01-01 09:00:00'),
(8, '2221', '应交税费',     'LIABILITY',  'CREDIT', b'1', 1, '2025-01-01 09:00:00'),
(9, '5001', '主营业务收入', 'REVENUE',    'CREDIT', b'1', 1, '2025-01-01 09:00:00'),
(10, '6601', '管理费用',    'EXPENSE',    'DEBIT',  b'1', 1, '2025-01-01 09:00:00');

-- 联系人（user_id=1）
INSERT INTO t_contact (id, name, type, contact_person, phone, email, address, tax_no, user_id, created_at) VALUES
(1, '深圳华创科技有限公司',   'SUPPLIER', '王经理', '0755-26661234', 'wang@huachuang.com', '深圳市南山区高新南一道', '91440300MA5G1111A1', 1, '2025-01-05 10:00:00'),
(2, '广州明达电子有限公司',   'CUSTOMER', '李总',   '020-38881234',  'li@mingda.com',      '广州市天河区珠江新城',   '91440100MA5G2222B2', 1, '2025-01-05 10:00:00');

-- 凭证（2张，user_id=1）
INSERT INTO t_voucher (id, voucher_no, voucher_date, type, amount, category, description, status, has_invoice, user_id, created_at) VALUES
(1, 'PZ-2025-0001', '2025-03-01', 'PAYMENT',  5000.00, '办公费用', '采购办公用品',   'POSTED', b'1', 1, '2025-03-01 10:00:00'),
(2, 'PZ-2025-0002', '2025-03-05', 'RECEIPT', 100000.00, '销售收入', '收到客户货款', 'POSTED', b'1', 1, '2025-03-05 14:00:00');

-- 凭证分录（4条，借贷平衡）
INSERT INTO t_voucher_entry (id, voucher_id, account_id, summary, debit_amount, credit_amount, sort_order) VALUES
(1, 1, 10, '采购办公用品-管理费用',   5000.00, NULL,    1),
(2, 1,  2, '采购办公用品-银行存款',   NULL,    5000.00, 2),
(3, 2,  2, '收到货款-银行存款',       100000.00, NULL,  1),
(4, 2,  9, '收到货款-主营业务收入',   NULL,   100000.00, 2);

-- 凭证模板
INSERT INTO t_voucher_template (id, name, type, description, entries_json, user_id, created_at) VALUES
(1, '采购入库', 'PAYMENT', '标准采购入库模板', '[{"accountCode":"1401","summary":"库存商品","debit":true},{"accountCode":"1002","summary":"银行存款","debit":false}]', 1, '2025-01-01 09:00:00');

-- 发票（3张，user_id=1）
INSERT INTO t_invoice (id, invoice_no, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, category, status, verified, user_id, created_at) VALUES
(1, '01234567', '2025-03-01', '深圳华创科技有限公司',   '深圳星辰科技有限公司', 4237.29,  550.85,   4788.14, '产品销售', 'VERIFIED', b'1', 1, '2025-03-01 10:00:00'),
(2, '01234568', '2025-03-05', '广州明达电子有限公司',   '深圳星辰科技有限公司', 88495.57, 11504.43, 100000.00, '产品销售', 'VERIFIED', b'1', 1, '2025-03-05 14:00:00'),
(3, '01234569', '2025-03-10', '东莞鑫源制造有限公司',   '深圳星辰科技有限公司', 943.40,   56.60,    1000.00, '咨询服务', 'PENDING',  b'0', 1, '2025-03-10 09:00:00');

-- 银行流水（3条，user_id=1）
INSERT INTO t_bank_transaction (id, bank_name, account_no, transaction_date, debit_amount, credit_amount, balance, counterparty, summary, status, user_id, created_at) VALUES
(1, '中国工商银行', '4000123456789012345', '2025-03-01', NULL,    50000.00, 150000.00, '广州明达电子有限公司',   '货款收入',   'MATCHED', 1, '2025-03-01 10:00:00'),
(2, '中国工商银行', '4000123456789012345', '2025-03-05', 5000.00, NULL,     145000.00, '深圳华创科技有限公司',   '采购付款',   'MATCHED', 1, '2025-03-05 14:00:00'),
(3, '中国工商银行', '4000123456789012345', '2025-03-10', 1000.00, NULL,     144000.00, '东莞鑫源制造有限公司',   '网络服务费', 'PENDING', 1, '2025-03-10 09:00:00');

-- 固定资产（2项，user_id=1）
INSERT INTO t_fixed_asset (id, asset_code, asset_name, category, original_value, salvage_value, useful_life_months, depreciated_months, monthly_depreciation, accumulated_depreciation, depreciation_method, purchase_date, status, account_id, depreciation_account_id, expense_account_id, user_id, created_at) VALUES
(1, 'FA-001', 'MacBook Pro笔记本电脑', '电子设备', 15000.00, 750.00, 36, 6, 395.83, 2375.00, '直线法', '2024-09-01', 'IN_USE', 4, 5, 10, 1, '2025-01-01 09:00:00'),
(2, 'FA-002', '办公桌椅套装',           '办公家具',  3000.00, 150.00, 60, 10, 47.50, 475.00, '直线法', '2024-05-01', 'IN_USE', 4, 5, 10, 1, '2025-01-01 09:00:00');

-- 工资记录（3条，user_id=1）
INSERT INTO t_salary_record (id, employee_no, employee_name, department, position, month, base_salary, allowance, bonus, overtime, gross_pay, social_insurance, housing_fund, other_deduction, tax, net_pay, status, user_id, created_at) VALUES
(1, 'EMP001', '张会计', '财务部', '会计',   '2025-03', 8000.00, 500.00, 1000.00, 0.00, 9500.00, 800.00, 400.00, 0.00, 9.00, 8291.00, 'PAID', 1, '2025-03-25 10:00:00'),
(2, 'EMP002', '李出纳', '财务部', '出纳',   '2025-03', 15000.00, 1000.00, 3000.00, 0.00, 19000.00, 1500.00, 800.00, 0.00, 267.00, 16433.00, 'PAID', 1, '2025-03-25 10:00:00'),
(3, 'EMP003', '王开发', '技术部', '工程师', '2025-03', 12000.00, 800.00, 2000.00, 500.00, 15300.00, 1200.00, 600.00, 0.00, 105.00, 13395.00, 'PAID', 1, '2025-03-25 10:00:00');

-- 交易记录（3条，user_id=1）
INSERT INTO t_transaction (id, transaction_date, type, amount, category, counterparty, description, user_id, created_at) VALUES
(1, '2025-03-01', 'INCOME',  50000.00, '销售收入', '深圳华创科技有限公司', '产品销售回款', 1, '2025-03-01 10:00:00'),
(2, '2025-03-05', 'EXPENSE',  5000.00, '办公费',   '华为技术有限公司',       '办公用品采购', 1, '2025-03-05 14:00:00'),
(3, '2025-03-10', 'EXPENSE',  1000.00, '差旅费',   '携程旅行网',           '出差交通费',   1, '2025-03-10 09:00:00');

-- 预算（3条，user_id=1）
INSERT INTO t_budget (id, year, month, department, category, budget_amount, used_amount, alert_threshold, description, user_id, created_at) VALUES
(1, 2025, 3, '财务部', '办公费',   10000.00, 5000.00, 80, '三月办公费预算', 1, '2025-03-01 09:00:00'),
(2, 2025, 3, '技术部', '研发费用', 50000.00, 32000.00, 80, '三月研发预算',   1, '2025-03-01 09:00:00'),
(3, 2025, 3, '管理部', '差旅费',   8000.00,  2500.00, 80, '三月差旅预算',   1, '2025-03-01 09:00:00');

-- 报销（2条，user_id=1）
INSERT INTO t_expense (id, title, applicant, amount, category, expense_date, description, status, approver, approval_opinion, approved_at, user_id, created_at) VALUES
(1, '深圳出差交通费报销', '张会计', 5000.00, '差旅费', '2025-03-01', '购买打印纸、墨盒等办公用品', 'APPROVED', '李明', '同意报销', '2025-03-02 10:00:00', 1, '2025-03-01 15:00:00'),
(2, '办公用品采购报销',   '李出纳', 1200.00, '办公费', '2025-03-08', '前往广州客户现场技术支持',   'APPROVED', '李明', '同意报销', '2025-03-09 10:00:00', 1, '2025-03-08 18:00:00');

-- 预警（3条，user_id=1）
INSERT INTO t_alert (id, title, content, level, type, status, user_id, created_at) VALUES
(1, '预算超支预警',     '本月增值税税负率为1.30%，低于行业平均水平2.0%，请关注。', 'WARNING', 'TAX_RISK',  'UNREAD', 1, '2025-03-15 09:00:00'),
(2, '发票待核验提醒',   '财务部办公费已使用80%，接近预算上限。',              'INFO',    'BUDGET',    'UNREAD', 1, '2025-03-20 10:00:00'),
(3, '银行流水未匹配',   '有2张进项发票将于本月底到期，请及时认证抵扣。',       'WARNING', 'INVOICE',   'UNREAD', 1, '2025-03-22 14:00:00');

-- 聊天消息（4条，user_id=1）
INSERT INTO t_chat_message (id, session_id, role, content, user_id, created_at) VALUES
(1, 'session-001', 'user',      '请分析本月利润表现', 1, '2025-03-25 10:00:00'),
(2, 'session-001', 'assistant', '根据您3月的财务数据：营业收入100,000元，管理费用5,000元，工资支出约19,000元。毛利率约80%，净利润率约76%。整体表现良好，建议关注办公费支出趋势。', 1, '2025-03-25 10:00:05'),
(3, 'session-001', 'user',      '有哪些税务风险需要注意？', 1, '2025-03-25 10:01:00'),
(4, 'session-001', 'assistant', '当前需关注：1.增值税税负率1.30%偏低，建议核查进项发票；2.确保所有费用发票合规；3.工资薪金个税已按时申报。', 1, '2025-03-25 10:01:05');


-- ============================================================
-- 第三部分：accountant 账号数据（user_id=2）
-- ============================================================

-- 会计科目（user_id=2，12个基础科目）
INSERT INTO t_account (id, user_id, code, name, type, balance_direction, enabled, created_at) VALUES
(11, 2, '1001', '库存现金',     'ASSET',     'DEBIT',  b'1', '2025-01-15 09:00:00'),
(12, 2, '1002', '银行存款',     'ASSET',     'DEBIT',  b'1', '2025-01-15 09:00:00'),
(13, 2, '1122', '应收账款',     'ASSET',     'DEBIT',  b'1', '2025-01-15 09:00:00'),
(14, 2, '1405', '库存商品',     'ASSET',     'DEBIT',  b'1', '2025-01-15 09:00:00'),
(15, 2, '1601', '固定资产',     'ASSET',     'DEBIT',  b'1', '2025-01-15 09:00:00'),
(16, 2, '2202', '应付账款',     'LIABILITY', 'CREDIT', b'1', '2025-01-15 09:00:00'),
(17, 2, '2211', '应付职工薪酬', 'LIABILITY', 'CREDIT', b'1', '2025-01-15 09:00:00'),
(18, 2, '5001', '主营业务收入', 'REVENUE',   'CREDIT', b'1', '2025-01-15 09:00:00'),
(19, 2, '5051', '其他业务收入', 'REVENUE',   'CREDIT', b'1', '2025-01-15 09:00:00'),
(20, 2, '6601', '管理费用',     'EXPENSE',   'DEBIT',  b'1', '2025-01-15 09:00:00'),
(21, 2, '6602', '销售费用',     'EXPENSE',   'DEBIT',  b'1', '2025-01-15 09:00:00'),
(22, 2, '6801', '营业外支出',   'EXPENSE',   'DEBIT',  b'1', '2025-01-15 09:00:00');

-- 发票（5张，user_id=2）
INSERT INTO t_invoice (id, user_id, invoice_no, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, category, status, created_at) VALUES
(4,  2, 'FP-2025-0001', '2025-04-05', '华为技术有限公司',     '深圳星辰科技有限公司', 28000.00, 3640.00, 31640.00,  '办公用品', 'VERIFIED', '2025-04-05 10:00:00'),
(5,  2, 'FP-2025-0002', '2025-04-12', '京东世纪贸易有限公司', '深圳星辰科技有限公司', 5600.00,  728.00,  6328.00,   '电子设备', 'VERIFIED', '2025-04-12 14:30:00'),
(6,  2, 'FP-2025-0003', '2025-04-18', '中石化深圳分公司',     '深圳星辰科技有限公司', 3200.00,  416.00,  3616.00,   '交通费用', 'VERIFIED', '2025-04-18 09:15:00'),
(7,  2, 'FP-2025-0004', '2025-04-25', '腾讯云计算有限公司',   '深圳星辰科技有限公司', 15000.00, 1950.00, 16950.00,  '技术服务', 'PENDING',  '2025-04-25 16:00:00'),
(8,  2, 'FP-2025-0005', '2025-05-03', '顺丰速运有限公司',     '深圳星辰科技有限公司', 1800.00,  234.00,  2034.00,   '物流费用', 'PENDING',  '2025-05-03 11:20:00');

-- 收支流水（8笔，user_id=2）
INSERT INTO t_transaction (id, user_id, type, amount, category, description, transaction_date, counterparty, created_at) VALUES
(4,  2, 'INCOME',  120000.00, '销售收入', '产品销售回款-A客户', '2025-03-10', '深圳华创科技有限公司',   '2025-03-10 10:00:00'),
(5,  2, 'INCOME',   85000.00, '销售收入', '产品销售回款-B客户', '2025-03-25', '广州明达电子有限公司',   '2025-03-25 14:00:00'),
(6,  2, 'INCOME',   45000.00, '服务收入', '技术咨询服务费',     '2025-04-08', '东莞鑫源制造有限公司',   '2025-04-08 09:30:00'),
(7,  2, 'INCOME',   68000.00, '销售收入', '产品销售回款-C客户', '2025-04-20', '珠海蓝海贸易有限公司', '2025-04-20 11:00:00'),
(8,  2, 'EXPENSE',  12000.00, '办公费用', '办公用品采购',       '2025-03-15', '华为技术有限公司',       '2025-03-15 10:00:00'),
(9,  2, 'EXPENSE',   8500.00, '差旅费用', '出差交通住宿',       '2025-04-02', '携程旅行网',             '2025-04-02 16:00:00'),
(10, 2, 'EXPENSE',  35000.00, '人工成本', '3月工资发放',        '2025-04-05', '员工薪酬',               '2025-04-05 09:00:00'),
(11, 2, 'EXPENSE',   6800.00, '办公费用', '云服务器租赁',       '2025-04-15', '腾讯云计算有限公司',     '2025-04-15 14:00:00');

-- 凭证（3张，user_id=2）
INSERT INTO t_voucher (id, user_id, voucher_no, type, amount, category, description, voucher_date, status, created_at) VALUES
(3, 2, 'PZ-2025-0010', 'RECEIPT', 120000.00, '销售收入', '收到A客户货款', '2025-03-10', 'POSTED', '2025-03-10 10:30:00'),
(4, 2, 'PZ-2025-0011', 'PAYMENT',  35000.00, '人工成本', '发放3月工资',   '2025-04-05', 'POSTED', '2025-04-05 09:30:00'),
(5, 2, 'PZ-2025-0012', 'PAYMENT',  12000.00, '办公费用', '采购办公用品',  '2025-03-15', 'DRAFT',  '2025-03-15 11:00:00');

-- 凭证分录（6条，user_id=2 对应上面3张凭证）
INSERT INTO t_voucher_entry (id, voucher_id, account_id, debit_amount, credit_amount, sort_order, summary) VALUES
(5,  3, 12, 120000.00, NULL,      1, '收到A客户货款-银行存款增加'),
(6,  3, 18, NULL,      120000.00, 2, '收到A客户货款-主营业务收入'),
(7,  4, 17, 35000.00,  NULL,      1, '发放3月工资-应付职工薪酬减少'),
(8,  4, 12, NULL,      35000.00,  2, '发放3月工资-银行存款减少'),
(9,  5, 20, 12000.00,  NULL,      1, '采购办公用品-管理费用增加'),
(10, 5, 12, NULL,      12000.00,  2, '采购办公用品-银行存款减少');

-- 客户/供应商（4个，user_id=2）
INSERT INTO t_contact (id, user_id, name, type, contact_person, phone, email, address, tax_no, bank_name, bank_account, created_at) VALUES
(3, 2, '深圳华创科技有限公司', 'CUSTOMER', '王经理', '0755-26881234', 'wang@huachuang.com', '深圳市南山区高新南一道', '91440300MA5G1111A1', '招商银行深圳高新支行', '7559012345678901', '2025-02-01 09:00:00'),
(4, 2, '广州明达电子有限公司', 'CUSTOMER', '李总',   '020-38881234',  'li@mingda.com',      '广州市天河区珠江新城',   '91440100MA5G2222B2', '工商银行广州天河支行', '3602012345678901', '2025-02-10 09:00:00'),
(5, 2, '华为技术有限公司',     'SUPPLIER', '张采购', '0755-28781234', 'zhang@huawei.com',   '深圳市龙岗区坂田基地',   '91440300MA5G3333C3', '建设银行深圳龙岗支行', '4420012345678901', '2025-02-15 09:00:00'),
(6, 2, '腾讯云计算有限公司',   'SUPPLIER', '赵经理', '0755-86011234', 'zhao@tencent.com',   '深圳市南山区科技园',     '91440300MA5G4444D4', '农业银行深圳南山支行', '4100012345678901', '2025-03-01 09:00:00');

-- 银行流水（5笔，user_id=2）
INSERT INTO t_bank_transaction (id, user_id, transaction_date, summary, counterparty, debit_amount, credit_amount, balance, account_no, bank_name, status, created_at) VALUES
(4, 2, '2025-03-10', '货款收入', '深圳华创科技有限公司',   NULL,     120000.00, 520000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2025-03-10 10:00:00'),
(5, 2, '2025-03-15', '办公采购', '华为技术有限公司',       12000.00, NULL,      508000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2025-03-15 10:00:00'),
(6, 2, '2025-03-25', '货款收入', '广州明达电子有限公司',   NULL,     85000.00,  593000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2025-03-25 14:00:00'),
(7, 2, '2025-04-05', '工资发放', '员工薪酬',               35000.00, NULL,      558000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2025-04-05 09:00:00'),
(8, 2, '2025-04-20', '货款收入', '珠海蓝海贸易有限公司',   NULL,     68000.00,  626000.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2025-04-20 11:00:00');

-- 固定资产（3项，user_id=2）
INSERT INTO t_fixed_asset (id, user_id, asset_code, asset_name, category, original_value, salvage_value, useful_life_months, monthly_depreciation, depreciated_months, accumulated_depreciation, purchase_date, depreciation_method, status, created_at) VALUES
(3, 2, 'FA-2024-001', '办公电脑（联想ThinkPad）', '电子设备', 8500.00, 425.00, 36, 224.31, 18, 4037.58, '2024-01-10', 'STRAIGHT_LINE', 'IN_USE', '2024-01-10 09:00:00'),
(4, 2, 'FA-2024-002', '办公桌椅套装',             '办公家具', 3200.00, 160.00, 60, 50.67,  18, 912.06,  '2024-01-10', 'STRAIGHT_LINE', 'IN_USE', '2024-01-10 09:00:00'),
(5, 2, 'FA-2025-001', '打印机（惠普激光）',       '办公设备', 4500.00, 225.00, 48, 89.06,  6,  534.36,  '2024-11-01', 'STRAIGHT_LINE', 'IN_USE', '2024-11-01 09:00:00');

-- 工资记录（4人，user_id=2）
INSERT INTO t_salary_record (id, user_id, month, employee_no, employee_name, department, position, base_salary, allowance, bonus, overtime, social_insurance, housing_fund, tax, other_deduction, gross_pay, net_pay, status, created_at) VALUES
(4, 2, '2025-03', 'EMP001', '张会计', '财务部', '会计',   12000.00, 2000.00, 1500.00, 0.00, 1800.00, 1200.00, 450.00, 0.00, 15500.00, 12050.00, 'PAID', '2025-04-05 09:00:00'),
(5, 2, '2025-03', 'EMP002', '李出纳', '财务部', '出纳',   10000.00, 1500.00, 1000.00, 500.00, 1500.00, 1000.00, 350.00, 0.00, 13000.00, 10150.00, 'PAID', '2025-04-05 09:00:00'),
(6, 2, '2025-03', 'EMP003', '王开发', '技术部', '工程师', 18000.00, 3000.00, 3000.00, 1200.00, 2700.00, 1800.00, 900.00, 0.00, 25200.00, 19800.00, 'PAID', '2025-04-05 09:00:00'),
(7, 2, '2025-03', 'EMP004', '赵销售', '销售部', '经理',   15000.00, 2500.00, 5000.00, 800.00, 2250.00, 1500.00, 750.00, 0.00, 23300.00, 18050.00, 'PAID', '2025-04-05 09:00:00');

-- 费用报销（3笔，user_id=2）
INSERT INTO t_expense (id, user_id, title, applicant, category, amount, expense_date, description, status, approver, approval_opinion, approved_at, created_at, updated_at) VALUES
(3, 2, '深圳出差交通费', '张会计', '差旅费用', 2800.00, '2025-04-02', '深圳到广州出差高铁票及市内交通', 'APPROVED', '李明', '同意报销', '2025-04-08 10:00:00', '2025-04-03 09:00:00', '2025-04-08 10:00:00'),
(4, 2, '办公用品采购',   '张会计', '办公费用', 1560.00, '2025-04-10', '打印纸、墨盒、文件夹等',         'APPROVED', '李明', '同意报销', '2025-04-12 14:00:00', '2025-04-10 11:00:00', '2025-04-12 14:00:00'),
(5, 2, '客户招待餐费',   '张会计', '招待费用', 980.00,  '2025-04-18', '招待A客户晚餐',                  'PENDING',  NULL,   NULL,         NULL,                  '2025-04-19 09:00:00', '2025-04-19 09:00:00');

-- 预算（3项，user_id=2）
INSERT INTO t_budget (id, user_id, year, month, department, category, budget_amount, used_amount, alert_threshold, description, created_at, updated_at) VALUES
(4, 2, 2025, 4, '财务部', '办公费用', 5000.00,  3560.00,  80, '4月办公用品预算', '2025-04-01 09:00:00', '2025-04-15 09:00:00'),
(5, 2, 2025, 4, '财务部', '差旅费用', 8000.00,  2800.00,  80, '4月出差预算',     '2025-04-01 09:00:00', '2025-04-10 09:00:00'),
(6, 2, 2025, 4, '技术部', '技术服务', 20000.00, 16950.00, 80, '4月云服务预算',   '2025-04-01 09:00:00', '2025-04-25 09:00:00');

-- 预警消息（3条，user_id=2）
INSERT INTO t_alert (id, user_id, type, level, title, content, status, related_id, created_at) VALUES
(4, 2, 'BUDGET',  'WARNING', '预算预警',     '技术服务类预算已使用84.75%，接近预警阈值', 'UNREAD', NULL, '2025-04-25 16:00:00'),
(5, 2, 'INVOICE', 'INFO',    '发票待核验',   '有2张发票待核验，请及时处理',              'UNREAD', NULL, '2025-05-03 11:30:00'),
(6, 2, 'BANK',    'WARNING', '银行流水未匹配', '1笔银行流水未匹配到对应凭证',            'READ',   NULL, '2025-04-20 11:30:00');

-- AI聊天消息（8条，user_id=2）
INSERT INTO t_chat_message (id, user_id, session_id, role, content, created_at) VALUES
(5,  2, 'sess-acct-001', 'user',      '帮我分析本月的收支情况', '2025-04-20 10:00:00'),
(6,  2, 'sess-acct-001', 'assistant', '本月收入218,000元，支出62,300元，净利润155,700元。收入主要来自销售收入和服务收入...', '2025-04-20 10:00:05'),
(7,  2, 'sess-acct-001', 'user',      '有哪些税务风险需要注意？', '2025-04-20 10:05:00'),
(8,  2, 'sess-acct-001', 'assistant', '根据当前数据分析，建议关注以下几点：1. 增值税进项抵扣是否充分；2. 企业所得税汇算清缴...', '2025-04-20 10:05:08'),
(9,  2, 'sess-acct-0601', 'user',      '帮我分析一下6月份的经营情况', '2026-06-15 10:00:00'),
(10, 2, 'sess-acct-0601', 'assistant', '根据您6月份的财务数据：营业收入419,000元，其中销售收入324,000元、服务收入45,000元、其他收入32,000元、设备租赁18,000元。总支出约77,700元。净利润约341,300元，利润率约81.5%。整体经营状况良好，建议关注以下几点：\n1. 办公费用预算已使用75.67%，注意控制\n2. 有2笔银行流水未匹配凭证\n3. 增值税税负率偏低，建议核查进项发票', '2026-06-15 10:00:08'),
(11, 2, 'sess-acct-0601', 'user',      '税负率偏低是什么原因？', '2026-06-15 10:02:00'),
(12, 2, 'sess-acct-0601', 'assistant', '税负率偏低可能有以下原因：\n1. 进项发票较多，抵扣充分——建议检查是否有不属于本期的进项提前抵扣\n2. 部分收入适用低税率或免税政策\n3. 存在大额固定资产采购导致进项集中\n建议：核查6月进项发票明细，确认是否有异常大额采购发票。如需进一步分析，可提供进项发票清单。', '2026-06-15 10:02:10');


-- ============================================================
-- 第四部分：accountant 账号 2026年6月补充数据
-- ============================================================

-- 发票（8张，user_id=2，2026年6月）
INSERT INTO t_invoice (id, user_id, invoice_no, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, category, status, verified, created_at) VALUES
(9,  2, 'INV-2026-0601', '2026-06-01', '深圳华创科技有限公司',   '深圳星辰科技有限公司', 56000.00,  7280.00,  63280.00,  '销售收入',   'VERIFIED', b'1', '2026-06-01 09:30:00'),
(10, 2, 'INV-2026-0602', '2026-06-03', '广州明达电子有限公司',   '深圳星辰科技有限公司', 32000.00,  4160.00,  36160.00,  '销售收入',   'VERIFIED', b'1', '2026-06-03 10:15:00'),
(11, 2, 'INV-2026-0603', '2026-06-05', '东莞鑫源制造有限公司',   '深圳星辰科技有限公司', 18500.00,  2405.00,  20905.00,  '服务收入',   'VERIFIED', b'1', '2026-06-05 14:20:00'),
(12, 2, 'INV-2026-0604', '2026-06-07', '华为技术有限公司',       '深圳星辰科技有限公司', 8900.00,   1157.00,  10057.00,  '办公用品',   'VERIFIED', b'1', '2026-06-07 11:00:00'),
(13, 2, 'INV-2026-0605', '2026-06-09', '顺丰速运有限公司',       '深圳星辰科技有限公司', 2400.00,   312.00,   2712.00,   '物流费用',   'VERIFIED', b'1', '2026-06-09 16:30:00'),
(14, 2, 'INV-2026-0606', '2026-06-11', '中国电信深圳分公司',     '深圳星辰科技有限公司', 3600.00,   468.00,   4068.00,   '通信费用',   'PENDING',  NULL, '2026-06-11 09:00:00'),
(15, 2, 'INV-2026-0607', '2026-06-13', '珠海蓝海贸易有限公司',   '深圳星辰科技有限公司', 42000.00,  5460.00,  47460.00,  '销售收入',   'PENDING',  NULL, '2026-06-13 15:45:00'),
(16, 2, 'INV-2026-0608', '2026-06-15', '京东世纪贸易有限公司',   '深圳星辰科技有限公司', 6800.00,   884.00,   7684.00,   '电子设备',   'PENDING',  NULL, '2026-06-15 10:00:00');

-- 收支流水（12笔，user_id=2，2026年6月）
INSERT INTO t_transaction (id, user_id, type, amount, category, description, transaction_date, counterparty, created_at) VALUES
(12, 2, 'INCOME',  150000.00, '销售收入', '产品销售回款-华创科技6月批次', '2026-06-02', '深圳华创科技有限公司',   '2026-06-02 10:00:00'),
(13, 2, 'INCOME',   98000.00, '销售收入', '产品销售回款-明达电子6月批次', '2026-06-05', '广州明达电子有限公司',   '2026-06-05 14:30:00'),
(14, 2, 'INCOME',   45000.00, '服务收入', '技术咨询服务费-6月',          '2026-06-08', '东莞鑫源制造有限公司',   '2026-06-08 09:00:00'),
(15, 2, 'INCOME',   76000.00, '销售收入', '产品销售回款-蓝海贸易6月批次', '2026-06-12', '珠海蓝海贸易有限公司', '2026-06-12 11:00:00'),
(16, 2, 'INCOME',   32000.00, '其他收入', '设备租赁收入',                 '2026-06-14', '佛山顺德制造有限公司',   '2026-06-14 16:00:00'),
(17, 2, 'INCOME',   18000.00, '服务收入', '财务顾问服务费',               '2026-06-15', '惠州恒信实业有限公司',   '2026-06-15 10:30:00'),
(18, 2, 'EXPENSE',  42000.00, '人工成本', '6月工资发放',                  '2026-06-05', '员工薪酬',               '2026-06-05 09:00:00'),
(19, 2, 'EXPENSE',  15600.00, '办公费用', '云服务器租赁-6月',             '2026-06-10', '腾讯云计算有限公司',     '2026-06-10 14:00:00'),
(20, 2, 'EXPENSE',   8900.00, '办公费用', '办公用品采购',                 '2026-06-07', '华为技术有限公司',       '2026-06-07 11:30:00'),
(21, 2, 'EXPENSE',   5200.00, '差旅费用', '广州出差交通住宿',             '2026-06-09', '携程旅行网',             '2026-06-09 17:00:00'),
(22, 2, 'EXPENSE',   3600.00, '通信费用', '公司电话费-6月',               '2026-06-11', '中国电信深圳分公司',     '2026-06-11 09:30:00'),
(23, 2, 'EXPENSE',   2400.00, '物流费用', '产品配送运费',                 '2026-06-13', '顺丰速运有限公司',       '2026-06-13 16:00:00');

-- 凭证（6张，user_id=2，2026年6月）
INSERT INTO t_voucher (id, user_id, voucher_no, type, amount, category, description, voucher_date, counterparty, has_invoice, status, created_at) VALUES
(6, 2, 'PZ-2026-0601', 'RECEIPT', 150000.00, '销售收入', '收到华创科技货款',     '2026-06-02', '深圳华创科技有限公司', b'1', 'POSTED', '2026-06-02 10:30:00'),
(7, 2, 'PZ-2026-0602', 'PAYMENT',  42000.00, '人工成本', '发放6月工资',           '2026-06-05', '员工薪酬',               b'0', 'POSTED', '2026-06-05 09:30:00'),
(8, 2, 'PZ-2026-0603', 'RECEIPT',  98000.00, '销售收入', '收到明达电子货款',     '2026-06-05', '广州明达电子有限公司',   b'1', 'POSTED', '2026-06-05 15:00:00'),
(9, 2, 'PZ-2026-0604', 'PAYMENT',  15600.00, '办公费用', '云服务器租赁费',         '2026-06-10', '腾讯云计算有限公司',     b'1', 'POSTED', '2026-06-10 14:30:00'),
(10,2, 'PZ-2026-0605', 'RECEIPT',  45000.00, '服务收入', '收到鑫源制造咨询费',   '2026-06-08', '东莞鑫源制造有限公司',   b'1', 'POSTED', '2026-06-08 09:30:00'),
(11,2, 'PZ-2026-0606', 'PAYMENT',   8900.00, '办公费用', '采购办公用品',           '2026-06-07', '华为技术有限公司',       b'1', 'DRAFT',  '2026-06-07 12:00:00');

-- 凭证分录（12条，user_id=2，2026年6月）
INSERT INTO t_voucher_entry (id, voucher_id, account_id, debit_amount, credit_amount, sort_order, summary) VALUES
(11, 6,  12, 150000.00, NULL,      1, '收到华创科技货款-银行存款增加'),
(12, 6,  18, NULL,      150000.00, 2, '收到华创科技货款-主营业务收入'),
(13, 7,  17,  42000.00, NULL,      1, '发放6月工资-应付职工薪酬减少'),
(14, 7,  12, NULL,       42000.00, 2, '发放6月工资-银行存款减少'),
(15, 8,  12,  98000.00, NULL,      1, '收到明达电子货款-银行存款增加'),
(16, 8,  18, NULL,       98000.00, 2, '收到明达电子货款-主营业务收入'),
(17, 9,  20,  15600.00, NULL,      1, '云服务器租赁费-管理费用增加'),
(18, 9,  12, NULL,       15600.00, 2, '云服务器租赁费-银行存款减少'),
(19, 10, 12,  45000.00, NULL,      1, '收到鑫源制造咨询费-银行存款增加'),
(20, 10, 19, NULL,       45000.00, 2, '收到鑫源制造咨询费-其他业务收入'),
(21, 11, 20,   8900.00, NULL,      1, '采购办公用品-管理费用增加'),
(22, 11, 12, NULL,        8900.00, 2, '采购办公用品-银行存款减少');

-- 银行流水（8笔，user_id=2，2026年6月）
INSERT INTO t_bank_transaction (id, user_id, transaction_date, summary, counterparty, debit_amount, credit_amount, balance, account_no, bank_name, status, created_at) VALUES
(9,  2, '2026-06-02', '货款收入', '深圳华创科技有限公司',   NULL,      150000.00, 776000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-02 10:00:00'),
(10, 2, '2026-06-05', '工资发放', '员工薪酬',               42000.00,  NULL,      734000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-05 09:00:00'),
(11, 2, '2026-06-05', '货款收入', '广州明达电子有限公司',   NULL,       98000.00, 832000.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-05 14:30:00'),
(12, 2, '2026-06-07', '办公采购', '华为技术有限公司',       8900.00,   NULL,      823100.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-07 11:30:00'),
(13, 2, '2026-06-08', '咨询收入', '东莞鑫源制造有限公司',   NULL,       45000.00, 868100.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-08 09:30:00'),
(14, 2, '2026-06-10', '云服务费', '腾讯云计算有限公司',     15600.00,  NULL,      852500.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-10 14:30:00'),
(15, 2, '2026-06-12', '货款收入', '珠海蓝海贸易有限公司',   NULL,       76000.00, 928500.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2026-06-12 11:00:00'),
(16, 2, '2026-06-14', '设备租金', '佛山顺德制造有限公司',   NULL,       32000.00, 960500.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2026-06-14 16:00:00');

-- 费用报销（4笔，user_id=2，2026年6月）
INSERT INTO t_expense (id, user_id, title, applicant, category, amount, expense_date, description, invoice_path, status, approver, approval_opinion, approved_at, created_at, updated_at) VALUES
(6,  2, '6月广州出差报销',   '张会计', '差旅费用', 4500.00,  '2026-06-09', '广州客户拜访高铁+住宿+餐饮', NULL, 'APPROVED', '李明', '同意报销', '2026-06-11 10:00:00', '2026-06-10 09:00:00', '2026-06-11 10:00:00'),
(7,  2, '6月办公用品采购',   '张会计', '办公费用', 2360.00,  '2026-06-07', '打印纸、墨盒、文件夹',       NULL, 'APPROVED', '李明', '同意报销', '2026-06-08 14:00:00', '2026-06-07 15:00:00', '2026-06-08 14:00:00'),
(8,  2, '6月客户招待餐费',   '张会计', '招待费用', 1680.00,  '2026-06-12', '招待蓝海贸易李总晚餐',       NULL, 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-13 09:00:00', '2026-06-13 09:00:00'),
(9,  2, '6月快递费报销',     '张会计', '办公费用', 580.00,   '2026-06-14', '产品样品寄送顺丰快递费',     NULL, 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-14 16:30:00', '2026-06-14 16:30:00');

-- 预算（3项，user_id=2，2026年6月）
INSERT INTO t_budget (id, user_id, year, month, department, category, budget_amount, used_amount, alert_threshold, description, created_at, updated_at) VALUES
(7,  2, 2026, 6, '财务部', '办公费用', 6000.00,   4540.00, 80, '6月办公用品及耗材预算', '2026-06-01 09:00:00', '2026-06-14 09:00:00'),
(8,  2, 2026, 6, '财务部', '差旅费用', 10000.00,  4500.00, 80, '6月出差预算',           '2026-06-01 09:00:00', '2026-06-10 09:00:00'),
(9,  2, 2026, 6, '技术部', '技术服务', 25000.00,  15600.00, 80, '6月云服务及技术服务',   '2026-06-01 09:00:00', '2026-06-10 14:00:00');

-- 工资记录（4人，user_id=2，2026年6月）
INSERT INTO t_salary_record (id, user_id, month, employee_no, employee_name, department, position, base_salary, allowance, bonus, overtime, social_insurance, housing_fund, tax, other_deduction, gross_pay, net_pay, status, created_at) VALUES
(8,  2, '2026-06', 'EMP001', '张会计', '财务部', '会计',    12000.00, 2000.00, 2000.00, 0.00, 1800.00, 1200.00, 510.00, 0.00, 16000.00, 12490.00, 'PAID', '2026-06-05 09:00:00'),
(9,  2, '2026-06', 'EMP002', '李出纳', '财务部', '出纳',    10000.00, 1500.00, 1200.00, 300.00, 1500.00, 1000.00, 380.00, 0.00, 13000.00, 10120.00, 'PAID', '2026-06-05 09:00:00'),
(10, 2, '2026-06', 'EMP003', '王开发', '技术部', '工程师',  18000.00, 3000.00, 4000.00, 1500.00, 2700.00, 1800.00, 1050.00, 0.00, 26500.00, 20950.00, 'PAID', '2026-06-05 09:00:00'),
(11, 2, '2026-06', 'EMP004', '赵销售', '销售部', '经理',    15000.00, 2500.00, 6000.00, 1000.00, 2250.00, 1500.00, 825.00, 0.00, 24500.00, 19925.00, 'PAID', '2026-06-05 09:00:00');

-- 预警消息（4条，user_id=2，2026年6月）
INSERT INTO t_alert (id, user_id, type, level, title, content, status, created_at) VALUES
(7,  2, 'BUDGET',  'WARNING', '办公费用预算预警',     '6月办公费用已使用75.67%，接近预警阈值80%，请注意控制支出。', 'UNREAD', '2026-06-10 09:00:00'),
(8,  2, 'INVOICE', 'INFO',    '发票待核验提醒',       '有3张发票尚未核验，请及时处理以免影响抵扣。',               'UNREAD', '2026-06-13 10:00:00'),
(9,  2, 'BANK',    'WARNING', '银行流水未匹配',       '有2笔银行流水未匹配到对应凭证，请核实。',                     'UNREAD', '2026-06-14 11:00:00'),
(10, 2, 'TAX_RISK','WARNING', '增值税税负率偏低',     '本月增值税税负率为1.15%，低于行业平均水平2.0%，请关注。',    'UNREAD', '2026-06-15 09:00:00');


-- ============================================================
-- 第五部分：accountant 账号 2026年6月 大量补充数据
-- ============================================================

-- 新增联系人（3个新客户/供应商）
INSERT INTO t_contact (id, user_id, name, type, contact_person, phone, email, address, tax_no, bank_name, bank_account, created_at) VALUES
(7,  2, '佛山顺德制造有限公司',   'CUSTOMER', '陈厂长', '0757-66881234', 'chen@foshun.com',     '佛山市顺德区大良街道',     '91440600MA5G5555E5', '中国银行佛山顺德支行', '6010012345678901', '2026-06-01 10:00:00'),
(8,  2, '惠州恒信实业有限公司',   'CUSTOMER', '刘副总', '0752-22881234', 'liu@hengxin.com',     '惠州市惠城区江北大道',     '91441300MA5G6666F6', '建设银行惠州惠城支行', '4400012345678901', '2026-06-03 10:00:00'),
(9,  2, '深圳中正会计师事务所',   'SUPPLIER', '周审计', '0755-25881234', 'zhou@zhongzheng.com', '深圳市福田区CBD中心',      '91440300MA5G7777G7', '招商银行福田支行',     '7559012345678902', '2026-06-05 10:00:00');

-- 新增发票（12张，6月中下旬）
INSERT INTO t_invoice (id, user_id, invoice_no, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, category, status, verified, created_at) VALUES
(17, 2, 'INV-2026-0609', '2026-06-16', '深圳华创科技有限公司',   '深圳星辰科技有限公司', 88000.00,  11440.00, 99440.00,  '销售收入',   'VERIFIED', b'1', '2026-06-16 09:00:00'),
(18, 2, 'INV-2026-0610', '2026-06-17', '佛山顺德制造有限公司',   '深圳星辰科技有限公司', 35000.00,  4550.00,  39550.00,  '销售收入',   'VERIFIED', b'1', '2026-06-17 10:30:00'),
(19, 2, 'INV-2026-0611', '2026-06-18', '惠州恒信实业有限公司',   '深圳星辰科技有限公司', 22000.00,  2860.00,  24860.00,  '服务收入',   'VERIFIED', b'1', '2026-06-18 14:00:00'),
(20, 2, 'INV-2026-0612', '2026-06-19', '中石化深圳分公司',       '深圳星辰科技有限公司', 4800.00,   624.00,   5424.00,   '交通费用',   'VERIFIED', b'1', '2026-06-19 08:30:00'),
(21, 2, 'INV-2026-0613', '2026-06-20', '深圳万象城物业管理',     '深圳星辰科技有限公司', 12000.00,  1560.00,  13560.00,  '办公费用',   'VERIFIED', b'1', '2026-06-20 11:00:00'),
(22, 2, 'INV-2026-0614', '2026-06-21', '东莞鑫源制造有限公司',   '深圳星辰科技有限公司', 56000.00,  7280.00,  63280.00,  '销售收入',   'PENDING',  NULL, '2026-06-21 15:00:00'),
(23, 2, 'INV-2026-0615', '2026-06-22', '广州明达电子有限公司',   '深圳星辰科技有限公司', 41000.00,  5330.00,  46330.00,  '销售收入',   'PENDING',  NULL, '2026-06-22 10:00:00'),
(24, 2, 'INV-2026-0616', '2026-06-23', '阿里云计算有限公司',     '深圳星辰科技有限公司', 9800.00,   1274.00,  11074.00,  '技术服务',   'PENDING',  NULL, '2026-06-23 09:30:00'),
(25, 2, 'INV-2026-0617', '2026-06-24', '深圳华创科技有限公司',   '深圳星辰科技有限公司', 67000.00,  8710.00,  75710.00,  '销售收入',   'VERIFIED', b'1', '2026-06-24 14:00:00'),
(26, 2, 'INV-2026-0618', '2026-06-25', '珠海蓝海贸易有限公司',   '深圳星辰科技有限公司', 28000.00,  3640.00,  31640.00,  '服务收入',   'PENDING',  NULL, '2026-06-25 16:00:00'),
(27, 2, 'INV-2026-0619', '2026-06-26', '美团企业版',             '深圳星辰科技有限公司', 3200.00,   416.00,   3616.00,   '餐饮费用',   'VERIFIED', b'1', '2026-06-26 12:00:00'),
(28, 2, 'INV-2026-0620', '2026-06-28', '深圳中正会计师事务所',   '深圳星辰科技有限公司', 15000.00,  1950.00,  16950.00,  '审计费用',   'PENDING',  NULL, '2026-06-28 09:00:00');

-- 新增收支流水（16笔）
INSERT INTO t_transaction (id, user_id, type, amount, category, description, transaction_date, counterparty, created_at) VALUES
(24, 2, 'INCOME',   88000.00, '销售收入', '产品销售回款-华创科技6月第二批',   '2026-06-16', '深圳华创科技有限公司',   '2026-06-16 10:00:00'),
(25, 2, 'INCOME',   35000.00, '销售收入', '产品销售回款-佛山顺德6月',         '2026-06-17', '佛山顺德制造有限公司',   '2026-06-17 11:00:00'),
(26, 2, 'INCOME',   22000.00, '服务收入', '财务顾问费-恒信实业6月',           '2026-06-18', '惠州恒信实业有限公司',   '2026-06-18 14:30:00'),
(27, 2, 'INCOME',   56000.00, '销售收入', '产品销售回款-鑫源制造6月',         '2026-06-21', '东莞鑫源制造有限公司',   '2026-06-21 15:30:00'),
(28, 2, 'INCOME',   41000.00, '销售收入', '产品销售回款-明达电子6月第二批',   '2026-06-22', '广州明达电子有限公司',   '2026-06-22 10:30:00'),
(29, 2, 'INCOME',   67000.00, '销售收入', '产品销售回款-华创科技6月第三批',   '2026-06-24', '深圳华创科技有限公司',   '2026-06-24 14:30:00'),
(30, 2, 'INCOME',   28000.00, '服务收入', '技术咨询服务费-蓝海贸易6月',       '2026-06-25', '珠海蓝海贸易有限公司', '2026-06-25 16:30:00'),
(31, 2, 'INCOME',   15000.00, '其他收入', '办公室转租收入-6月',               '2026-06-26', '深圳创想空间有限公司', '2026-06-26 09:00:00'),
(32, 2, 'EXPENSE',  12000.00, '办公费用', '办公室物业费-6月',                 '2026-06-20', '深圳万象城物业管理',     '2026-06-20 11:30:00'),
(33, 2, 'EXPENSE',   4800.00, '交通费用', '公司车辆加油-6月',                 '2026-06-19', '中石化深圳分公司',       '2026-06-19 09:00:00'),
(34, 2, 'EXPENSE',   9800.00, '技术服务', '阿里云服务器-6月',                 '2026-06-23', '阿里云计算有限公司',     '2026-06-23 10:00:00'),
(35, 2, 'EXPENSE',   3200.00, '餐饮费用', '员工团建聚餐-6月',                 '2026-06-26', '美团企业版',             '2026-06-26 12:30:00'),
(36, 2, 'EXPENSE',  15000.00, '审计费用', '半年度审计费',                     '2026-06-28', '深圳中正会计师事务所',   '2026-06-28 09:30:00'),
(37, 2, 'EXPENSE',   6500.00, '差旅费用', '珠海客户拜访差旅',                 '2026-06-25', '携程旅行网',             '2026-06-25 17:00:00'),
(38, 2, 'EXPENSE',   2800.00, '物流费用', '产品发货运费-6月第二批',           '2026-06-22', '顺丰速运有限公司',       '2026-06-22 16:00:00'),
(39, 2, 'EXPENSE',   1800.00, '办公费用', '办公用品补货-6月下旬',             '2026-06-27', '京东世纪贸易有限公司',   '2026-06-27 14:00:00');

-- 新增凭证（8张）
INSERT INTO t_voucher (id, user_id, voucher_no, type, amount, category, description, voucher_date, counterparty, has_invoice, status, created_at) VALUES
(12, 2, 'PZ-2026-0607', 'RECEIPT',  88000.00, '销售收入', '收到华创科技第二批货款',   '2026-06-16', '深圳华创科技有限公司', b'1', 'POSTED', '2026-06-16 10:30:00'),
(13, 2, 'PZ-2026-0608', 'RECEIPT',  35000.00, '销售收入', '收到佛山顺德货款',         '2026-06-17', '佛山顺德制造有限公司',   b'1', 'POSTED', '2026-06-17 11:30:00'),
(14, 2, 'PZ-2026-0609', 'RECEIPT',  22000.00, '服务收入', '收到恒信实业咨询费',       '2026-06-18', '惠州恒信实业有限公司',   b'1', 'POSTED', '2026-06-18 15:00:00'),
(15, 2, 'PZ-2026-0610', 'PAYMENT',  12000.00, '办公费用', '支付办公室物业费',         '2026-06-20', '深圳万象城物业管理',     b'1', 'POSTED', '2026-06-20 11:30:00'),
(16, 2, 'PZ-2026-0611', 'RECEIPT',  56000.00, '销售收入', '收到鑫源制造货款',         '2026-06-21', '东莞鑫源制造有限公司',   b'1', 'POSTED', '2026-06-21 16:00:00'),
(17, 2, 'PZ-2026-0612', 'PAYMENT',   9800.00, '技术服务', '支付阿里云服务费',         '2026-06-23', '阿里云计算有限公司',     b'1', 'POSTED', '2026-06-23 10:30:00'),
(18, 2, 'PZ-2026-0613', 'RECEIPT',  67000.00, '销售收入', '收到华创科技第三批货款',   '2026-06-24', '深圳华创科技有限公司',   b'1', 'POSTED', '2026-06-24 15:00:00'),
(19, 2, 'PZ-2026-0614', 'PAYMENT',  15000.00, '审计费用', '支付半年度审计费',         '2026-06-28', '深圳中正会计师事务所',   b'1', 'DRAFT',  '2026-06-28 10:00:00');

-- 新增凭证分录（16条）
INSERT INTO t_voucher_entry (id, voucher_id, account_id, debit_amount, credit_amount, sort_order, summary) VALUES
(23, 12, 12,  88000.00, NULL,      1, '收到华创科技第二批货款-银行存款增加'),
(24, 12, 18,  NULL,      88000.00, 2, '收到华创科技第二批货款-主营业务收入'),
(25, 13, 12,  35000.00, NULL,      1, '收到佛山顺德货款-银行存款增加'),
(26, 13, 18,  NULL,      35000.00, 2, '收到佛山顺德货款-主营业务收入'),
(27, 14, 12,  22000.00, NULL,      1, '收到恒信实业咨询费-银行存款增加'),
(28, 14, 19,  NULL,      22000.00, 2, '收到恒信实业咨询费-其他业务收入'),
(29, 15, 20,  12000.00, NULL,      1, '办公室物业费-管理费用增加'),
(30, 15, 12,  NULL,      12000.00, 2, '办公室物业费-银行存款减少'),
(31, 16, 12,  56000.00, NULL,      1, '收到鑫源制造货款-银行存款增加'),
(32, 16, 18,  NULL,      56000.00, 2, '收到鑫源制造货款-主营业务收入'),
(33, 17, 20,   9800.00, NULL,      1, '阿里云服务费-管理费用增加'),
(34, 17, 12,  NULL,       9800.00, 2, '阿里云服务费-银行存款减少'),
(35, 18, 12,  67000.00, NULL,      1, '收到华创科技第三批货款-银行存款增加'),
(36, 18, 18,  NULL,      67000.00, 2, '收到华创科技第三批货款-主营业务收入'),
(37, 19, 20,  15000.00, NULL,      1, '半年度审计费-管理费用增加'),
(38, 19, 12,  NULL,      15000.00, 2, '半年度审计费-银行存款减少');

-- 新增银行流水（12笔）
INSERT INTO t_bank_transaction (id, user_id, transaction_date, summary, counterparty, debit_amount, credit_amount, balance, account_no, bank_name, status, created_at) VALUES
(17, 2, '2026-06-16', '货款收入', '深圳华创科技有限公司',   NULL,      88000.00, 1048500.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-16 10:00:00'),
(18, 2, '2026-06-17', '货款收入', '佛山顺德制造有限公司',   NULL,      35000.00, 1083500.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-17 11:00:00'),
(19, 2, '2026-06-18', '咨询收入', '惠州恒信实业有限公司',   NULL,      22000.00, 1105500.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-18 14:30:00'),
(20, 2, '2026-06-19', '车辆加油', '中石化深圳分公司',       4800.00,   NULL,     1100700.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-19 09:00:00'),
(21, 2, '2026-06-20', '物业费',   '深圳万象城物业管理',     12000.00,  NULL,     1088700.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-20 11:30:00'),
(22, 2, '2026-06-21', '货款收入', '东莞鑫源制造有限公司',   NULL,      56000.00, 1144700.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-21 15:30:00'),
(23, 2, '2026-06-22', '货款收入', '广州明达电子有限公司',   NULL,      41000.00, 1185700.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2026-06-22 10:30:00'),
(24, 2, '2026-06-23', '云服务费', '阿里云计算有限公司',     9800.00,   NULL,     1175900.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-23 10:00:00'),
(25, 2, '2026-06-24', '货款收入', '深圳华创科技有限公司',   NULL,      67000.00, 1242900.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-24 14:30:00'),
(26, 2, '2026-06-25', '咨询收入', '珠海蓝海贸易有限公司',   NULL,      28000.00, 1270900.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2026-06-25 16:30:00'),
(27, 2, '2026-06-26', '团建聚餐', '美团企业版',             3200.00,   NULL,     1267700.00, '4000123456789012345', '工商银行深圳南山支行', 'MATCHED',   '2026-06-26 12:30:00'),
(28, 2, '2026-06-28', '审计费',   '深圳中正会计师事务所',   15000.00,  NULL,     1252700.00, '4000123456789012345', '工商银行深圳南山支行', 'UNMATCHED', '2026-06-28 10:00:00');

-- 新增费用报销（6笔）
INSERT INTO t_expense (id, user_id, title, applicant, category, amount, expense_date, description, invoice_path, status, approver, approval_opinion, approved_at, created_at, updated_at) VALUES
(10, 2, '6月深圳展会布展费',   '张会计', '展览费用', 8500.00,  '2026-06-15', '深圳国际会展中心展位搭建',     NULL, 'APPROVED', '李明', '同意报销', '2026-06-16 10:00:00', '2026-06-15 09:00:00', '2026-06-16 10:00:00'),
(11, 2, '6月珠海出差报销',     '张会计', '差旅费用', 6200.00,  '2026-06-25', '珠海蓝海贸易现场审计差旅',     NULL, 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-25 17:30:00', '2026-06-25 17:30:00'),
(12, 2, '6月员工团建费用',     '张会计', '团建费用', 4800.00,  '2026-06-26', '部门聚餐+KTV团建活动',         NULL, 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-26 13:00:00', '2026-06-26 13:00:00'),
(13, 2, '6月办公用品补货',     '张会计', '办公费用', 1800.00,  '2026-06-27', '硒鼓、墨盒、A4纸采购',         NULL, 'APPROVED', '李明', '同意报销', '2026-06-28 09:00:00', '2026-06-27 14:30:00', '2026-06-28 09:00:00'),
(14, 2, '6月车辆油费报销',     '张会计', '交通费用', 960.00,   '2026-06-19', '公司商务车加油2次',            NULL, 'APPROVED', '李明', '同意报销', '2026-06-20 10:00:00', '2026-06-19 18:00:00', '2026-06-20 10:00:00'),
(15, 2, '6月快递物流费',       '张会计', '办公费用', 1250.00,  '2026-06-22', '产品样品寄送+合同快递',        NULL, 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-22 17:00:00', '2026-06-22 17:00:00');

-- 新增预算（3项）
INSERT INTO t_budget (id, user_id, year, month, department, category, budget_amount, used_amount, alert_threshold, description, created_at, updated_at) VALUES
(10, 2, 2026, 6, '销售部', '招待费用',   8000.00,   5680.00, 80, '6月客户招待预算',       '2026-06-01 09:00:00', '2026-06-20 09:00:00'),
(11, 2, 2026, 6, '管理部', '审计费用',   20000.00,  15000.00, 80, '6月半年度审计预算',     '2026-06-01 09:00:00', '2026-06-28 09:00:00'),
(12, 2, 2026, 6, '财务部', '团建费用',   5000.00,   4800.00, 80, '6月部门团建活动预算',   '2026-06-01 09:00:00', '2026-06-26 13:00:00');

-- 新增工资记录（2人）
INSERT INTO t_salary_record (id, user_id, month, employee_no, employee_name, department, position, base_salary, allowance, bonus, overtime, social_insurance, housing_fund, tax, other_deduction, gross_pay, net_pay, status, created_at) VALUES
(12, 2, '2026-06', 'EMP005', '刘实习', '财务部', '实习会计', 5000.00,  500.00, 0.00, 0.00, 600.00, 400.00, 0.00, 0.00, 5500.00, 4500.00, 'PAID', '2026-06-05 09:00:00'),
(13, 2, '2026-06', 'EMP006', '陈仓管', '仓储部', '仓管员',   6500.00,  800.00, 500.00, 600.00, 900.00, 600.00, 120.00, 0.00, 8400.00, 6180.00, 'PAID', '2026-06-05 09:00:00');

-- 新增固定资产（2项）
INSERT INTO t_fixed_asset (id, user_id, asset_code, asset_name, category, original_value, salvage_value, useful_life_months, monthly_depreciation, depreciated_months, accumulated_depreciation, purchase_date, depreciation_method, status, created_at) VALUES
(6, 2, 'FA-2026-001', '投影仪（爱普生商用）',   '电子设备', 6800.00, 340.00, 48, 134.58, 0, 0.00, '2026-06-10', 'STRAIGHT_LINE', 'IN_USE', '2026-06-10 09:00:00'),
(7, 2, 'FA-2026-002', '保险柜（得力大型）',     '办公家具', 2500.00, 125.00, 60, 39.58,  0, 0.00, '2026-06-15', 'STRAIGHT_LINE', 'IN_USE', '2026-06-15 09:00:00');

-- 新增预警消息（6条）
INSERT INTO t_alert (id, user_id, type, level, title, content, status, created_at) VALUES
(11, 2, 'TAX_RISK', 'WARNING', '进项发票认证到期',   '有3张进项发票将于本月底到期，请及时认证抵扣，逾期将无法抵扣。',         'UNREAD', '2026-06-18 09:00:00'),
(12, 2, 'BUDGET',   'WARNING', '招待费用预算预警',   '销售部招待费用已使用71%，接近预警阈值，请控制后续支出。',              'UNREAD', '2026-06-20 10:00:00'),
(13, 2, 'INVOICE',  'INFO',    '新发票待核验',       '新增5张发票待核验，其中2张金额超过5万元，请优先处理。',                'UNREAD', '2026-06-22 11:00:00'),
(14, 2, 'BANK',     'WARNING', '大额支出提醒',       '6月28日支付审计费15,000元，请确认是否已获授权审批。',                  'UNREAD', '2026-06-28 10:30:00'),
(15, 2, 'CASHFLOW', 'INFO',    '现金流预测正常',     '根据当前收支趋势，预计6月底账户余额约125万元，现金流健康。',          'READ',   '2026-06-25 09:00:00'),
(16, 2, 'COMPLIANCE','WARNING','个税申报截止提醒',   '6月个人所得税申报截止日为6月15日，请确保已完成全员申报。',            'UNREAD', '2026-06-14 08:00:00');

-- 新增AI聊天消息（6条）
INSERT INTO t_chat_message (id, user_id, session_id, role, content, created_at) VALUES
(13, 2, 'sess-acct-0602', 'user',      '帮我看看6月哪些客户回款最多？', '2026-06-28 14:00:00'),
(14, 2, 'sess-acct-0602', 'assistant', '根据6月份银行流水和交易记录，客户回款排名如下：\n1. 深圳华创科技有限公司：305,000元（3批次回款）\n2. 广州明达电子有限公司：139,000元（2批次回款）\n3. 东莞鑫源制造有限公司：74,500元\n4. 珠海蓝海贸易有限公司：104,000元（含咨询费）\n5. 佛山顺德制造有限公司：35,000元\n6. 惠州恒信实业有限公司：40,000元\n\n华创科技是最大的回款客户，占总回款额的约37%。建议维护好与华创科技的合作关系。', '2026-06-28 14:00:10'),
(15, 2, 'sess-acct-0602', 'user',      '6月的利润率怎么样？和上月比呢？', '2026-06-28 14:02:00'),
(16, 2, 'sess-acct-0602', 'assistant', '6月份利润率分析：\n\n**6月汇总：**\n- 营业收入：约854,000元\n- 营业支出：约198,000元（含工资、办公、差旅、审计等）\n- 净利润：约656,000元\n- 净利润率：约76.8%\n\n**对比5月：**\n- 5月营业收入约680,000元，6月环比增长约25.6%\n- 6月利润率略有下降（5月约79.2%），主要因为审计费和差旅费增加\n\n**建议：**\n1. 审计费为一次性支出，下月不会重复\n2. 差旅费增长较快，建议加强出差审批管理\n3. 整体盈利能力依然强劲', '2026-06-28 14:02:12'),
(17, 2, 'sess-acct-0602', 'user',      '有没有什么税务风险需要提醒的？', '2026-06-28 14:05:00'),
(18, 2, 'sess-acct-0602', 'assistant', '6月税务风险提醒：\n\n1. **增值税税负率偏低**：本月税负率约1.15%，低于行业平均2.0%，建议核查进项发票明细\n2. **进项发票认证**：有3张进项发票月底到期，请及时认证\n3. **个税申报**：6月15日是本月个税申报截止日，请确认已完成\n4. **大额审计费**：15,000元审计费可取得增值税专用发票进行抵扣\n5. **关联交易**：华创科技占收入比例较高（约37%），注意关联交易定价合理性\n\n建议本周内完成以上事项，避免税务风险。', '2026-06-28 14:05:15');


-- ============================================================
-- 数据统计
-- ============================================================
SELECT '=== 数据统计 ===' AS info;
SELECT 't_user' AS table_name, COUNT(*) AS cnt FROM t_user
UNION ALL SELECT 't_company', COUNT(*) FROM t_company
UNION ALL SELECT 't_account', COUNT(*) FROM t_account
UNION ALL SELECT 't_contact', COUNT(*) FROM t_contact
UNION ALL SELECT 't_voucher', COUNT(*) FROM t_voucher
UNION ALL SELECT 't_voucher_entry', COUNT(*) FROM t_voucher_entry
UNION ALL SELECT 't_voucher_template', COUNT(*) FROM t_voucher_template
UNION ALL SELECT 't_invoice', COUNT(*) FROM t_invoice
UNION ALL SELECT 't_bank_transaction', COUNT(*) FROM t_bank_transaction
UNION ALL SELECT 't_fixed_asset', COUNT(*) FROM t_fixed_asset
UNION ALL SELECT 't_salary_record', COUNT(*) FROM t_salary_record
UNION ALL SELECT 't_transaction', COUNT(*) FROM t_transaction
UNION ALL SELECT 't_budget', COUNT(*) FROM t_budget
UNION ALL SELECT 't_expense', COUNT(*) FROM t_expense
UNION ALL SELECT 't_alert', COUNT(*) FROM t_alert
UNION ALL SELECT 't_chat_message', COUNT(*) FROM t_chat_message;
