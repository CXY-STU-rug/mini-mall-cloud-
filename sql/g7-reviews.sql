-- ============================================================
-- G7 评价业务 - 增量迁移脚本
-- 用途: 在现有 mini_mall 库上执行, 不会 DROP 现有数据
-- 执行方式:
--   mysql -uroot -p123456 mini_mall < g7-reviews.sql
-- 重复执行说明:
--   ALTER TABLE 若列已存在会报 "Duplicate column name", 这是预期警告, 可忽略
--   CREATE TABLE 用 IF NOT EXISTS 已经做了幂等
-- ============================================================

USE mini_mall;

-- ------------------------------------------------------------
-- 1. product 表加 2 列: 评分聚合 (G7)
--    avg_rating  : 平均评分 (review 服务回写, 商品详情直接读, 避免实时 AVG)
--    review_count: 评价总数 (列表/排序常用, 避开 COUNT(*) 扫表)
-- ------------------------------------------------------------
ALTER TABLE `product`
    ADD COLUMN `avg_rating`   DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '平均评分 0.0~5.0',
    ADD COLUMN `review_count` INT          NOT NULL DEFAULT 0   COMMENT '评价总数';

-- ------------------------------------------------------------
-- 2. reviews 表 (G7 新建)
--    UNIQUE KEY (order_id, product_id) - 一笔订单同一商品只能评一次
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reviews` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '主键ID',
  `user_id`     BIGINT       NOT NULL                      COMMENT '评价人ID',
  `order_id`    BIGINT       NOT NULL                      COMMENT '关联订单ID',
  `product_id`  BIGINT       NOT NULL                      COMMENT '商品ID',
  `rating`      TINYINT      NOT NULL                      COMMENT '评分 1-5 星',
  `content`     VARCHAR(500) DEFAULT NULL                  COMMENT '评价文字内容',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product` (`order_id`, `product_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id`    (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价表';
