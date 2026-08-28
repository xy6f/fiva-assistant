DROP DATABASE IF EXISTS `fiva_assistant`;
CREATE DATABASE `fiva_assistant` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `fiva_assistant`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
  `year` int NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `content` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  UNIQUE KEY `UKjkoh62aj4rx1jgmbrunqj7byy` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE `t_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_no` varchar(50) DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `seller_name` varchar(200) DEFAULT NULL,
  `buyer_name` varchar(200) DEFAULT NULL,
  `amount` decimal(15,2) DEFAULT NULL,
  `tax_amount` decimal(15,2) DEFAULT NULL,
  `total_amount` decimal(15,2) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `ocr_raw_text` text,
  `status` varchar(20) DEFAULT 'PENDING',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  `check_code` varchar(20) DEFAULT NULL,
  `invoice_code` varchar(50) DEFAULT NULL,
  `verified` bit(1) DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `verify_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `company_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjhib4legehrm4yscx9t3lirqi` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  `voucher_date` date NOT NULL,
  `voucher_no` varchar(30) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8iauiayny29ybtsh3w5nsjamu` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

INSERT IGNORE INTO t_user (username, password, real_name, role, email, phone, department, enabled, created_at)
VALUES ('viewer1', '$2a$10$...', '赵强', 'VIEWER', 'zhaoqiang@example.com', '13800003333', '审计部', 1, NOW());

INSERT IGNORE INTO t_user (username, password, real_name, role, email, phone, department, enabled, created_at)
VALUES ('cashier1', '$2a$10$...', '孙丽', 'ACCOUNTANT', 'sunli@example.com', '13800004444', '财务部', 1, NOW());

INSERT IGNORE INTO t_company (user_id, name, legal_person, tax_no, address, phone, bank_name, bank_account, created_at, updated_at)
VALUES (3, '深圳星河信息技术有限公司', '陈刚', '91440300MA02AB345C', '深圳市南山区科技园路88号', '0755-86881234', '招商银行深圳分行', '755928123456701', NOW(), NOW());

INSERT INTO t_contact (user_id, name, type, contact_person, phone, email, address, tax_no, bank_name, bank_account, remark, created_at, updated_at)
VALUES
(1, '杭州华联供应链有限公司', 'SUPPLIER', '周经理', '0571-88765432', 'zhou@hualian.com', '杭州市西湖区文三路100号', '91330100MA01BC234D', '工商银行杭州分行', '3301012609300001234', '主要供应商', NOW(), NOW()),
(1, '广州天河商贸有限公司', 'CUSTOMER', '李总', '020-38765432', 'li@tianhe.com', '广州市天河区体育西路200号', '91440100MA01CD345E', '建设银行广州分行', '4401012609300002345', '大客户', NOW(), NOW()),
(2, '北京中关村电子有限公司', 'SUPPLIER', '王工', '010-82765432', 'wang@zgc.com', '北京市海淀区中关村大街300号', '91110100MA01DE456F', '农业银行北京分行', '1101012609300003456', '电子设备供应商', NOW(), NOW()),
(2, '上海浦东国际贸易有限公司', 'CUSTOMER', '张经理', '021-58765432', 'zhang@pudong.com', '上海市浦东新区陆家嘴环路400号', '91310100MA01EF567G', '中国银行上海分行', '3101012609300004567', '长期合作客户', NOW(), NOW());

INSERT INTO t_voucher (user_id, voucher_no, voucher_date, type, category, amount, description, counterparty, status, has_invoice, created_at)
VALUES
(1, 'PZ-2025-0009', '2025-01-15', 'PAYMENT', '采购', 58500.00, '采购办公用品', '杭州华联供应链有限公司', 'POSTED', 1, NOW()),
(1, 'PZ-2025-0010', '2025-02-10', 'RECEIPT', '销售', 113000.00, '销售商品收入', '广州天河商贸有限公司', 'POSTED', 1, NOW()),
(1, 'PZ-2025-0011', '2025-03-05', 'PAYMENT', '费用', 12000.00, '支付办公室水电费', '国家电网', 'POSTED', 0, NOW()),
(1, 'PZ-2025-0012', '2025-04-20', 'TRANSFER', '调账', 200000.00, '银行存款转定期', NULL, 'POSTED', 0, NOW()),
(3, 'PZ-2025-0013', '2025-05-08', 'PAYMENT', '采购', 35000.00, '采购服务器设备', '北京中关村电子有限公司', 'DRAFT', 1, NOW()),
(3, 'PZ-2025-0014', '2025-06-01', 'RECEIPT', '销售', 89000.00, '软件服务收入', '上海浦东国际贸易有限公司', 'POSTED', 1, NOW());

