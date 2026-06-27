package com.minimall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.user.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    @Update("UPDATE address SET is_default = 0 WHERE user_id = #{userId}")
    void clearDefaultByUserId(@Param("userId") Long userId);

    @Update("UPDATE address SET is_default = 1 WHERE id = #{addressId} AND user_id = #{userId}")
    void setDefaultById(@Param("addressId") Long addressId, @Param("userId") Long userId);
}
