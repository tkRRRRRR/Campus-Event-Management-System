-- ============================================================================
--  索引脚本（仅 MySQL 需要执行一次）
--  H2 只在演示模式使用、数据量极小，不需要这些索引，
--  且 H2 / MySQL 的 CREATE INDEX 语法不完全一致，所以单独成文件。
--  执行顺序：schema.sql -> indexes.sql -> data.sql
-- ============================================================================

-- 活动列表：按状态 + 开始时间筛选
CREATE INDEX idx_activity_status_time ON activity (status, start_time);
-- 组织者维度：我的活动
CREATE INDEX idx_activity_organizer ON activity (organizer_id);
-- 分类统计
CREATE INDEX idx_activity_category ON activity (category_id);
-- 学生维度：我的报名
CREATE INDEX idx_reg_user_status ON activity_registration (user_id, status);
-- 活动维度：报名名单 / 签到名单
CREATE INDEX idx_reg_activity_status ON activity_registration (activity_id, status);
-- 评价：按活动聚合
CREATE INDEX idx_review_activity ON activity_review (activity_id);
-- 站内信：未读数
CREATE INDEX idx_notice_user_read ON notice (user_id, is_read);