INSERT INTO t_voucher_entry (voucher_id, account_id, summary, debit_amount, credit_amount, sort_order) VALUES
(9, 5, '采购办公用品', 50000.00, NULL, 1),
(9, 2, '银行存款支付', NULL, 50000.00, 2),
(9, 10, '进项税额', 8500.00, NULL, 3),
(10, 2, '收到货款', 113000.00, NULL, 1),
(10, 14, '主营业务收入', NULL, 100000.00, 2),
(10, 10, '销项税额', NULL, 13000.00, 3),
(11, 18, '管理费用-水电费', 12000.00, NULL, 1),
(11, 2, '银行存款支付', NULL, 12000.00, 2),
(12, 3, '定期存款', 200000.00, NULL, 1),
(12, 2, '活期转出', NULL, 200000.00, 2),
(13, 6, '固定资产-服务器', 30973.45, NULL, 1),
(13, 10, '进项税额', 4026.55, NULL, 2),
(13, 2, '银行存款', NULL, 35000.00, 3),
(14, 2, '收到服务费', 89000.00, NULL, 1),
(14, 14, '主营业务收入', NULL, 80000.00, 2),
(14, 10, '销项税额', NULL, 9000.00, 3);

INSERT INTO t_voucher_template (user_id, name, type, description, entries_json, created_at, updated_at)
VALUES
(1, '采购入库模板', 'PURCHASE', '标准采购入库凭证模板', '[{"accountId":5,...}]', NOW(), NOW()),
(3, '差旅费报销模板', 'EXPENSE', '员工差旅费报销模板', '[{"accountId":18,...}]', NOW(), NOW());

INSERT INTO t_invoice (user_id, invoice_no, invoice_code, invoice_date, seller_name, buyer_name, amount, tax_amount, total_amount, category, status, check_code, verified, verify_message, created_at)
VALUES
(1, '01234567', '031001900111', '2025-01-15', '杭州华联供应链有限公司', '北京星辰科技有限公司', 50000.00, 8500.00, 58500.00, '采购', 'VERIFIED', '123456', 1, '发票要素齐全', NOW()),
(1, '02345678', '031001900222', '2025-02-10', '北京星辰科技有限公司', '广州天河商贸有限公司', 100000.00, 13000.00, 113000.00, '销售', 'VERIFIED', '234567', 1, '发票要素齐全', NOW()),
(1, '03456789', '031001900333', '2025-03-05', '国家电网有限公司', '北京星辰科技有限公司', 11320.75, 679.25, 12000.00, '费用', 'PENDING', NULL, 0, NULL, NOW()),
(3, '04567890', '031001900444', '2025-05-08', '北京中关村电子有限公司', '深圳星河信息技术有限公司', 30973.45, 4026.55, 35000.00, '采购', 'PENDING', NULL, 0, NULL, NOW()),
(3, '05678901', '031001900555', '2025-06-01', '深圳星河信息技术有限公司', '上海浦东国际贸易有限公司', 80000.00, 9000.00, 89000.00, '销售', 'VERIFIED', '567890', 1, '发票要素齐全', NOW()),
(1, '06789012', '031001900666', '2025-06-10', '北京京东世纪贸易有限公司', '北京星辰科技有限公司', 5600.00, 728.00, 6328.00, '采购', 'PENDING', NULL, 0, NULL, NOW());

INSERT INTO t_bank_transaction (user_id, bank_name, account_no, transaction_date, counterparty, summary, debit_amount, credit_amount, balance, status, created_at)
VALUES
(1, '工商银行北京分行', '1001234567890', '2025-01-20', '杭州华联供应链有限公司', '采购办公用品货款', NULL, 58500.00, 441500.00, 'MATCHED', NOW()),
(1, '工商银行北京分行', '1001234567890', '2025-02-15', '广州天河商贸有限公司', '销售商品收款', 113000.00, NULL, 554500.00, 'MATCHED', NOW()),
(1, '工商银行北京分行', '1001234567890', '2025-03-10', '国家电网', '3月电费', NULL, 12000.00, 542500.00, 'MATCHED', NOW()),
(1, '工商银行北京分行', '1001234567890', '2025-04-25', '北京星辰科技有限公司', '工资发放-4月', NULL, 156000.00, 386500.00, 'UNMATCHED', NOW()),
(3, '招商银行深圳分行', '755928123456701', '2025-05-10', '北京中关村电子有限公司', '服务器设备款', NULL, 35000.00, 265000.00, 'MATCHED', NOW()),
(3, '招商银行深圳分行', '755928123456701', '2025-06-05', '上海浦东国际贸易有限公司', '软件服务费收款', 89000.00, NULL, 354000.00, 'MATCHED', NOW()),
(1, '工商银行北京分行', '1001234567890', '2025-06-12', '北京京东世纪贸易有限公司', '办公用品采购', NULL, 6328.00, 380172.00, 'UNMATCHED', NOW());

