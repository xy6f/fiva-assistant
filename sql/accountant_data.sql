-- ============================================================
-- accountant (user_id=2) complete test data
-- Usage: mysql -u root -p fiva_assistant < accountant_data.sql
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- Part 0: Create tables (IF NOT EXISTS)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_user` (
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

CREATE TABLE IF NOT EXISTS `t_company` (
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

CREATE TABLE IF NOT EXISTS `t_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `balance_direction` varchar(10) NOT NULL DEFAULT 'DEBIT',
  `code` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) DEFAULT b'1',
  `name` varchar(100) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_contact` (
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

CREATE TABLE IF NOT EXISTS `t_voucher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `counterparty` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `has_invoice` bit(1) DEFAULT b'0',
  `invoice_image_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'DRAFT',
  `type` varchar(10) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `voucher_date` date NOT NULL,
  `voucher_no` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_voucher_no` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_voucher_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `credit_amount` decimal(15,2) DEFAULT 0.00,
  `debit_amount` decimal(15,2) DEFAULT 0.00,
  `sort_order` int DEFAULT 0,
  `summary` varchar(500) DEFAULT NULL,
  `voucher_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) DEFAULT NULL,
  `buyer_name` varchar(200) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `check_code` varchar(20) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `invoice_code` varchar(50) DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `invoice_no` varchar(50) DEFAULT NULL,
  `ocr_raw_text` text,
  `seller_name` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `tax_amount` decimal(15,2) DEFAULT NULL,
  `total_amount` decimal(15,2) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `verified` bit(1) DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `verify_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_transaction` (
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

CREATE TABLE IF NOT EXISTS `t_bank_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_no` varchar(50) DEFAULT NULL,
  `balance` decimal(15,2) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `counterparty` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `credit_amount` decimal(15,2) DEFAULT 0.00,
  `debit_amount` decimal(15,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'UNMATCHED',
  `summary` varchar(500) DEFAULT NULL,
  `transaction_date` date DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `voucher_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_expense` (
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
  `status` varchar(20) DEFAULT 'PENDING',
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_salary_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `allowance` decimal(15,2) DEFAULT 0.00,
  `base_salary` decimal(15,2) DEFAULT 0.00,
  `bonus` decimal(15,2) DEFAULT 0.00,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `employee_name` varchar(100) NOT NULL,
  `employee_no` varchar(50) DEFAULT NULL,
  `gross_pay` decimal(15,2) DEFAULT 0.00,
  `housing_fund` decimal(15,2) DEFAULT 0.00,
  `month` varchar(10) NOT NULL,
  `net_pay` decimal(15,2) DEFAULT 0.00,
  `other_deduction` decimal(15,2) DEFAULT 0.00,
  `overtime` decimal(15,2) DEFAULT 0.00,
  `position` varchar(100) DEFAULT NULL,
  `social_insurance` decimal(15,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'DRAFT',
  `tax` decimal(15,2) DEFAULT 0.00,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_fixed_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint DEFAULT NULL,
  `accumulated_depreciation` decimal(15,2) DEFAULT 0.00,
  `asset_code` varchar(50) DEFAULT NULL,
  `asset_name` varchar(200) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `depreciated_months` int DEFAULT 0,
  `depreciation_account_id` bigint DEFAULT NULL,
  `depreciation_method` varchar(30) DEFAULT 'STRAIGHT_LINE',
  `expense_account_id` bigint DEFAULT NULL,
  `monthly_depreciation` decimal(15,2) DEFAULT NULL,
  `original_value` decimal(15,2) NOT NULL,
  `purchase_date` date DEFAULT NULL,
  `salvage_value` decimal(15,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'IN_USE',
  `useful_life_months` int DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_budget` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alert_threshold` int DEFAULT 80,
  `budget_amount` decimal(15,2) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `month` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `used_amount` decimal(15,2) DEFAULT 0.00,
  `user_id` bigint DEFAULT NULL,
  `year` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `level` varchar(20) NOT NULL DEFAULT 'NORMAL',
  `read_at` datetime(6) DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'UNREAD',
  `title` varchar(200) NOT NULL,
  `type` varchar(30) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_voucher_template` (
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

-- ============================================================
-- Part 1: User & Company
-- ============================================================

DELETE FROM `t_user` WHERE `id` = 2;
INSERT INTO `t_user` (`id`, `username`, `password`, `real_name`, `role`, `enabled`, `email`, `phone`, `department`, `company_id`, `created_at`)
VALUES (2, 'accountant', '$2a$10$4kWPeFZpTkvudqUGpAjkDO5RWltKu8fANRYO7uv24BOludRumjyjW', '张会计', 'ACCOUNTANT', b'1', 'zhang@fiva.com', '13800000002', '财务部', 2, '2025-01-01 09:00:00');

DELETE FROM `t_company` WHERE `user_id` = 2;
DELETE FROM `t_company` WHERE `id` = 2;
INSERT INTO `t_company` (`id`, `user_id`, `name`, `tax_no`, `address`, `phone`, `bank_name`, `bank_account`, `legal_person`, `created_at`)
VALUES (2, 2, '深圳市前海智联科技有限公司', '91440300MA5G1234AB', '深圳市南山区科技园南区B栋12楼', '0755-86001234', '中国工商银行深圳科技支行', '4000234567890123', '王建国', '2025-01-01 09:00:00');

-- ============================================================
-- Part 2: Accounts (22 subjects)
-- ============================================================

DELETE FROM `t_account` WHERE `user_id` = 2;

INSERT INTO `t_account` (`user_id`, `code`, `name`, `type`, `balance_direction`, `enabled`, `created_at`) VALUES
(2, '1001', '现金', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1002', '银行存款', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1122', '应收账款', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1221', '其他应收款', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1403', '原材料', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1601', '固定资产', 'ASSET', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '1602', '累计折旧', 'ASSET', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '2202', '应付账款', 'LIABILITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '2211', '应付职工薪酬', 'LIABILITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '2221', '应交税费', 'LIABILITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '4001', '实收资本', 'EQUITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '4101', '盈余公积', 'EQUITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '4104', '未分配利润', 'EQUITY', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '6001', '主营业务收入', 'REVENUE', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '6051', '其他业务收入', 'REVENUE', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '6301', '营业外收入', 'REVENUE', 'CREDIT', b'1', '2025-01-01 09:00:00'),
(2, '6401', '主营业务成本', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '6601', '销售费用', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '6602', '管理费用', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '6603', '财务费用', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '6711', '营业外支出', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00'),
(2, '6801', '所得税费用', 'EXPENSE', 'DEBIT', b'1', '2025-01-01 09:00:00');

-- ============================================================
-- Part 3: Contacts
-- ============================================================

DELETE FROM `t_contact` WHERE `user_id` = 2;

INSERT INTO `t_contact` (`user_id`, `name`, `type`, `contact_person`, `phone`, `email`, `address`, `tax_no`, `bank_name`, `bank_account`, `created_at`) VALUES
(2, '蓝海贸易', 'CUSTOMER', '李明', '13800001111', 'lh@bluetrade.com', '深圳市福田华强北路100号', '91440300MA987654CD', '招商银行华强支行', '6222801234567890', '2025-01-15 10:00:00'),
(2, '信达电子科技', 'CUSTOMER', '陈伟', '13900002222', 'xd@xindatech.com', '广州市天河天河路385号', '91440100MA5XYZ12EF', '建设银行天河支行', '6222802345678901', '2025-02-10 14:00:00'),
(2, '鸿图制造', 'CUSTOMER', '王芳', '13700003333', 'ht@hongtumfg.com', '东莞长安镇', '91441900MA5ABC12GH', '农业银行长安支行', '6222803456789012', '2025-03-05 09:30:00'),
(2, '顺德材料供应', 'SUPPLIER', '刘强', '13600004441', 'cs@shundematerial.com', '佛山顺德大良', '91440600MA5DEF12IJ', '中国银行顺德支行', '6222804567890123', '2025-01-20 11:00:00'),
(2, '华强电子市场', 'SUPPLIER', '赵敏', '13500005555', 'hq@huaqiang.com', '深圳市福田华强赛格广场', '91440300MA5GHI12KL', '交通银行华强支行', '6222805678901234', '2025-02-18 16:00:00'),
(2, '博达软件', 'SUPPLIER', '孙丽', '13400006666', 'bd@bodasoft.com', '珠海香洲情侣路', '91440400MA5MNO12PQ', '工商银行香洲支行', '6222806789012345', '2025-04-01 10:00:00'),
(2, '中科云联', 'SUPPLIER', '周杰', '13300007777', 'zk@zhongkeyun.com', '北京海淀中关村', '91110100MA5RST12UV', '工商银行海淀支行', '6222807890123456', '2025-05-12 09:00:00');

-- ============================================================
-- Part 4: Vouchers + Entries (Jan-Jun 2026)
-- ============================================================

DELETE FROM `t_voucher_entry` WHERE `voucher_id` IN (SELECT `id` FROM `t_voucher` WHERE `user_id` = 2);
DELETE FROM `t_voucher` WHERE `user_id` = 2;

INSERT INTO `t_voucher` (`id`, `user_id`, `voucher_no`, `type`, `amount`, `category`, `description`, `voucher_date`, `counterparty`, `status`, `has_invoice`, `created_at`) VALUES
(201, 2, 'PZ-2026-01-001', 'PAYMENT',  50000.00, '货款',   '收到蓝海货款',       '2026-01-05', '蓝海贸易', 'POSTED', b'1', '2026-01-05 10:00:00'),
(202, 2, 'PZ-2026-01-002', 'PAYMENT',  32000.00, '采购',    '支付顺德材料款',     '2026-01-12', '顺德材料', 'POSTED', b'1', '2026-01-12 14:00:00'),
(203, 2, 'PZ-2026-01-003', 'TRANSFER', 12000.00, '工资',    '发放1月工资',            '2026-01-25', NULL, 'POSTED', b'0', '2026-01-25 09:00:00'),
(204, 2, 'PZ-2026-01-004', 'TRANSFER',  3500.00, '费用',   '报销1月差旅费',        '2026-01-28', NULL, 'POSTED', b'1', '2026-01-28 16:00:00'),
(205, 2, 'PZ-2026-02-001', 'PAYMENT',  68000.00, '货款',   '收到信达货款',         '2026-02-08', '信达电子', 'POSTED', b'1', '2026-02-08 10:00:00'),
(206, 2, 'PZ-2026-02-002', 'PAYMENT',  15000.00, '采购',    '采购华强元器件',   '2026-02-15', '华强电子', 'POSTED', b'1', '2026-02-15 14:00:00'),
(207, 2, 'PZ-2026-02-003', 'TRANSFER', 12000.00, '工资',    '发放2月工资',            '2026-02-25', NULL, 'POSTED', b'0', '2026-02-25 09:00:00'),
(208, 2, 'PZ-2026-03-001', 'PAYMENT',  45000.00, '货款',   '收到鸿图货款',        '2026-03-10', '鸿图制造', 'POSTED', b'1', '2026-03-10 10:00:00'),
(209, 2, 'PZ-2026-03-002', 'PAYMENT',  28000.00, '采购',    '支付博达服务费',           '2026-03-18', '博达软件', 'POSTED', b'1', '2026-03-18 14:00:00'),
(210, 2, 'PZ-2026-03-003', 'TRANSFER', 12500.00, '工资',    '发放3月工资',            '2026-03-25', NULL, 'POSTED', b'0', '2026-03-25 09:00:00'),
(211, 2, 'PZ-2026-03-004', 'TRANSFER',  5000.00, '费用',   '报销3月展会费',      '2026-03-28', NULL, 'POSTED', b'1', '2026-03-28 16:00:00'),
(212, 2, 'PZ-2026-04-001', 'PAYMENT',  72000.00, '货款',   '收到蓝海货款',        '2026-04-06', '蓝海贸易', 'POSTED', b'1', '2026-04-06 10:00:00'),
(213, 2, 'PZ-2026-04-002', 'PAYMENT',  18500.00, '采购',    '采购原材料',            '2026-04-14', '顺德材料', 'POSTED', b'1', '2026-04-14 14:00:00'),
(214, 2, 'PZ-2026-04-003', 'TRANSFER', 13000.00, '工资',    '发放4月工资',            '2026-04-25', NULL, 'POSTED', b'0', '2026-04-25 09:00:00'),
(215, 2, 'PZ-2026-05-001', 'PAYMENT',  55000.00, '货款',   '收到信达货款',         '2026-05-08', '信达电子', 'POSTED', b'1', '2026-05-08 10:00:00'),
(216, 2, 'PZ-2026-05-002', 'PAYMENT',  42000.00, '采购',    '采购电子设备',           '2026-05-15', '华强电子', 'POSTED', b'1', '2026-05-15 14:00:00'),
(217, 2, 'PZ-2026-05-003', 'TRANSFER', 13500.00, '工资',    '发放5月工资',            '2026-05-25', NULL, 'POSTED', b'0', '2026-05-25 09:00:00'),
(218, 2, 'PZ-2026-05-004', 'TRANSFER',  8000.00, '费用',   '支付中科技术费',       '2026-05-28', '中科云联', 'POSTED', b'1', '2026-05-28 16:00:00'),
(219, 2, 'PZ-2026-06-001', 'PAYMENT',  85000.00, '货款',   '收到鸿图货款',        '2026-06-05', '鸿图制造', 'POSTED', b'1', '2026-06-05 10:00:00'),
(220, 2, 'PZ-2026-06-002', 'PAYMENT',  25000.00, '采购',    '采购原材料',            '2026-06-12', '顺德材料', 'POSTED', b'1', '2026-06-12 14:00:00'),
(221, 2, 'PZ-2026-06-003', 'TRANSFER', 14000.00, '工资',    '发放6月工资',            '2026-06-25', NULL, 'POSTED', b'0', '2026-06-25 09:00:00'),
(222, 2, 'PZ-2026-06-004', 'TRANSFER',  6500.00, '费用',   '报销6月差旅及招待', '2026-06-28', NULL, 'DRAFT', b'1', '2026-06-28 16:00:00');

INSERT INTO `t_voucher_entry` (`voucher_id`, `account_id`, `summary`, `debit_amount`, `credit_amount`, `sort_order`) VALUES
(201, 2,  '银行存款增加',     50000.00, 0,        0),
(201, 3,  '冲减应收',            0,        50000.00, 1),
(202, 8,  '应付材料款',           0,        32000.00, 0),
(202, 2,  '银行存款支付',        32000.00, 0,        1),
(203, 9,  '计提工资',                   12000.00, 0,        0),
(203, 2,  '银行代发',                 0,        12000.00, 1),
(204, 19, '差旅费报销',             3500.00,  0,        0),
(204, 2,  '现金报销',               0,        3500.00,  1),
(205, 2,  '银行存款增加',     68000.00, 0,        0),
(205, 3,  '冲减应收',            0,        68000.00, 1),
(206, 5,  '采购元器件',            15000.00, 0,        0),
(206, 2,  '银行存款支付',        0,        15000.00, 1),
(207, 9,  '计提工资',                   12000.00, 0,        0),
(207, 2,  '银行代发',                 0,        12000.00, 1),
(208, 2,  '银行存款增加',     45000.00, 0,        0),
(208, 3,  '冲减应收',            0,        45000.00, 1),
(209, 19, '软件服务费',             28000.00, 0,        0),
(209, 2,  '银行存款支付',        0,        28000.00, 1),
(210, 9,  '计提工资',                   12500.00, 0,        0),
(210, 2,  '银行代发',                 0,        12500.00, 1),
(211, 18, '展会费用',               5000.00,  0,        0),
(211, 2,  '现金报销',               0,        5000.00,  1),
(212, 2,  '银行存款增加',     72000.00, 0,        0),
(212, 3,  '冲减应收',            0,        72000.00, 1),
(213, 5,  '采购原材料',           18500.00, 0,        0),
(213, 2,  '银行存款支付',        0,        18500.00, 1),
(214, 9,  '计提工资',                   13000.00, 0,        0),
(214, 2,  '银行代发',                 0,        13000.00, 1),
(215, 2,  '银行存款增加',     55000.00, 0,        0),
(215, 3,  '冲减应收',            0,        55000.00, 1),
(216, 6,  '采购电子设备',         42000.00, 0,        0),
(216, 2,  '银行存款支付',        0,        42000.00, 1),
(217, 9,  '计提工资',                   13500.00, 0,        0),
(217, 2,  '银行代发',                 0,        13500.00, 1),
(218, 19, '技术服务费',                8000.00,  0,        0),
(218, 2,  '银行转账',            0,        8000.00,  1),
(219, 2,  '银行存款增加',     85000.00, 0,        0),
(219, 3,  '冲减应收',            0,        85000.00, 1),
(220, 5,  '采购原材料',           25000.00, 0,        0),
(220, 2,  '银行存款支付',        0,        25000.00, 1),
(221, 9,  '计提工资',                   14000.00, 0,        0),
(221, 2,  '银行代发',                 0,        14000.00, 1),
(222, 19, '差旅招待费',             6500.00,  0,        0),
(222, 2,  '现金报销',               0,        6500.00,  1);

-- ============================================================
-- Part 5: Transactions (Jan-Jun 2026)
-- ============================================================

DELETE FROM `t_transaction` WHERE `user_id` = 2;

INSERT INTO `t_transaction` (`user_id`, `type`, `amount`, `category`, `description`, `transaction_date`, `counterparty`, `created_at`) VALUES
(2, 'INCOME',  50000.00, '销售收入',   'Lan Hai Ding Dan 货款',       '2026-01-05', '蓝海贸易', '2026-01-05 10:00:00'),
(2, 'EXPENSE', 32000.00, '采购支出',     '顺德材料采购',        '2026-01-12', '顺德材料', '2026-01-12 14:00:00'),
(2, 'EXPENSE', 12000.00, '人力成本',    '1 Yue 工资',                   '2026-01-25', NULL, '2026-01-25 09:00:00'),
(2, 'EXPENSE',  3500.00, '差旅费用',     '1月深圳出差',        '2026-01-28', NULL, '2026-01-28 16:00:00'),
(2, 'INCOME',  15000.00, '技术服务',        '信达技术咨询费',        '2026-01-20', '信达电子', '2026-01-20 11:00:00'),
(2, 'INCOME',  68000.00, '销售收入',   '信达订单货款',        '2026-02-08', '信达电子', '2026-02-08 10:00:00'),
(2, 'EXPENSE', 15000.00, '采购支出',     'Hua Qiang Yuan Qi Jian 采购',  '2026-02-15', '华强电子', '2026-02-15 14:00:00'),
(2, 'EXPENSE', 12000.00, '人力成本',    '2 Yue 工资',                   '2026-02-25', NULL, '2026-02-25 09:00:00'),
(2, 'EXPENSE',  2800.00, '办公费用',   'Ban Gong Yong Pin 采购',       '2026-02-18', NULL, '2026-02-18 11:00:00'),
(2, 'INCOME',  22000.00, '销售收入',   '鸿图小批量订单',  '2026-02-20', '鸿图制造', '2026-02-20 15:00:00'),
(2, 'INCOME',  45000.00, '销售收入',   '鸿图订单 货款',       '2026-03-10', '鸿图制造', '2026-03-10 10:00:00'),
(2, 'EXPENSE', 28000.00, '技术服务',        '博达年度服务费',         '2026-03-18', '博达软件', '2026-03-18 14:00:00'),
(2, 'EXPENSE', 12500.00, '人力成本',    '3 Yue 工资',                   '2026-03-25', NULL, '2026-03-25 09:00:00'),
(2, 'EXPENSE',  5000.00, '展览费用',   '深圳电子展参展', '2026-03-28', NULL, '2026-03-28 16:00:00'),
(2, 'INCOME',  38000.00, '销售收入',   '蓝海补单',                  '2026-03-15', '蓝海贸易', '2026-03-15 11:00:00'),
(2, 'INCOME',  72000.00, '销售收入',   '蓝海大额订单',           '2026-04-06', '蓝海贸易', '2026-04-06 10:00:00'),
(2, 'EXPENSE', 18500.00, '采购支出',     '原材料 采购',           '2026-04-14', '顺德材料', '2026-04-14 14:00:00'),
(2, 'EXPENSE', 13000.00, '人力成本',    '4 Yue 工资',                   '2026-04-25', NULL, '2026-04-25 09:00:00'),
(2, 'EXPENSE',  4200.00, '招待费用',   '客户招待餐费',          '2026-04-18', NULL, '2026-04-18 19:00:00'),
(2, 'INCOME',  28000.00, '技术服务',        '信达运维费',              '2026-04-22', '信达电子', '2026-04-22 10:00:00'),
(2, 'INCOME',  55000.00, '销售收入',   '信达订单货款',        '2026-05-08', '信达电子', '2026-05-08 10:00:00'),
(2, 'EXPENSE', 42000.00, '设备 采购',     '电子设备采购',         '2026-05-15', '华强电子', '2026-05-15 14:00:00'),
(2, 'EXPENSE', 13500.00, '人力成本',    '5 Yue 工资',                   '2026-05-25', NULL, '2026-05-25 09:00:00'),
(2, 'EXPENSE',  8000.00, '技术服务',        'Zhong Ke 技术服务费',       '2026-05-28', '中科云联', '2026-05-28 16:00:00'),
(2, 'INCOME',  35000.00, '销售收入',   '鸿图订单',                '2026-05-12', '鸿图制造', '2026-05-12 11:00:00'),
(2, 'INCOME',  85000.00, '销售收入',   '鸿图大额订单',           '2026-06-05', '鸿图制造', '2026-06-05 10:00:00'),
(2, 'EXPENSE', 25000.00, '采购支出',     '原材料 采购',           '2026-06-12', '顺德材料', '2026-06-12 14:00:00'),
(2, 'EXPENSE', 14000.00, '人力成本',    '6 Yue 工资',                   '2026-06-25', NULL, '2026-06-25 09:00:00'),
(2, 'EXPENSE',  4500.00, '差旅费用',     '6月广州出差',       '2026-06-09', NULL, '2026-06-09 08:00:00'),
(2, 'EXPENSE',  1680.00, '招待费用',   '客户招待餐费',          '2026-06-12', NULL, '2026-06-12 19:00:00'),
(2, 'INCOME',  42000.00, '销售收入',   '蓝海月度结算',         '2026-06-20', '蓝海贸易', '2026-06-20 10:00:00'),
(2, 'EXPENSE',  2360.00, '办公费用',   'Ban Gong Yong Pin 采购',       '2026-06-07', NULL, '2026-06-07 15:00:00'),
(2, 'EXPENSE',   960.00, '交通费用',  '公司车辆加油',       '2026-06-19', NULL, '2026-06-19 18:00:00');

-- ============================================================
-- Part 6: Invoices (Jan-Jun 2026)
-- ============================================================

DELETE FROM `t_invoice` WHERE `user_id` = 2;

INSERT INTO `t_invoice` (`user_id`, `invoice_no`, `invoice_date`, `seller_name`, `buyer_name`, `amount`, `tax_amount`, `total_amount`, `category`, `status`, `created_at`) VALUES
(2, 'INV-2026-0101', '2026-01-05', '蓝海贸易', '智联科技', 44247.79, 5752.21, 50000.00, '销售', 'VERIFIED', '2026-01-05 10:30:00'),
(2, 'INV-2026-0102', '2026-01-12', '顺德材料', '智联科技', 28318.58, 3681.42, 32000.00, '采购', 'VERIFIED', '2026-01-12 14:30:00'),
(2, 'INV-2026-0103', '2026-01-20', '信达电子', '智联科技', 13274.34, 1725.66, 15000.00, '技术', 'VERIFIED', '2026-01-20 11:30:00'),
(2, 'INV-2026-0201', '2026-02-08', '信达电子', '智联科技', 60176.99, 7823.01, 68000.00, '销售', 'VERIFIED', '2026-02-08 10:30:00'),
(2, 'INV-2026-0202', '2026-02-15', '华强电子', '智联科技', 13274.34, 1725.66, 15000.00, '采购', 'VERIFIED', '2026-02-15 14:30:00'),
(2, 'INV-2026-0203', '2026-02-20', '鸿图制造', '智联科技', 19469.03, 2530.97, 22000.00, '销售', 'VERIFIED', '2026-02-20 15:30:00'),
(2, 'INV-2026-0301', '2026-03-10', '鸿图制造', '智联科技', 39823.01, 5176.99, 45000.00, '销售', 'VERIFIED', '2026-03-10 10:30:00'),
(2, 'INV-2026-0302', '2026-03-18', '博达软件', '智联科技', 24778.76, 3221.24, 28000.00, '技术', 'VERIFIED', '2026-03-18 14:30:00'),
(2, 'INV-2026-0303', '2026-03-15', '蓝海贸易', '智联科技', 33628.32, 4371.68, 38000.00, '销售', 'VERIFIED', '2026-03-15 11:30:00'),
(2, 'INV-2026-0401', '2026-04-06', '蓝海贸易', '智联科技', 63716.81, 8283.19, 72000.00, '销售', 'VERIFIED', '2026-04-06 10:30:00'),
(2, 'INV-2026-0402', '2026-04-14', '顺德材料', '智联科技', 16371.68, 2128.32, 18500.00, '采购', 'VERIFIED', '2026-04-14 14:30:00'),
(2, 'INV-2026-0403', '2026-04-22', '信达电子', '智联科技', 24778.76, 3221.24, 28000.00, '技术', 'VERIFIED', '2026-04-22 10:30:00'),
(2, 'INV-2026-0501', '2026-05-08', '信达电子', '智联科技', 48672.57, 6327.43, 55000.00, '销售', 'VERIFIED', '2026-05-08 10:30:00'),
(2, 'INV-2026-0502', '2026-05-15', '华强电子', '智联科技', 37168.14, 4831.86, 42000.00, '设备', 'VERIFIED', '2026-05-15 14:30:00'),
(2, 'INV-2026-0503', '2026-05-12', '鸿图制造', '智联科技', 30973.45, 4026.55, 35000.00, '销售', 'VERIFIED', '2026-05-12 11:30:00'),
(2, 'INV-2026-0504', '2026-05-28', '中科云联', '智联科技', 7079.65, 920.35, 8000.00, '技术', 'VERIFIED', '2026-05-28 16:30:00'),
(2, 'INV-2026-0601', '2026-06-05', '鸿图制造', '智联科技', 75221.24, 9778.76, 85000.00, '销售', 'VERIFIED', '2026-06-05 10:30:00'),
(2, 'INV-2026-0602', '2026-06-12', '顺德材料', '智联科技', 22123.89, 2876.11, 25000.00, '采购', 'VERIFIED', '2026-06-12 14:30:00'),
(2, 'INV-2026-0603', '2026-06-20', '蓝海贸易', '智联科技', 37168.14, 4831.86, 42000.00, '销售', 'VERIFIED', '2026-06-20 10:30:00'),
(2, 'INV-2026-0604', '2026-06-09', '蓝海贸易', '智联科技', 3982.30, 517.70, 4500.00, '差旅', 'PENDING', '2026-06-09 10:30:00');

-- ============================================================
-- Part 7: Bank Transactions (Jan-Jun 2026)
-- ============================================================

DELETE FROM `t_bank_transaction` WHERE `user_id` = 2;

INSERT INTO `t_bank_transaction` (`user_id`, `bank_name`, `account_no`, `transaction_date`, `counterparty`, `summary`, `debit_amount`, `credit_amount`, `balance`, `status`, `created_at`) VALUES
(2, '工商银行', '6222801234567890', '2026-01-05', '蓝海贸易',   '货款到账',       0,        50000.00, 550000.00, 'MATCHED',   '2026-01-05 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-01-12', '顺德材料', '采购付款',          32000.00, 0,        518000.00, 'MATCHED',   '2026-01-12 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-01-25', '代发工资',   '1月工资代发',     12000.00, 0,        506000.00, 'MATCHED',   '2026-01-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-01-20', '信达电子',   '技术咨询费到账', 0,     15000.00, 521000.00, 'MATCHED',   '2026-01-20 11:00:00'),
(2, '工商银行', '6222801234567890', '2026-02-08', '信达电子',   '货款到账',       0,        68000.00, 589000.00, 'MATCHED',   '2026-02-08 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-02-15', '华强电子','采购付款',          15000.00, 0,        574000.00, 'MATCHED',   '2026-02-15 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-02-25', '代发工资',   '2月工资代发',     12000.00, 0,        562000.00, 'MATCHED',   '2026-02-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-02-20', '鸿图制造',  '订单货款到账', 0,     22000.00, 584000.00, 'MATCHED',   '2026-02-20 15:00:00'),
(2, '工商银行', '6222801234567890', '2026-03-10', '鸿图制造',  '货款到账',       0,        45000.00, 629000.00, 'MATCHED',   '2026-03-10 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-03-18', '博达软件',  '服务费付款',        28000.00, 0,        601000.00, 'MATCHED',   '2026-03-18 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-03-25', '代发工资',   '3月工资代发',     12500.00, 0,        588500.00, 'MATCHED',   '2026-03-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-03-15', '蓝海贸易',   '补单货款到账', 0,       38000.00, 626500.00, 'MATCHED',   '2026-03-15 11:00:00'),
(2, '工商银行', '6222801234567890', '2026-04-06', '蓝海贸易',   '大额货款到账',  0,        72000.00, 698500.00, 'MATCHED',   '2026-04-06 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-04-14', '顺德材料', '采购付款',          18500.00, 0,        680000.00, 'MATCHED',   '2026-04-14 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-04-25', '代发工资',   '4月工资代发',     13000.00, 0,        667000.00, 'MATCHED',   '2026-04-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-04-22', '信达电子',   '运维费到账',    0,        28000.00, 695000.00, 'MATCHED',   '2026-04-22 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-05-08', '信达电子',   '货款到账',       0,        55000.00, 750000.00, 'MATCHED',   '2026-05-08 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-05-15', '华强电子','设备采购付款',  42000.00, 0,        708000.00, 'MATCHED',   '2026-05-15 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-05-25', '代发工资',   '5月工资代发',     13500.00, 0,        694500.00, 'MATCHED',   '2026-05-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-05-28', '中科云联','技术服务费付款', 8000.00,  0,        686500.00, 'MATCHED',   '2026-05-28 16:00:00'),
(2, '工商银行', '6222801234567890', '2026-05-12', '鸿图制造',  '订单货款到账', 0,      35000.00, 721500.00, 'MATCHED',   '2026-05-12 11:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-05', '鸿图制造',  '大额货款到账',  0,        85000.00, 806500.00, 'MATCHED',   '2026-06-05 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-12', '顺德材料', '采购付款',          25000.00, 0,        781500.00, 'MATCHED',   '2026-06-12 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-25', '代发工资',   '6月工资代发',     14000.00, 0,        767500.00, 'MATCHED',   '2026-06-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-20', '蓝海贸易',   '月度结算到账', 0,       42000.00, 809500.00, 'MATCHED',   '2026-06-20 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-18', '腾讯计算', '办公软件年费', 3600.00, 0,      805900.00, 'UNMATCHED', '2026-06-18 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-06-22', '顺丰快递', '快递月结费',      1250.00,  0,        804650.00, 'UNMATCHED', '2026-06-22 10:00:00');

-- ============================================================
-- Part 8: Salary Records (Jan-Jun 2026, 4 employees)
-- ============================================================

DELETE FROM `t_salary_record` WHERE `user_id` = 2;

INSERT INTO `t_salary_record` (`user_id`, `month`, `employee_no`, `employee_name`, `department`, `position`, `base_salary`, `bonus`, `allowance`, `overtime`, `gross_pay`, `social_insurance`, `housing_fund`, `tax`, `other_deduction`, `net_pay`, `status`, `created_at`) VALUES
(2, '2026-01', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 1500.00, 0.00, 15500.00, 1200.00, 450.00, 85.00,  0.00, 13765.00, 'PAID', '2026-01-25 09:00:00'),
(2, '2026-02', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 1500.00, 0.00, 15500.00, 1200.00, 450.00, 85.00,  0.00, 13765.00, 'PAID', '2026-02-25 09:00:00'),
(2, '2026-03', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2500.00, 1500.00, 200.00, 16200.00, 1200.00, 450.00, 118.00, 0.00, 14432.00, 'PAID', '2026-03-25 09:00:00'),
(2, '2026-04', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 1500.00, 0.00, 15500.00, 1200.00, 450.00, 85.00,  0.00, 13765.00, 'PAID', '2026-04-25 09:00:00'),
(2, '2026-05', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 2000.00, 0.00, 16000.00, 1200.00, 450.00, 110.00, 0.00, 14240.00, 'PAID', '2026-05-25 09:00:00'),
(2, '2026-06', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 2000.00, 0.00, 16000.00, 1200.00, 450.00, 110.00, 0.00, 14240.00, 'PAID', '2026-06-25 09:00:00'),
(2, '2026-01', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-01-25 09:00:00'),
(2, '2026-02', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-02-25 09:00:00'),
(2, '2026-03', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  100.00, 9900.00,  800.00,  300.00, 0.00,   0.00, 8800.00,  'PAID', '2026-03-25 09:00:00'),
(2, '2026-04', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-04-25 09:00:00'),
(2, '2026-05', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-05-25 09:00:00'),
(2, '2026-06', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-06-25 09:00:00'),
(2, '2026-01', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 3000.00, 1200.00, 0.00, 14200.00, 1000.00, 400.00, 48.00,  0.00, 12752.00, 'PAID', '2026-01-25 09:00:00'),
(2, '2026-02', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 4000.00, 1200.00, 0.00, 15200.00, 1000.00, 400.00, 108.00, 0.00, 13692.00, 'PAID', '2026-02-25 09:00:00'),
(2, '2026-03', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 5000.00, 1200.00, 0.00, 16200.00, 1000.00, 400.00, 168.00, 0.00, 14632.00, 'PAID', '2026-03-25 09:00:00'),
(2, '2026-04', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 3500.00, 1200.00, 0.00, 14700.00, 1000.00, 400.00, 73.00,  0.00, 13227.00, 'PAID', '2026-04-25 09:00:00'),
(2, '2026-05', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 4500.00, 1200.00, 0.00, 15700.00, 1000.00, 400.00, 138.00, 0.00, 14162.00, 'PAID', '2026-05-25 09:00:00'),
(2, '2026-06', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 6000.00, 1200.00, 0.00, 17200.00, 1000.00, 400.00, 218.00, 0.00, 15582.00, 'PAID', '2026-06-25 09:00:00'),
(2, '2026-01', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 3000.00, 2000.00, 500.00, 20500.00, 1500.00, 600.00, 328.00, 0.00, 18072.00, 'PAID', '2026-01-25 09:00:00'),
(2, '2026-02', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 3000.00, 2000.00, 300.00, 20300.00, 1500.00, 600.00, 318.00, 0.00, 17882.00, 'PAID', '2026-02-25 09:00:00'),
(2, '2026-03', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 4000.00, 2000.00, 800.00, 21800.00, 1500.00, 600.00, 398.00, 0.00, 19302.00, 'PAID', '2026-03-25 09:00:00'),
(2, '2026-04', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 3000.00, 2000.00, 0.00, 20000.00, 1500.00, 600.00, 303.00, 0.00, 17597.00, 'PAID', '2026-04-25 09:00:00'),
(2, '2026-05', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 3500.00, 2000.00, 0.00, 20500.00, 1500.00, 600.00, 328.00, 0.00, 18072.00, 'PAID', '2026-05-25 09:00:00'),
(2, '2026-06', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 4000.00, 2000.00, 0.00, 21000.00, 1500.00, 600.00, 353.00, 0.00, 18547.00, 'PAID', '2026-06-25 09:00:00');

-- ============================================================
-- Part 9: Expenses (Jan-Jun 2026)
-- ============================================================

DELETE FROM `t_expense` WHERE `user_id` = 2;

INSERT INTO `t_expense` (`user_id`, `title`, `applicant`, `category`, `amount`, `expense_date`, `description`, `status`, `approver`, `approval_opinion`, `approved_at`, `created_at`, `updated_at`) VALUES
(2, '1月深圳出差', '张会计', '差旅费用', 3500.00,  '2026-01-28', '深圳客户拜访', 'APPROVED', '李明', '同意报销', '2026-01-29 10:00:00', '2026-01-28 16:00:00', '2026-01-29 10:00:00'),
(2, '1月办公用品', '张会计', '办公费用', 1860.00, '2026-01-15', '打印纸墨盒', 'APPROVED', '李明', '同意报销', '2026-01-16 10:00:00', '2026-01-15 11:00:00', '2026-01-16 10:00:00'),
(2, '2月客户招待', '张会计', '招待费用', 2200.00, '2026-02-18', '招待蓝海李总', 'APPROVED', '李明', '同意报销', '2026-02-19 10:00:00', '2026-02-18 20:00:00', '2026-02-19 10:00:00'),
(2, '2月快递费', '张会计', '办公费用', 680.00,   '2026-02-22', '产品样品顺丰', 'APPROVED', '李明', '同意报销', '2026-02-23 10:00:00', '2026-02-22 17:00:00', '2026-02-23 10:00:00'),
(2, '3月展会布展', '张会计', '展览费用', 8500.00, '2026-03-20', '深圳国际展览中心', 'APPROVED', '李明', '同意报销', '2026-03-21 10:00:00', '2026-03-20 09:00:00', '2026-03-21 10:00:00'),
(2, '3月广州出差', '张会计', '差旅费用', 4200.00, '2026-03-28', '广州客户拜访', 'APPROVED', '李明', '同意报销', '2026-03-29 10:00:00', '2026-03-28 16:00:00', '2026-03-29 10:00:00'),
(2, '4月办公用品', '张会计', '办公费用', 2360.00, '2026-04-10', '打印纸墨盒文件夹', 'APPROVED', '李明', '同意报销', '2026-04-11 10:00:00', '2026-04-10 11:00:00', '2026-04-11 10:00:00'),
(2, '4月客户招待', '张会计', '招待费用', 1680.00, '2026-04-18', '招待信达陈总', 'PENDING',  NULL,   NULL,         NULL,                  '2026-04-18 20:00:00', '2026-04-18 20:00:00'),
(2, '5月珠海出差', '张会计', '差旅费用', 5800.00, '2026-05-28', '珠海博达现场审计', 'APPROVED', '李明', '同意报销', '2026-05-29 10:00:00', '2026-05-28 16:00:00', '2026-05-29 10:00:00'),
(2, '5月员工团建', '张会计', '团建费用', 4800.00, '2026-05-22', '部门聚餐KTV', 'APPROVED', '李明', '同意报销', '2026-05-23 10:00:00', '2026-05-22 13:00:00', '2026-05-23 10:00:00'),
(2, '6月广州出差', '张会计', '差旅费用', 4500.00, '2026-06-09', '广州客户拜访', 'APPROVED', '李明', '同意报销', '2026-06-11 10:00:00', '2026-06-10 09:00:00', '2026-06-11 10:00:00'),
(2, '6月办公用品', '张会计', '办公费用', 2360.00, '2026-06-07', '打印纸墨盒', 'APPROVED', '李明', '同意报销', '2026-06-08 14:00:00', '2026-06-07 15:00:00', '2026-06-08 14:00:00'),
(2, '6月客户招待', '张会计', '招待费用', 1680.00, '2026-06-12', '招待蓝海李总', 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-13 09:00:00', '2026-06-13 09:00:00'),
(2, '6月快递费', '张会计', '办公费用', 580.00,   '2026-06-14', '产品样品顺丰', 'PENDING',  NULL,   NULL,         NULL,                  '2026-06-14 16:30:00', '2026-06-14 16:30:00'),
(2, '6月车辆油费', '张会计', '交通费用', 960.00, '2026-06-19', '公司商务车加油', 'APPROVED', '李明', '同意报销', '2026-06-20 10:00:00', '2026-06-19 18:00:00', '2026-06-20 10:00:00');

-- ============================================================
-- Part 10: Fixed Assets (5 items)
-- ============================================================

DELETE FROM `t_fixed_asset` WHERE `user_id` = 2;

INSERT INTO `t_fixed_asset` (`user_id`, `asset_code`, `asset_name`, `category`, `purchase_date`, `original_value`, `salvage_value`, `depreciation_method`, `useful_life_months`, `monthly_depreciation`, `accumulated_depreciation`, `depreciated_months`, `status`, `created_at`) VALUES
(2, 'FA-2025-001', '联想ThinkPad X1',  '办公设备', '2025-03-15', 12000.00, 600.00,  'STRAIGHT_LINE', 36, 316.67,  4116.71, 13, 'IN_USE', '2025-03-15 09:00:00'),
(2, 'FA-2025-002', '惠普LaserJet打印机', '办公设备', '2025-04-20', 5000.00,  250.00,  'STRAIGHT_LINE', 36, 131.94,  1583.28, 12, 'IN_USE', '2025-04-20 09:00:00'),
(2, 'FA-2025-003', '戴尔4K显示器',   '办公设备', '2025-06-10', 4500.00,  225.00,  'STRAIGHT_LINE', 36, 118.75,  1187.50, 10, 'IN_USE', '2025-06-10 09:00:00'),
(2, 'FA-2026-001', '丰田凯美瑞',    '运输设备', '2026-01-10', 180000.00, 9000.00, 'STRAIGHT_LINE', 60, 2850.00, 14250.00, 5,  'IN_USE', '2026-01-10 09:00:00'),
(2, 'FA-2026-002', '华为MateBook 16s',     '办公设备', '2026-03-01', 9000.00,  450.00,  'STRAIGHT_LINE', 36, 237.50,  712.50,  3,  'IN_USE', '2026-03-01 09:00:00');

-- ============================================================
-- Part 11: Budgets (2026)
-- ============================================================

DELETE FROM `t_budget` WHERE `user_id` = 2;

INSERT INTO `t_budget` (`user_id`, `department`, `year`, `month`, `category`, `budget_amount`, `used_amount`, `alert_threshold`, `description`, `created_at`) VALUES
(2, '财务部', 2026, 1, '办公费用',  5000.00,  1860.00, 80, '1 Yue 办公费用',   '2026-01-01 09:00:00'),
(2, '财务部', 2026, 2, '办公费用',  5000.00,  2540.00, 80, '2 Yue 办公费用',   '2026-02-01 09:00:00'),
(2, '财务部', 2026, 3, '办公费用',  5000.00,  3200.00, 80, '3 Yue 办公费用',   '2026-03-01 09:00:00'),
(2, '财务部', 2026, 4, '办公费用',  5000.00,  2360.00, 80, '4 Yue 办公费用',   '2026-04-01 09:00:00'),
(2, '财务部', 2026, 5, '办公费用',  5000.00,  4800.00, 80, '5 Yue 办公费用',   '2026-05-01 09:00:00'),
(2, '财务部', 2026, 6, '办公费用',  5000.00,  4180.00, 80, '6 Yue 办公费用',   '2026-06-01 09:00:00'),
(2, '销售部', 2026, 1, '差旅费用', 10000.00, 3500.00, 80, '1 Yue 差旅费用',     '2026-01-01 09:00:00'),
(2, '销售部', 2026, 2, '差旅费用', 10000.00, 2200.00, 80, '2 Yue 差旅费用',     '2026-02-01 09:00:00'),
(2, '销售部', 2026, 3, '差旅费用', 10000.00, 13500.00, 80, '3 Yue 差旅费用',     '2026-03-01 09:00:00'),
(2, '销售部', 2026, 4, '差旅费用', 10000.00, 4200.00, 80, '4 Yue 差旅费用',     '2026-04-01 09:00:00'),
(2, '销售部', 2026, 5, '差旅费用', 10000.00, 5800.00, 80, '5 Yue 差旅费用',     '2026-05-01 09:00:00'),
(2, '销售部', 2026, 6, '差旅费用', 10000.00, 9460.00, 80, '6 Yue 差旅费用',     '2026-06-01 09:00:00'),
(2, '技术部', 2026, 1, '技术服务', 15000.00, 0.00,    80, '1 Yue 技术服务',         '2026-01-01 09:00:00'),
(2, '技术部', 2026, 2, '技术服务', 15000.00, 0.00,    80, '2 Yue 技术服务',         '2026-02-01 09:00:00'),
(2, '技术部', 2026, 3, '技术服务', 15000.00, 28000.00, 80, '3 Yue 技术服务',         '2026-03-01 09:00:00'),
(2, '技术部', 2026, 4, '技术服务', 15000.00, 0.00,    80, '4 Yue 技术服务',         '2026-04-01 09:00:00'),
(2, '技术部', 2026, 5, '技术服务', 15000.00, 8000.00, 80, '5 Yue 技术服务',         '2026-05-01 09:00:00'),
(2, '技术部', 2026, 6, '技术服务', 15000.00, 0.00,    80, '6 Yue 技术服务',         '2026-06-01 09:00:00');

-- ============================================================
-- Part 12: Alerts
-- ============================================================

DELETE FROM `t_alert` WHERE `user_id` = 2;

INSERT INTO `t_alert` (`user_id`, `type`, `level`, `title`, `content`, `status`, `created_at`) VALUES
(2, 'BUDGET',     'WARNING',  '3月差旅费超预算',     '销售部3月差旅费超出', 'READ',   '2026-03-28 09:00:00'),
(2, 'BUDGET',     'WARNING',  '5月办公费接近上限', '财务部5月办公费达到96%', 'READ',   '2026-05-25 09:00:00'),
(2, 'TAX_RISK',   'INFO',     '6月增值税申报提醒', '6月增值税申报截止7月15日', 'READ',   '2026-06-28 09:00:00'),
(2, 'INVOICE',    'WARNING',  '待认证发票提醒',         '您有1张发票待认证', 'UNREAD', '2026-06-29 09:00:00'),
(2, 'BUDGET',     'WARNING',  '6月差旅费接近上限', '销售部6月差旅费达到94.6%', 'UNREAD', '2026-06-29 10:00:00'),
(2, 'CASHFLOW',   'NORMAL',   '6月资金流入正常',     '6月累计收入210000元', 'UNREAD', '2026-06-30 09:00:00'),
(2, 'COMPLIANCE', 'INFO',     '季度所得税预缴提醒',     'Q2企业所得税预缴截止7月15日', 'UNREAD', '2026-06-30 09:00:00');

-- ============================================================
-- Part 13: Chat Messages
-- ============================================================

DELETE FROM `t_chat_message` WHERE `user_id` = 2;

INSERT INTO `t_chat_message` (`user_id`, `session_id`, `role`, `content`, `created_at`) VALUES
(2, 'sess-acc-001', 'user',      '帮我分析一下6月的利润情况', '2026-06-28 10:00:00'),
(2, 'sess-acc-001', 'assistant', '6月收入210000元，支出81700元，利润128300元，利润率61.1%', '2026-06-28 10:00:05'),
(2, 'sess-acc-002', 'user',      '3月的差旅费为什么超预算', '2026-06-28 10:05:00'),
(2, 'sess-acc-002', 'assistant', '3月展会费用8500元超出预算', '2026-06-28 10:05:05'),
(2, 'sess-acc-003', 'user',      '帮我算一下上半年应缴增值税', '2026-06-29 09:00:00'),
(2, 'sess-acc-003', 'assistant', '上半年累计收入601000元，增值税约78130元', '2026-06-29 09:00:05'),
(2, 'sess-acc-004', 'user',      '哪些客户回款比较慢', '2026-06-29 14:00:00'),
(2, 'sess-acc-004', 'assistant', '鸿图制造回款周期15天，蓝海10天，信达8天', '2026-06-29 14:00:05'),
(2, 'sess-acc-005', 'user',      '6月有哪些未报销的费用', '2026-06-30 09:00:00'),
(2, 'sess-acc-005', 'assistant', '6月待报销费用合计8460元', '2026-06-30 09:00:05');

-- ============================================================
-- Part 14: Voucher Templates
-- ============================================================

DELETE FROM `t_voucher_template` WHERE `user_id` = 2;

INSERT INTO `t_voucher_template` (`user_id`, `name`, `type`, `description`, `entries_json`, `created_at`) VALUES
(2, '销售收款模板', 'RECEIPT', '收到客户货款时使用', '[{"accountId":2,"summary":"银行存款"},{"accountId":3,"summary":"冲减应收"}]', '2026-01-01 09:00:00'),
(2, '采购付款模板', 'PAYMENT', '支付供应商货款时使用', '[{"accountId":8,"summary":"应付账款"},{"accountId":2,"summary":"银行存款"}]', '2026-01-01 09:00:00'),
(2, '工资发放模板', 'TRANSFER', '每月发放员工工资时使用', '[{"accountId":9,"summary":"应付职工薪酬"},{"accountId":2,"summary":"银行代发"}]', '2026-01-01 09:00:00');

-- ============================================================
-- Part 15: Vouchers Jul-Aug 2026
-- ============================================================

DELETE FROM `t_voucher_entry` WHERE `voucher_id` IN (SELECT `id` FROM `t_voucher` WHERE `user_id` = 2 AND `voucher_date` BETWEEN '2026-07-01' AND '2026-08-31');
DELETE FROM `t_voucher` WHERE `user_id` = 2 AND `voucher_date` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_voucher` (`id`, `user_id`, `voucher_no`, `type`, `amount`, `category`, `description`, `voucher_date`, `counterparty`, `status`, `has_invoice`, `created_at`) VALUES
(301, 2, 'PZ-2026-07-001', 'PAYMENT',  78000.00, '货款',   '收到蓝海货款',        '2026-07-07', '蓝海贸易', 'POSTED', b'1', '2026-07-07 10:00:00'),
(302, 2, 'PZ-2026-07-002', 'PAYMENT',  35000.00, '采购',    '采购原材料',            '2026-07-16', '顺德材料', 'POSTED', b'1', '2026-07-16 14:00:00'),
(303, 2, 'PZ-2026-07-003', 'TRANSFER', 14500.00, '工资',    '发放7月工资',             '2026-07-25', NULL, 'POSTED', b'0', '2026-07-25 09:00:00'),
(304, 2, 'PZ-2026-07-004', 'TRANSFER',  4200.00, '费用',   '报销7月交通费',      '2026-07-28', NULL, 'POSTED', b'1', '2026-07-28 16:00:00'),
(305, 2, 'PZ-2026-08-001', 'PAYMENT',  62000.00, '货款',   '收到信达货款',          '2026-08-06', '信达电子', 'POSTED', b'1', '2026-08-06 10:00:00'),
(306, 2, 'PZ-2026-08-002', 'PAYMENT',  48000.00, '采购',    '采购设备维修',           '2026-08-14', '华强电子', 'POSTED', b'1', '2026-08-14 14:00:00'),
(307, 2, 'PZ-2026-08-003', 'TRANSFER', 15000.00, '工资',    '发放8月工资',             '2026-08-25', NULL, 'POSTED', b'0', '2026-08-25 09:00:00'),
(308, 2, 'PZ-2026-08-004', 'TRANSFER',  5500.00, '费用',   '支付博达输入专家费',  '2026-08-28', '博达软件', 'POSTED', b'1', '2026-08-28 16:00:00');

INSERT INTO `t_voucher_entry` (`voucher_id`, `account_id`, `summary`, `debit_amount`, `credit_amount`, `sort_order`) VALUES
(301, 2,  '银行存款增加',     78000.00, 0,        0),
(301, 3,  '冲减应收',            0,        78000.00, 1),
(302, 5,  '采购原材料',           35000.00, 0,        0),
(302, 2,  '银行存款支付',        0,        35000.00, 1),
(303, 9,  '计提工资',                   14500.00, 0,        0),
(303, 2,  '银行代发',                 0,        14500.00, 1),
(304, 19, '交通费报销',          4200.00,  0,        0),
(304, 2,  '现金报销',               0,        4200.00,  1),
(305, 2,  '银行存款增加',     62000.00, 0,        0),
(305, 3,  '冲减应收',            0,        62000.00, 1),
(306, 19, '设备维修费',             48000.00, 0,        0),
(306, 2,  '银行存款支付',        0,        48000.00, 1),
(307, 9,  '计提工资',                   15000.00, 0,        0),
(307, 2,  '银行代发',                 0,        15000.00, 1),
(308, 19, '专家费',                   5500.00,  0,        0),
(308, 2,  '银行转账',            0,        5500.00,  1);

-- ============================================================
-- Part 16: Transactions Jul-Aug 2026
-- ============================================================

DELETE FROM `t_transaction` WHERE `user_id` = 2 AND `transaction_date` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_transaction` (`user_id`, `type`, `amount`, `category`, `description`, `transaction_date`, `counterparty`, `created_at`) VALUES
(2, 'INCOME',  78000.00, '销售收入',   '蓝海7月订单货款',   '2026-07-07', '蓝海贸易', '2026-07-07 10:00:00'),
(2, 'EXPENSE', 35000.00, '采购支出',     '顺德7月采购',              '2026-07-16', '顺德材料', '2026-07-16 14:00:00'),
(2, 'EXPENSE', 14500.00, '人力成本',    '7月工资',                      '2026-07-25', NULL, '2026-07-25 09:00:00'),
(2, 'EXPENSE',  4200.00, '交通费用',  '7月报销',                     '2026-07-28', NULL, '2026-07-28 16:00:00'),
(2, 'INCOME',  32000.00, '技术服务',        '信达7月运维费',           '2026-07-20', '信达电子', '2026-07-20 10:00:00'),
(2, 'INCOME',  62000.00, '销售收入',   '信达8月订单货款',    '2026-08-06', '信达电子', '2026-08-06 10:00:00'),
(2, 'EXPENSE', 48000.00, '设备维修',     '华强8月设备维修',   '2026-08-14', '华强电子', '2026-08-14 14:00:00'),
(2, 'EXPENSE', 15000.00, '人力成本',    '8月工资',                      '2026-08-25', NULL, '2026-08-25 09:00:00'),
(2, 'EXPENSE',  5500.00, '技术服务',        '博达专家费',                '2026-08-28', '博达软件', '2026-08-28 16:00:00'),
(2, 'INCOME',  58000.00, '销售收入',   '鸿图8月订单',             '2026-08-10', '鸿图制造', '2026-08-10 11:00:00'),
(2, 'EXPENSE',  2800.00, '办公费用',   '7月办公用品',            '2026-07-09', NULL, '2026-07-09 15:00:00'),
(2, 'EXPENSE',  3200.00, '招待费用',   '7月客户招待',               '2026-07-22', NULL, '2026-07-22 19:00:00'),
(2, 'EXPENSE',  1350.00, '办公费用',   '8月快递费',                  '2026-08-05', NULL, '2026-08-05 17:00:00'),
(2, 'EXPENSE',  3600.00, '招待费用',   '8月客户招待',               '2026-08-18', NULL, '2026-08-18 19:00:00'),
(2, 'EXPENSE',  850.00,  '交通费用',  '8月车辆加油',            '2026-08-22', NULL, '2026-08-22 18:00:00'),
(2, 'INCOME',  25000.00, '其他业务收入',  '7月专利许可收入',      '2026-07-30', '中科云联', '2026-07-30 10:00:00'),
(2, 'INCOME',  45000.00, '销售收入',   '蓝海8月补单货款',      '2026-08-20', '蓝海贸易', '2026-08-20 10:00:00');

-- ============================================================
-- Part 17: Invoices Jul-Aug 2026
-- ============================================================

DELETE FROM `t_invoice` WHERE `user_id` = 2 AND `invoice_date` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_invoice` (`user_id`, `invoice_no`, `invoice_date`, `seller_name`, `buyer_name`, `amount`, `tax_amount`, `total_amount`, `category`, `status`, `created_at`) VALUES
(2, 'INV-2026-0701', '2026-07-07', '蓝海贸易', '智联科技', 69026.55, 8973.45, 78000.00, '销售', 'VERIFIED', '2026-07-07 10:30:00'),
(2, 'INV-2026-0702', '2026-07-16', '顺德材料', '智联科技', 30973.45, 4026.55, 35000.00, '采购', 'VERIFIED', '2026-07-16 14:30:00'),
(2, 'INV-2026-0703', '2026-07-20', '信达电子', '智联科技', 28318.58, 3681.42, 32000.00, '技术', 'VERIFIED', '2026-07-20 10:30:00'),
(2, 'INV-2026-0704', '2026-07-30', '中科云联', '智联科技', 22123.89, 2876.11, 25000.00, '其他', 'VERIFIED', '2026-07-30 10:30:00'),
(2, 'INV-2026-0801', '2026-08-06', '信达电子', '智联科技', 54867.26, 7132.74, 62000.00, '销售', 'VERIFIED', '2026-08-06 10:30:00'),
(2, 'INV-2026-0802', '2026-08-10', '鸿图制造', '智联科技', 51327.43, 6672.57, 58000.00, '销售', 'VERIFIED', '2026-08-10 11:30:00'),
(2, 'INV-2026-0803', '2026-08-14', '华强电子', '智联科技', 42477.88, 5522.12, 48000.00, '维修', 'VERIFIED', '2026-08-14 14:30:00'),
(2, 'INV-2026-0804', '2026-08-20', '蓝海贸易', '智联科技', 39823.01, 5176.99, 45000.00, '销售', 'VERIFIED', '2026-08-20 10:30:00');

-- ============================================================
-- Part 18: Bank Transactions Jul-Aug 2026
-- ============================================================

DELETE FROM `t_bank_transaction` WHERE `user_id` = 2 AND `transaction_date` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_bank_transaction` (`user_id`, `bank_name`, `account_no`, `transaction_date`, `counterparty`, `summary`, `debit_amount`, `credit_amount`, `balance`, `status`, `created_at`) VALUES
(2, '工商银行', '6222801234567890', '2026-07-07', '蓝海贸易',   '货款到账',          0,        78000.00, 887500.00, 'MATCHED',   '2026-07-07 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-07-16', '顺德材料', '采购付款',             35000.00, 0,        852500.00, 'MATCHED',   '2026-07-16 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-07-25', '代发工资',   '7月工资代发',        14500.00, 0,        838000.00, 'MATCHED',   '2026-07-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-07-20', '信达电子',   '运维费到账',       0,        32000.00, 870000.00, 'MATCHED',   '2026-07-20 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-07-30', '中科云联','专利许可到账',    0,        25000.00, 895000.00, 'MATCHED',   '2026-07-30 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-06', '信达电子',   '货款到账',          0,        62000.00, 957000.00, 'MATCHED',   '2026-08-06 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-10', '鸿图制造',  '订单货款到账', 0,        58000.00, 1015000.00,'MATCHED',   '2026-08-10 11:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-14', '华强电子','维修费付款',         48000.00, 0,        967000.00, 'MATCHED',   '2026-08-14 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-25', '代发工资',   '8月工资代发',        15000.00, 0,        952000.00, 'MATCHED',   '2026-08-25 09:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-28', '博达软件',  '专家费付款',        5500.00,  0,        946500.00, 'MATCHED',   '2026-08-28 16:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-20', '蓝海贸易',   '补单货款到账',   0,        45000.00, 991500.00, 'MATCHED',   '2026-08-20 10:00:00'),
(2, '工商银行', '6222801234567890', '2026-07-12', '微信公众号','微信提现',          600.00,   0,        994400.00, 'UNMATCHED', '2026-07-12 14:00:00'),
(2, '工商银行', '6222801234567890', '2026-08-08', '滴滴打车',     '商务打车费',          280.00,   0,        991220.00, 'UNMATCHED', '2026-08-08 20:00:00');

-- ============================================================
-- Part 19: Expenses Jul-Aug 2026
-- ============================================================

DELETE FROM `t_expense` WHERE `user_id` = 2 AND `expense_date` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_expense` (`user_id`, `title`, `applicant`, `category`, `amount`, `expense_date`, `description`, `status`, `approver`, `approval_opinion`, `approved_at`, `created_at`, `updated_at`) VALUES
(2, '7月办公用品', '张会计', '办公费用', 2800.00, '2026-07-09', '打印纸墨盒', 'APPROVED', '李明', '同意报销', '2026-07-10 10:00:00', '2026-07-09 15:00:00', '2026-07-10 10:00:00'),
(2, '7月客户招待', '张会计', '招待费用', 3200.00, '2026-07-22', '招待信达陈总', 'APPROVED', '李明', '同意报销', '2026-07-23 10:00:00', '2026-07-22 19:00:00', '2026-07-23 10:00:00'),
(2, '7月上门服务', '张会计', '差旅费用', 4800.00, '2026-07-18', '上门维护', 'APPROVED', '李明', '同意报销', '2026-07-19 10:00:00', '2026-07-18 14:00:00', '2026-07-19 10:00:00'),
(2, '7月办公软件', '张会计', '技术服务', 6800.00, '2026-07-15', '博达年度服务', 'APPROVED', '李明', '同意报销', '2026-07-16 10:00:00', '2026-07-15 11:00:00', '2026-07-16 10:00:00'),
(2, '8月办公用品', '张会计', '办公费用', 1350.00, '2026-08-05', '快递费月结', 'PENDING',  NULL, NULL, NULL, '2026-08-05 17:00:00', '2026-08-05 17:00:00'),
(2, '8月客户招待', '张会计', '招待费用', 3600.00, '2026-08-18', '招待蓝海吴总', 'APPROVED', '李明', '同意报销', '2026-08-19 10:00:00', '2026-08-18 19:00:00', '2026-08-19 10:00:00'),
(2, '8月房屋租金', '张会计', '租金', 15000.00, '2026-08-01', '8月办公室租金', 'APPROVED', '李明', '同意报销', '2026-08-02 10:00:00', '2026-08-01 09:00:00', '2026-08-02 10:00:00'),
(2, '8月水电费', '张会计', '水电费', 2100.00, '2026-08-10', '八月水电费缴纳', 'APPROVED', '李明', '同意报销', '2026-08-11 10:00:00', '2026-08-10 15:00:00', '2026-08-11 10:00:00'),
(2, '8月车辆油费', '张会计', '交通费用', 850.00, '2026-08-22', '公司商务车加油', 'PENDING',  NULL, NULL, NULL, '2026-08-22 18:00:00', '2026-08-22 18:00:00'),
(2, '8月员工团建', '张会计', '团建费用', 5500.00, '2026-08-23', '部门聚餐BBQ', 'APPROVED', '李明', '同意报销', '2026-08-24 10:00:00', '2026-08-23 13:00:00', '2026-08-24 10:00:00');

-- ============================================================
-- Part 20: Salary Records Jul-Aug 2026
-- ============================================================

DELETE FROM `t_salary_record` WHERE `user_id` = 2 AND `month` IN ('2026-07', '2026-08');

INSERT INTO `t_salary_record` (`user_id`, `month`, `employee_no`, `employee_name`, `department`, `position`, `base_salary`, `bonus`, `allowance`, `overtime`, `gross_pay`, `social_insurance`, `housing_fund`, `tax`, `other_deduction`, `net_pay`, `status`, `created_at`) VALUES
(2, '2026-07', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2500.00, 2000.00, 0.00, 16500.00, 1200.00, 450.00, 145.00, 0.00, 14705.00, 'PAID', '2026-07-25 09:00:00'),
(2, '2026-08', 'EMP001', '张会计', '财务部', '会计',     12000.00, 2000.00, 2000.00, 0.00, 16000.00, 1200.00, 450.00, 110.00, 0.00, 14240.00, 'PAID', '2026-08-25 09:00:00'),
(2, '2026-07', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1200.00, 800.00,  0.00, 10000.00, 800.00,  300.00, 0.00,   0.00, 8900.00,  'PAID', '2026-07-25 09:00:00'),
(2, '2026-08', 'EMP002', '李出纳', '财务部', '出纳',        8000.00,  1000.00, 800.00,  0.00, 9800.00,  800.00,  300.00, 0.00,   0.00, 8700.00,  'PAID', '2026-08-25 09:00:00'),
(2, '2026-07', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 5500.00, 1200.00, 0.00, 16700.00, 1000.00, 400.00, 193.00, 0.00, 15107.00, 'PAID', '2026-07-25 09:00:00'),
(2, '2026-08', 'EMP003', '王销售', '销售部', '销售经理', 10000.00, 4000.00, 1200.00, 0.00, 15200.00, 1000.00, 400.00, 108.00, 0.00, 13692.00, 'PAID', '2026-08-25 09:00:00'),
(2, '2026-07', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 4500.00, 2000.00, 500.00, 22000.00, 1500.00, 600.00, 413.00, 0.00, 19487.00, 'PAID', '2026-07-25 09:00:00'),
(2, '2026-08', 'EMP004', '赵技术', '技术部', '技术主管', 15000.00, 3000.00, 2000.00, 300.00, 20300.00, 1500.00, 600.00, 318.00, 0.00, 17882.00, 'PAID', '2026-08-25 09:00:00');

-- ============================================================
-- Part 21: Budgets Jul-Aug 2026
-- ============================================================

DELETE FROM `t_budget` WHERE `user_id` = 2 AND `month` IN (7, 8);

INSERT INTO `t_budget` (`user_id`, `department`, `year`, `month`, `category`, `budget_amount`, `used_amount`, `alert_threshold`, `description`, `created_at`) VALUES
(2, '财务部', 2026, 7, '办公费用',  5000.00,  2800.00, 80, '7 Yue 办公费用',   '2026-07-01 09:00:00'),
(2, '财务部', 2026, 8, '办公费用',  5000.00,  1350.00, 80, '8 Yue 办公费用',   '2026-08-01 09:00:00'),
(2, '销售部', 2026, 7, '差旅费用', 10000.00, 8000.00, 80, '7 Yue 差旅费用',     '2026-07-01 09:00:00'),
(2, '销售部', 2026, 8, '差旅费用', 10000.00, 4450.00, 80, '8 Yue 差旅费用',     '2026-08-01 09:00:00'),
(2, '技术部', 2026, 7, '技术服务', 15000.00, 6800.00, 80, '7 Yue 技术服务',         '2026-07-01 09:00:00'),
(2, '技术部', 2026, 8, '技术服务', 15000.00, 5500.00, 80, '8 Yue 技术服务',         '2026-08-01 09:00:00');

-- ============================================================
-- Part 22: Alerts Jul-Aug 2026
-- ============================================================

DELETE FROM `t_alert` WHERE `user_id` = 2 AND `created_at` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_alert` (`user_id`, `type`, `level`, `title`, `content`, `status`, `created_at`) VALUES
(2, 'BUDGET',   'WARNING',  '7月差旅费接近上限', '销售部7月差旅费达到80%', 'READ',   '2026-07-28 09:00:00'),
(2, 'BUDGET',   'WARNING',  '8月专家费超预算',    '技术部8月专家费超出',  'UNREAD', '2026-08-29 09:00:00'),
(2, 'TAX_RISK', 'INFO',     '9月增值税申报提醒', '9月增值税申报截止10月15日', 'UNREAD', '2026-08-30 09:00:00'),
(2, 'CASHFLOW', 'NORMAL',   '7月资金流入正常',     '7月累计收入135000元', 'READ',   '2026-07-31 09:00:00'),
(2, 'CASHFLOW', 'NORMAL',   '8月资金流入正常',     '8月累计收入165000元', 'UNREAD', '2026-08-31 09:00:00'),
(2, 'COMPLIANCE', 'INFO',   '已年度所得税预缴提醒', '去年度企业所得税补充申报截止5月31日', 'READ', '2026-07-05 09:00:00'),
(2, 'INVOICE',  'WARNING',  '待认证发票提醒',         '您有2张发票待认证', 'UNREAD', '2026-08-28 09:00:00');

-- ============================================================
-- Part 23: Chat Messages Jul-Aug 2026
-- ============================================================

DELETE FROM `t_chat_message` WHERE `user_id` = 2 AND `created_at` BETWEEN '2026-07-01' AND '2026-08-31';

INSERT INTO `t_chat_message` (`user_id`, `session_id`, `role`, `content`, `created_at`) VALUES
(2, 'sess-acc-006', 'user',      '帮我分析7月的支出结构', '2026-07-28 10:00:00'),
(2, 'sess-acc-006', 'assistant', '7月支出54500元，其中采购35000元，人力14500元', '2026-07-28 10:00:05'),
(2, 'sess-acc-007', 'user',      '8月有哪些待收款项', '2026-08-28 10:05:00'),
(2, 'sess-acc-007', 'assistant', '8月应收账款逾期45000元，预计9月15日前到账', '2026-08-28 10:05:05'),
(2, 'sess-acc-008', 'user',      '帮我看看上半年应交税费', '2026-08-30 09:00:00'),
(2, 'sess-acc-008', 'assistant', '上半年累计应缴增值税约85000元，企业所得税约42000元', '2026-08-30 09:00:05');

-- ============================================================
-- Done
-- ============================================================
SELECT 'DONE' AS result;
