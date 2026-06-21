package com.minimall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.user.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户领券 Mapper
 * <p>
 * 普通 CRUD 用 MP, status 流转(领→用→退)走 LambdaUpdateWrapper.
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
