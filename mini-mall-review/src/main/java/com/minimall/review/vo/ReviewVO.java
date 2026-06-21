package com.minimall.review.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价展示 VO (View Object)
 * <p>
 * Service → Controller → 前端的【出参】
 * <p>
 * 跟 Entity 的区别:
 *   ✓ 多了 username (Entity 只有 userId, 这里要显示"小明"而不是"1024")
 *     username 暂时先空着, 真要做时用 Feign 调 user 服务批量查 → G7 进阶
 *   ✗ 没有 isDeleted / updateTime (内部字段不返前端)
 *   ✓ createTime 格式化成字符串 (前端少一个 ISO 转换)
 *
 * 顺手教学: 为啥前端列表要返 username 不返 userId?
 *   用户体验: 看到"1024"和看到"小明", 哪个用户能认?
 */
@Data
public class ReviewVO {

    // ⭐ TODO ① id
    // [你写]
private Long id;

    // ⭐ TODO ② userId (展示用, 前端不强依赖, 但留着方便点头像)
    // [你写]
private Long userId;

    // ⭐ TODO ③ username (字符串, 暂时可以填 null, G7 进阶再 Feign 拿)
    //
private String userName;

    // ⭐ TODO ④ orderId
    // [你写]
private Long orderId;

    // ⭐ TODO ⑤ productId
    // [你写]
private Long productId;

    // ⭐ TODO ⑥ rating (Integer)
    // [你写]
private Integer rating;

    // ⭐ TODO ⑦ content
    // [你写]
private String content;

    // ⭐ TODO ⑧ createTime
    //   类型: LocalDateTime
    //   注解: @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    //   注: 不加注解 LocalDateTime 默认序列化成数组 [2026,6,21,9,47,17], 前端不友好
    // [你写]
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private LocalDateTime createTime;
}