INSERT INTO t_fixed_asset (user_id, asset_code, asset_name, category, purchase_date, original_value, salvage_value, depreciation_method, useful_life_months, monthly_depreciation, accumulated_depreciation, depreciated_months, status, created_at)
VALUES
(1, 'FA-2025-006', '中央空调系统', '电子设备', '2025-01-10', 120000.00, 6000.00, 'STRAIGHT_LINE', 120, 950.00, 4750.00, 5, 'IN_USE', NOW()),
(1, 'FA-2025-007', '办公桌椅(批量)', '办公家具', '2025-03-15', 35000.00, 1750.00, 'STRAIGHT_LINE', 60, 554.17, 1662.51, 3, 'IN_USE', NOW()),
(3, 'FA-2025-008', 'Dell PowerEdge R750服务器', '电子设备', '2025-05-01', 85000.00, 4250.00, 'DOUBLE_DECLINING', 60, 2833.33, 5666.66, 2, 'IN_USE', NOW());

INSERT INTO t_salary_record (user_id, employee_name, employee_no, department, position, month, base_salary, bonus, allowance, overtime, gross_pay, social_insurance, housing_fund, tax, other_deduction, net_pay, status, created_at)
VALUES
(1, '张伟', 'EMP001', '管理部', '总经理', '2025-01', 25000.00, 5000.00, 2000.00, 0.00, 32000.00, 4500.00, 3200.00, 2890.00, 500.00, 20910.00, 'POSTED', NOW()),
(1, '王芳', 'EMP002', '财务部', '财务主管', '2025-01', 18000.00, 3000.00, 1500.00, 800.00, 23300.00, 3200.00, 2300.00, 1130.00, 300.00, 16370.00, 'POSTED', NOW()),
(1, '李明', 'EMP003', '技术部', '高级工程师', '2025-01', 20000.00, 4000.00, 1500.00, 1200.00, 26700.00, 3800.00, 2600.00, 1690.00, 400.00, 18210.00, 'POSTED', NOW()),
(1, '赵强', 'EMP004', '销售部', '销售经理', '2025-01', 15000.00, 8000.00, 1000.00, 0.00, 24000.00, 3000.00, 2100.00, 1380.00, 200.00, 17320.00, 'POSTED', NOW()),
(1, '孙丽', 'EMP005', '财务部', '会计', '2025-01', 12000.00, 2000.00, 1000.00, 500.00, 15500.00, 2200.00, 1500.00, 430.00, 100.00, 11270.00, 'POSTED', NOW()),
(3, '陈刚', 'EMP006', '管理部', '总经理', '2025-06', 28000.00, 6000.00, 2000.00, 0.00, 36000.00, 5000.00, 3500.00, 3590.00, 500.00, 23410.00, 'DRAFT', NOW()),
(3, '刘洋', 'EMP007', '技术部', '架构师', '2025-06', 30000.00, 5000.00, 2000.00, 1500.00, 38500.00, 5500.00, 3800.00, 4350.00, 600.00, 24250.00, 'DRAFT', NOW()),
(3, '周杰', 'EMP008', '销售部', '销售代表', '2025-06', 10000.00, 6000.00, 800.00, 0.00, 16800.00, 2400.00, 1600.00, 520.00, 100.00, 12180.00, 'DRAFT', NOW()),
(3, '吴敏', 'EMP009', '人事部', 'HR专员', '2025-06', 11000.00, 1500.00, 800.00, 300.00, 13600.00, 1900.00, 1300.00, 310.00, 100.00, 9990.00, 'DRAFT', NOW()),
(3, '郑浩', 'EMP010', '技术部', '测试工程师', '2025-06', 15000.00, 3000.00, 1200.00, 800.00, 20000.00, 2800.00, 2000.00, 910.00, 300.00, 13990.00, 'DRAFT', NOW());

INSERT INTO t_transaction (user_id, transaction_date, type, category, amount, counterparty, description, created_at)
VALUES
(1, '2025-01-15', 'EXPENSE', '采购', 58500.00, '杭州华联供应链有限公司', '采购办公用品', NOW()),
(1, '2025-02-10', 'INCOME', '销售', 113000.00, '广州天河商贸有限公司', '商品销售收入', NOW()),
(1, '2025-03-05', 'EXPENSE', '费用', 12000.00, '国家电网', '办公室水电费', NOW()),
(1, '2025-04-25', 'EXPENSE', '工资', 156000.00, NULL, '4月工资发放', NOW()),
(3, '2025-05-10', 'EXPENSE', '采购', 35000.00, '北京中关村电子有限公司', '服务器设备采购', NOW()),
(3, '2025-06-05', 'INCOME', '销售', 89000.00, '上海浦东国际贸易有限公司', '软件服务收入', NOW());

