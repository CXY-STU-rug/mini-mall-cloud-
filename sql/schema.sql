-- ============================================================
-- 迷你商城（mini-mall-cloud 微服务版）数据库建表脚本
-- 数据库：mini_mall
-- 用途：换电脑/重装时一键重建所有表；阅读时一目了然
-- 使用方法：
--   1) mysql -uroot -p123456
--   2) CREATE DATABASE mini_mall DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   3) USE mini_mall;
--   4) SOURCE D:/path/to/schema.sql;   （或在 Navicat 里运行整个文件）
--
-- 跟单体 schema.sql 的差异（增量内联到 CREATE 里, 不写 ALTER 补丁）:
--   - orders     新增 logistics_no / logistics_company   (G6 物流签收)
--   - orders     新增 user_coupon_id / discount_amount   (G8 优惠券抵扣)
--   - products   新增 avg_rating / review_count          (G7 评价聚合)
--   - reviews    全新表                                  (G7 评价主表)
--   - coupon     全新表                                  (G8 券模板)
--   - user_coupon 全新表                                 (G8 用户领券)
-- ============================================================

-- 注意：建表顺序按依赖关系排（被引用的先建，但本项目没建外键，只是逻辑依赖）

-- ------------------------------------------------------------
-- 1. user —— 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键ID',
  `username`    VARCHAR(50)  NOT NULL                       COMMENT '用户名（登录用，唯一）',
  `password`    VARCHAR(100) NOT NULL                       COMMENT '密码（BCrypt 加密后）',
  `nickname`    VARCHAR(50)  DEFAULT NULL                   COMMENT '昵称（展示用）',
  `phone`       VARCHAR(20)  DEFAULT NULL                   COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL                   COMMENT '邮箱',
  `avatar`      VARCHAR(100) DEFAULT NULL                   COMMENT '头像URL',
  `role`        TINYINT      NOT NULL DEFAULT 0             COMMENT '角色：0=普通用户 1=管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1             COMMENT '状态：0=禁用 1=启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)                     -- 用户名唯一，注册时防止重名
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. category —— 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '分类ID',
  `name`        VARCHAR(50)  NOT NULL                       COMMENT '分类名',
  `icon`        VARCHAR(255) DEFAULT NULL                   COMMENT '图标URL',
  `sort`        INT          NOT NULL DEFAULT 0             COMMENT '排序值，越小越靠前',
  `status`      TINYINT      NOT NULL DEFAULT 1             COMMENT '状态：0=禁用 1=启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. product —— 商品表
--   外键关系（逻辑上）：category_id → category.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT      COMMENT '商品ID',
  `category_id` BIGINT        NOT NULL                      COMMENT '分类ID（指向 category.id）',
  `name`        VARCHAR(100)  NOT NULL                      COMMENT '商品名',
  `description` VARCHAR(500)  DEFAULT NULL                  COMMENT '商品简介',
  `detail`      TEXT          DEFAULT NULL                  COMMENT '商品详情（长文本HTML）',
  `price`       DECIMAL(10,2) NOT NULL                      COMMENT '价格（元）',
  `stock`       INT           NOT NULL DEFAULT 0            COMMENT '库存',
  `sales`       INT           NOT NULL DEFAULT 0            COMMENT '销量（累计）',
  -- G7 评分聚合字段（评价落库后由 review 服务回写，避免实时 AVG/COUNT 拖慢商品详情）
  `avg_rating`   DECIMAL(2,1) NOT NULL DEFAULT 0.0          COMMENT '平均评分 0.0~5.0',
  `review_count` INT          NOT NULL DEFAULT 0            COMMENT '评价总数',
  `cover_image` VARCHAR(255)  DEFAULT NULL                  COMMENT '封面图URL',
  `status`      TINYINT       NOT NULL DEFAULT 1            COMMENT '状态：0=下架 1=上架',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT       NOT NULL DEFAULT 0            COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)                     -- 按分类查商品的查询会走这个索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ------------------------------------------------------------
