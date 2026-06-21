package com.minimall.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.review.entity.Reviews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 评价表 Mapper
 * <p>
 * G7.7 时聚合查询写在 product 服务里 (直读 reviews 表), 是【教学妥协】.
 * 重构后聚合查询移到这里 (review 服务自家 mapper), product 通过 Feign 调.
 * → 这才符合"微服务一服务一表"原则.
 */
@Mapper
public interface ReviewsMapper extends BaseMapper<Reviews> {

    /**
     * 算指定商品的评分统计 (AVG + COUNT)
     * <p>
     * 返回 Map:
     *   "avgRating"   → BigDecimal  (无评价时是 null)
     *   "reviewCount" → Long
     */
    @Select("SELECT AVG(rating) AS avgRating, COUNT(*) AS reviewCount " +
            "FROM reviews " +
            "WHERE product_id = #{productId} AND is_deleted = 0")
    Map<String, Object> selectStats(@Param("productId") Long productId);
}
