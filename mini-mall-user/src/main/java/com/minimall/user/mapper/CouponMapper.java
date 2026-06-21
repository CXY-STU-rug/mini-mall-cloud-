package com.minimall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.user.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 券模板 Mapper
 * <p>
 * MP 自带 CRUD; 额外加 1 个【原子扣库存】SQL, 防超发.
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子扣减剩余库存 (CAS 思想)
     * <p>
     * 条件: remain_stock > 0
     * 失败返 0 行, 业务层抛"券已领完"
     */
    @Update("UPDATE coupon SET remain_stock = remain_stock - 1 " +
            "WHERE id = #{id} AND remain_stock > 0 AND is_deleted = 0")
    int deductRemainStock(@Param("id") Long id);

    /**
     * 回滚剩余库存 (用户退券时, 或领券事务回滚补偿时用)
     */
    @Update("UPDATE coupon SET remain_stock = remain_stock + 1 " +
            "WHERE id = #{id} AND is_deleted = 0")
    int restoreRemainStock(@Param("id") Long id);
}
