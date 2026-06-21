package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.coupon.StoreCouponUser;

import java.util.List;
import java.util.Map;

/**
 * 优惠券记录表 Mapper 接口
 */
public interface StoreCouponUserDao extends BaseMapper<StoreCouponUser> {
    List<StoreCouponUser> getListByPreOrderNo(Map<String, Object> map);
}
