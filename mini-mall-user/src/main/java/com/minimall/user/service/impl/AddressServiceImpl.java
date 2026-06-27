package com.minimall.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.user.entity.Address;
import com.minimall.user.mapper.AddressMapper;
import com.minimall.user.service.IAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Address Service 实现 — 跟单体一样, 全靠 ServiceImpl 白嫖
 */
@Service
public class AddressServiceImpl
        extends ServiceImpl<AddressMapper, Address>
        implements IAddressService {

    @Autowired
    private AddressMapper addressMapper;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        addressMapper.clearDefaultByUserId(userId);
        addressMapper.setDefaultById(addressId, userId);
    }
}