INSERT INTO t_budget (user_id, year, month, department, category, budget_amount, used_amount, alert_threshold, description, created_at, updated_at)
VALUES
(1, 2025, 1, '管理部', '办公费', 20000.00, 15600.00, 80, '1月办公费预算', NOW(), NOW()),
(1, 2025, 2, '管理部', '办公费', 20000.00, 18200.00, 80, '2月办公费预算', NOW(), NOW()),
(1, 2025, 3, '技术部', '研发费', 100000.00, 76000.00, 85, '3月研发预算', NOW(), NOW()),
(1, 2025, 4, '销售部', '差旅费', 30000.00, 28500.00, 90, '4月销售差旅', NOW(), NOW()),
(3, 2025, 6, '技术部', '设备采购', 200000.00, 35000.00, 80, '6月设备采购预算', NOW(), NOW()),
(3, 2025, 6, '管理部', '日常运营', 50000.00, 32000.00, 85, '6月日常运营预算', NOW(), NOW());

INSERT INTO t_expense (user_id, title, applicant, category, amount, expense_date, description, status, invoice_path, approver, approval_opinion, created_at, updated_at)
VALUES
(1, '1月办公用品采购', '王芳', '办公费', 3500.00, '2025-01-18', '采购打印纸、墨盒等', 'APPROVED', '/uploads/expense/001.jpg', '张伟', '同意报销', NOW(), NOW()),
(1, '2月客户招待费', '赵强', '招待费', 2800.00, '2025-02-20', '招待广州天河商贸客户', 'APPROVED', '/uploads/expense/002.jpg', '张伟', '同意，注意控制额度', NOW(), NOW()),
(1, '3月差旅费-上海出差', '李明', '差旅费', 5600.00, '2025-03-15', '上海客户技术交流出差3天', 'APPROVED', '/uploads/expense/003.jpg', '张伟', '同意报销', NOW(), NOW()),
(3, '6月服务器维护费', '刘洋', '维护费', 8000.00, '2025-06-10', 'Dell服务器年度维保', 'PENDING', '/uploads/expense/004.jpg', NULL, NULL, NOW(), NOW()),
(3, '6月团建费用', '吴敏', '活动费', 4500.00, '2025-06-12', '部门季度团建活动', 'PENDING', '/uploads/expense/005.jpg', NULL, NULL, NOW(), NOW());

INSERT INTO t_alert (user_id, type, level, title, content, status, related_id, created_at)
VALUES
(1, 'BUDGET_WARNING', 'WARNING', '预算预警', '4月销售部差旅费预算使用率已达95%', 'UNREAD', 12, NOW()),
(1, 'INVOICE_ALERT', 'INFO', '发票到期提醒', '发票号03456789已开具超过90天', 'UNREAD', 9, NOW()),
(3, 'BUDGET_WARNING', 'WARNING', '预算预警', '6月技术部设备采购预算使用率偏低', 'READ', 17, NOW()),
(1, 'ASSET_ALERT', 'INFO', '资产折旧提醒', '本月需计提固定资产折旧', 'UNREAD', NULL, NOW());

INSERT INTO t_chat_message (user_id, session_id, role, content, created_at)
VALUES
(1, 'session-001', 'user', '帮我分析一下本月的收支情况', NOW()),
(1, 'session-001', 'assistant', '根据系统数据，本月总收入为202,000元，总支出为226,500元...', NOW()),
(1, 'session-001', 'user', '下个月有哪些应付账款？', NOW()),
(1, 'session-001', 'assistant', '根据记录，下月主要应付款项：1. 杭州华联供应链 58,500元...', NOW()),
(3, 'session-002', 'user', '如何优化固定资产折旧方案？', NOW()),
(3, 'session-002', 'assistant', '建议：1. 电子设备可采用双倍余额递减法...', NOW()),
(3, 'session-002', 'user', '帮我生成本月工资计提凭证', NOW()),
(3, 'session-002', 'assistant', '已为您生成2025年6月工资计提凭证...', NOW()),
(1, 'session-003', 'user', '最近有什么税务风险需要注意？', NOW()),
(1, 'session-003', 'assistant', '提醒：1. 增值税发票需在360天内认证...', NOW());

SET FOREIGN_KEY_CHECKS = 1;