USE `fiva_assistant`;
SHOW TABLES;
SELECT 't_account' AS table_name, COUNT(*) AS rows_count FROM t_account
UNION ALL SELECT 't_alert', COUNT(*) FROM t_alert
UNION ALL SELECT 't_bank_transaction', COUNT(*) FROM t_bank_transaction
UNION ALL SELECT 't_budget', COUNT(*) FROM t_budget
UNION ALL SELECT 't_chat_message', COUNT(*) FROM t_chat_message
UNION ALL SELECT 't_company', COUNT(*) FROM t_company
UNION ALL SELECT 't_contact', COUNT(*) FROM t_contact
UNION ALL SELECT 't_expense', COUNT(*) FROM t_expense
UNION ALL SELECT 't_fixed_asset', COUNT(*) FROM t_fixed_asset
UNION ALL SELECT 't_invoice', COUNT(*) FROM t_invoice
UNION ALL SELECT 't_salary_record', COUNT(*) FROM t_salary_record
UNION ALL SELECT 't_transaction', COUNT(*) FROM t_transaction
UNION ALL SELECT 't_user', COUNT(*) FROM t_user
UNION ALL SELECT 't_voucher', COUNT(*) FROM t_voucher
UNION ALL SELECT 't_voucher_entry', COUNT(*) FROM t_voucher_entry
UNION ALL SELECT 't_voucher_template', COUNT(*) FROM t_voucher_template;
