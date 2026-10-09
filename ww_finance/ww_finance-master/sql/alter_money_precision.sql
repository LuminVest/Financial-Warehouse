-- ============================================================
-- 金额/利率精度扩容：支持高额计算（如借款 10 亿 + 多位小数）
-- 金额列 decimal(10,2) -> decimal(20,8)   （最大 9999...9999.99999999，8 位小数）
-- 利率列 decimal(10,4) -> decimal(12,6)
-- 执行：mysql -uroot -proot xx_finance < alter_money_precision.sql
-- ============================================================
USE xx_finance;

ALTER TABLE borrow_info
  MODIFY COLUMN amount DECIMAL(20,8),
  MODIFY COLUMN borrow_year_rate DECIMAL(12,6);

ALTER TABLE integral_grade
  MODIFY COLUMN borrow_amount DECIMAL(20,8);

ALTER TABLE lend
  MODIFY COLUMN amount DECIMAL(20,8),
  MODIFY COLUMN expect_amount DECIMAL(20,8),
  MODIFY COLUMN invest_amount DECIMAL(20,8),
  MODIFY COLUMN lowest_amount DECIMAL(20,8),
  MODIFY COLUMN real_amount DECIMAL(20,8),
  MODIFY COLUMN service_fee DECIMAL(20,8),
  MODIFY COLUMN service_rate DECIMAL(12,6),
  MODIFY COLUMN lend_year_rate DECIMAL(12,6);

ALTER TABLE lend_item
  MODIFY COLUMN expect_amount DECIMAL(20,8),
  MODIFY COLUMN invest_amount DECIMAL(20,8),
  MODIFY COLUMN real_amount DECIMAL(20,8),
  MODIFY COLUMN lend_year_rate DECIMAL(12,6);

ALTER TABLE lend_item_return
  MODIFY COLUMN fee DECIMAL(20,8),
  MODIFY COLUMN interest DECIMAL(20,8),
  MODIFY COLUMN invest_amount DECIMAL(20,8),
  MODIFY COLUMN overdue_total DECIMAL(20,8),
  MODIFY COLUMN principal DECIMAL(20,8),
  MODIFY COLUMN total DECIMAL(20,8),
  MODIFY COLUMN lend_year_rate DECIMAL(12,6);

ALTER TABLE lend_return
  MODIFY COLUMN amount DECIMAL(20,8),
  MODIFY COLUMN base_amount DECIMAL(20,8),
  MODIFY COLUMN fee DECIMAL(20,8),
  MODIFY COLUMN interest DECIMAL(20,8),
  MODIFY COLUMN overdue_total DECIMAL(20,8),
  MODIFY COLUMN principal DECIMAL(20,8),
  MODIFY COLUMN total DECIMAL(20,8),
  MODIFY COLUMN lend_year_rate DECIMAL(12,6);

ALTER TABLE trans_flow
  MODIFY COLUMN trans_amount DECIMAL(20,8);

ALTER TABLE user_account
  MODIFY COLUMN amount DECIMAL(20,8),
  MODIFY COLUMN freeze_amount DECIMAL(20,8);