-- 4. address —— 收货地址表
--   逻辑外键：user_id → user.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键ID',
  `user_id`     BIGINT       NOT NULL                       COMMENT '用户ID（指向 user.id）',
  `receiver`    VARCHAR(50)  NOT NULL                       COMMENT '收货人姓名',
  `phone`       VARCHAR(20)  NOT NULL                       COMMENT '手机号',
  `province`    VARCHAR(50)  NOT NULL                       COMMENT '省',
  `city`        VARCHAR(50)  NOT NULL                       COMMENT '市',
  `district`    VARCHAR(50)  NOT NULL                       COMMENT '区/县',
  `detail`      VARCHAR(200) NOT NULL                       COMMENT '详细地址',
  `is_default`  TINYINT      NOT NULL DEFAULT 0             COMMENT '是否默认：0否 1是',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)                             -- "我的地址列表"按 user_id 查
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- ------------------------------------------------------------
-- 5. cart_item —— 购物车表
--   逻辑外键：user_id → user.id, product_id → product.id
--   关键：(user_id, product_id) 唯一约束 —— 同一用户同一商品只能有一条，加购时累加 quantity
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT          COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                          COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL                          COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 1                COMMENT '数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT  NOT NULL DEFAULT 0                COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)   -- 同一用户的同一商品只能有一行（幂等保障）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ------------------------------------------------------------
-- 6. orders —— 订单主表
--   注意：表名是 orders（带 s），因为 order 是 MySQL 保留字
--   设计要点：
--     - order_no：业务订单号（如 20260524123456001），对外暴露用这个，不是 id
--     - receiver/phone/address：**快照字段**，下单瞬间拷贝过来，
--       之后用户改地址也不影响历史订单
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
  `order_no`     VARCHAR(32)   NOT NULL                     COMMENT '订单号（业务唯一）',
  `user_id`      BIGINT        NOT NULL                     COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL                     COMMENT '订单总金额',
  `status`       TINYINT       NOT NULL DEFAULT 0           COMMENT '状态：0待付款 1已付款 2已发货 3已完成 4已取消',
  `receiver`     VARCHAR(50)   NOT NULL                     COMMENT '收货人（快照）',
  `phone`        VARCHAR(20)   NOT NULL                     COMMENT '手机号（快照）',
  `address`      VARCHAR(500)  NOT NULL                     COMMENT '收货地址（快照，省+市+区+详细）',
  `pay_time`     DATETIME      DEFAULT NULL                 COMMENT '支付时间',
  `ship_time`    DATETIME      DEFAULT NULL                 COMMENT '发货时间',
  `finish_time`  DATETIME      DEFAULT NULL                 COMMENT '完成时间',
  -- G6 物流字段（发货时 UPDATE 填入，签收/定时自动签收会读）
  `logistics_company` VARCHAR(32)  DEFAULT NULL              COMMENT '物流公司（如：顺丰/中通/京东）',
  `logistics_no`      VARCHAR(64)  DEFAULT NULL              COMMENT '物流单号',
  -- G8 优惠券抵扣字段（下单选了券就填, 没选保持默认）
  `user_coupon_id`    BIGINT       DEFAULT NULL              COMMENT '用户使用的具体券(user_coupon.id)',
  `discount_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '实际抵扣金额',
  `remark`       VARCHAR(200)  DEFAULT NULL                 COMMENT '备注',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0           COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),                    -- 订单号唯一
  KEY `idx_user_id` (`user_id`)                             -- "我的订单"按 user_id 查
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ------------------------------------------------------------
-- 7. order_item —— 订单明细表
--   一个订单可能有多个商品 → 一行 orders 对应多行 order_item
--   关键设计：product_name / product_image / price 都是**快照**
--             商品改名改价不影响历史订单的展示
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL                    COMMENT '订单ID（指向 orders.id）',
  `product_id`    BIGINT        NOT NULL                    COMMENT '商品ID',
  `product_name`  VARCHAR(100)  NOT NULL                    COMMENT '商品名（快照）',
  `product_image` VARCHAR(255)  DEFAULT NULL                COMMENT '商品图（快照）',
  `price`         DECIMAL(10,2) NOT NULL                    COMMENT '成交单价（快照）',
  `quantity`      INT           NOT NULL                    COMMENT '购买数量',
  `subtotal`      DECIMAL(10,2) NOT NULL                    COMMENT '小计 = price * quantity',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)                           -- 按订单查明细
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';


-- ============================================================
-- 📐 表关系图（文字版）
-- ============================================================
--
--   user (1) ────────┬──────── (N) address       一个用户多个地址
--                    │
--                    ├──────── (N) cart_item ──── (1) product   购物车连接用户和商品
--                    │
--                    └──────── (N) orders ─────── (N) order_item ── (1) product
--                                                                 （商品信息是快照）
--   category (1) ──── (N) product       一个分类多个商品
--
-- ============================================================
-- 🔑 设计要点速查
-- ============================================================
--   1. 所有表都有 is_deleted —— 配合 MyBatis-Plus @TableLogic 实现"软删除"
--   2. 所有表都有 create_time/update_time —— 自动维护，看数据生命周期方便
--   3. 金额一律 DECIMAL(10,2) —— 永远不要用 float/double 存钱（精度丢失）
--   4. 字符集 utf8mb4 —— 支持 emoji 和生僻字
--   5. 命名规约：表名/字段名小写下划线（user_id），Java 字段驼峰（userId）
--      由 mybatis-plus 的 map-underscore-to-camel-case: true 自动映射
--   6. 索引原则：where 经常用、order by 经常用的字段加 KEY；唯一字段加 UNIQUE KEY
--   7. 订单/订单明细的"快照字段"：下单瞬间冻结，不跟随商品/地址后续变化



CREATE TABLE IF NOT EXISTS seckill_activity (
id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
product_id BIGINT NOT NULL COMMENT '商品ID',
 seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀单价',
stock INT NOT NULL COMMENT '秒杀库存',
start_time DATETIME NOT NULL COMMENT '秒杀开始时间',
end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待开始，1-进行中，2-已结束'
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';


create table if not exists seckill_activity_id (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
  `order_no`     VARCHAR(32)   NOT NULL                     COMMENT '订单号（业务唯一）',
  `user_id`      BIGINT        NOT NULL                     COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL                     COMMENT '订单总金额',
  `status`       TINYINT       NOT NULL DEFAULT 0           COMMENT '状态：0待付款 1已付款 2已发货 3已完成 4已取消',
  `receiver`     VARCHAR(50)   NOT NULL                     COMMENT '收货人（快照）',
  `phone`        VARCHAR(20)   NOT NULL                     COMMENT '手机号（快照）',
  `address`      VARCHAR(500)  NOT NULL                     COMMENT '收货地址（快照，省+市+区+详细）',
  `pay_time`     DATETIME      DEFAULT NULL                 COMMENT '支付时间',
  `ship_time`    DATETIME      DEFAULT NULL                 COMMENT '发货时间',
  `finish_time`  DATETIME      DEFAULT NULL                 COMMENT '完成时间',
  `remark`       VARCHAR(200)  DEFAULT NULL                 COMMENT '备注',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0           COMMENT '逻辑删除',
  `seckill_activity_id` BIGINT NOT NULL COMMENT '秒杀活动ID（指向 seckill_activity.id）',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';


-- ------------------------------------------------------------
-- 9. reviews —— 商品评价表（G7 新增，微服务版独有，单体没做）
--   设计要点：
--     - 一笔订单里同一个商品只能评价一次（数据库层用 UNIQUE KEY 兜底）
--     - 评分聚合（avg_rating/review_count）落 product 表，避免每次查商品都跑 AVG/COUNT
--     - reviews 表自己不存 user_nickname 快照，列表展示再 Feign 调 user 服务取
--     - 软删除：同 orders，方便后续做"删除评价但保留聚合"的需求
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '主键ID',
  `user_id`     BIGINT       NOT NULL                      COMMENT '评价人ID（指向 user.id）',
  `order_id`    BIGINT       NOT NULL                      COMMENT '关联订单ID（指向 orders.id）',
  `product_id`  BIGINT       NOT NULL                      COMMENT '商品ID（指向 product.id）',
  `rating`      TINYINT      NOT NULL                      COMMENT '评分 1-5 星',
  `content`     VARCHAR(500) DEFAULT NULL                  COMMENT '评价文字内容',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product` (`order_id`, `product_id`),  -- 同一订单同一商品只能评一次（数据库兜底，应用层是第一道防线）
  KEY `idx_product_id` (`product_id`),                       -- "商品详情页评价列表"按 product_id 查
  KEY `idx_user_id`    (`user_id`)                            -- "我的评价"按 user_id 查
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价表';

-- ------------------------------------------------------------
-- 10. coupon —— 优惠券模板 (G8 新增, 微服务版独有)
--   设计要点:
--     - type=1 满减 (最常见, G8 教学主要做这种)
--     - type=2 折扣 留扩展, 暂时不写代码
--     - remain_stock 跟领券时原子扣减, 防超发
--     - valid_from / valid_to 双时间界, 业务层查时跟 NOW() 比对决定能不能领能不能用
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT      COMMENT '主键ID',
  `name`          VARCHAR(100)  NOT NULL                      COMMENT '券名 如"满100减10"',
  `type`          TINYINT       NOT NULL DEFAULT 1            COMMENT '类型 1=满减 2=折扣',
  `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0.00         COMMENT '使用门槛 满多少',
  `discount`      DECIMAL(10,2) NOT NULL                      COMMENT '抵扣值 type=1 是金额 type=2 是折扣率',
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
-- 11. user_coupon —— 用户领的具体券 (G8 新增)
--   设计要点:
--     - UNIQUE KEY (user_id, coupon_id) 每人每种券只能领 1 张
--     - status: 0=未用 1=已用; 过期不存独立 status, 看 coupon.valid_to 比 NOW
--     - order_id 用在哪个订单上 (退券时用来核对)
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