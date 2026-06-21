-- ============================================================
-- G8 优惠券增量脚本 (在已有 mini_mall 库上跑一次, 非幂等)
-- 跑法: mysql -uroot -p123456 --default-character-set=utf8mb4 mini_mall < g8-coupons.sql
--      不加 --default-character-set 会让 Windows mysql 用 GBK 解析, 中文 INSERT 报 1366
-- ============================================================
USE mini_mall;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. orders 加 2 个字段
--    user_coupon_id  : 关联【用户领的那张具体券】(不是券模板), 退回时直接定位
--    discount_amount : 实际抵扣金额快照, 取消时按这个值回原券
-- ------------------------------------------------------------
ALTER TABLE `orders`
    ADD COLUMN `user_coupon_id`  BIGINT        DEFAULT NULL COMMENT '用户使用的具体券(user_coupon.id)',
    ADD COLUMN `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际抵扣金额';

-- ------------------------------------------------------------
-- 2. coupon —— 券模板
--    type=1 满减 (满 threshold 减 discount 元)
--    type=2 折扣 (满 threshold 打 discount 折, 0.9 = 9 折) - 预留
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT      COMMENT '主键ID',
  `name`          VARCHAR(100)  NOT NULL                      COMMENT '券名 如"满100减10"',
  `type`          TINYINT       NOT NULL DEFAULT 1            COMMENT '类型 1=满减 2=折扣',
  `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0.00         COMMENT '使用门槛 满多少',
  `discount`      DECIMAL(10,2) NOT NULL                      COMMENT '抵扣值 type=1 是金额 type=2 是折扣率(0.9)',
  `total_stock`   INT           NOT NULL DEFAULT 0            COMMENT '总发行量',
  `remain_stock`  INT           NOT NULL DEFAULT 0            COMMENT '剩余可领数',
  `valid_from`    DATETIME      NOT NULL                      COMMENT '生效时间',
  `valid_to`      DATETIME      NOT NULL                      COMMENT '过期时间',
  `status`        TINYINT       NOT NULL DEFAULT 1            COMMENT '状态 0下架 1上架',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT       NOT NULL DEFAULT 0            COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板';

-- ------------------------------------------------------------
-- 3. user_coupon —— 用户领的具体券
--    UNIQUE KEY (user_id, coupon_id) → 每人每种券最多领 1 张 (业务规则)
--    status: 0=未用 1=已用
--    过期: 不存独立 status, 业务层查 coupon.valid_to < NOW() 即过期
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键ID',
  `user_id`       BIGINT       NOT NULL                       COMMENT '领券人',
  `coupon_id`     BIGINT       NOT NULL                       COMMENT '关联 coupon.id',
  `status`        TINYINT      NOT NULL DEFAULT 0             COMMENT '0=未用 1=已用',
  `receive_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time`      DATETIME     DEFAULT NULL                   COMMENT '使用时间',
  `order_id`      BIGINT       DEFAULT NULL                   COMMENT '用在哪个订单上',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
  KEY `idx_user_id`   (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领券记录';

-- ------------------------------------------------------------
-- 4. 测试数据 (方便 G8.8 端到端)
-- ------------------------------------------------------------
INSERT INTO `coupon`
  (`name`, `type`, `threshold`, `discount`, `total_stock`, `remain_stock`, `valid_from`, `valid_to`, `status`)
VALUES
  ('满100减10', 1, 100.00, 10.00, 100, 100, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1),
  ('满5000减500', 1, 5000.00, 500.00, 50, 50, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1);
