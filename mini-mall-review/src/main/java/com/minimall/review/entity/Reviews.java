package com.minimall.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价表实体 (PO - Persistent Object)
 * 对应数据库表: reviews
 * <p>
 * 跟表字段一一映射, 不加业务逻辑, 不带格式化, 不脱敏.
 *
 * 关键注解:
 *   @TableName("reviews")      → 指定表名 (类名是 Reviews 不是 Review, 防止 MP 自动推 review)
 *   @TableId(type = AUTO)      → 主键交给数据库 AUTO_INCREMENT
 *   @TableLogic                → MP 逻辑删除标志, delete 时自动转 UPDATE is_deleted=1
 *
 * yml 里 map-underscore-to-camel-case=true, 所以 user_id ↔ userId 自动转
 */
@Data
@TableName("reviews")
public class Reviews {

    // ⭐ TODO ① 主键 id
    //   类型: Long
    //   注解: @TableId(type = IdType.AUTO)
    //   注释: 主键ID
    // [你写]
    @TableId(type = IdType.AUTO)
    private Long id;

    // ⭐ TODO ② userId (评价人)
    //   类型: Long
    //   注释: 评价人ID
    // [你写]
private Long userId;

    // ⭐ TODO ③ orderId (关联订单)
    //   类型: Long
    //   注释: 关联订单ID
    // [你写]
private Long orderId;

    // ⭐ TODO ④ productId (商品)
    //   类型: Long
    //   注释: 商品ID
    // [你写]
private Long productId;

    // ⭐ TODO ⑤ rating (评分)
    //   类型: Integer (注意: 数据库是 TINYINT, Java 这边用 Integer 不用 Byte, 跟前端 JSON 数字对齐方便)
    //   注释: 评分 1-5 星
    // [你写]
private Integer rating;

    // ⭐ TODO ⑥ content (评价文字)
    //   类型: String
    //   注释: 评价文字内容
    // [你写]
private String content;

    // ⭐ TODO ⑦ createTime (创建时间)
    //   类型: LocalDateTime
    //   注释: 创建时间
    // [你写]
private LocalDateTime createTime;

    // ⭐ TODO ⑧ updateTime (更新时间)
    //   类型: LocalDateTime
    //   注释: 更新时间
    // [你写]
private LocalDateTime updateTime;

    // ⭐ TODO ⑨ isDeleted (逻辑删除, 加 @TableLogic)
    //   类型: Integer
    //   注解: @TableLogic
    //   注释: 逻辑删除 (0=正常 1=已删)
    // [你写]
    @TableLogic
private Integer isDeleted;
}
