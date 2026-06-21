package com.jsy.crmeb.modern.service.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreCouponMapper extends BaseMapper<StoreCoupon> {
    @Update("""
            update eb_store_coupon
            set name = #{coupon.name},
                money = #{coupon.money},
                is_limited = #{coupon.isLimited},
                total = #{coupon.total},
                last_total = #{coupon.lastTotal},
                use_type = #{coupon.useType},
                primary_key = #{coupon.primaryKey},
                min_price = #{coupon.minPrice},
                receive_start_time = #{coupon.receiveStartTime},
                receive_end_time = #{coupon.receiveEndTime},
                is_fixed_time = #{coupon.isFixedTime},
                use_start_time = #{coupon.useStartTime},
                use_end_time = #{coupon.useEndTime},
                day = #{coupon.day},
                type = #{coupon.type},
                sort = #{coupon.sort},
                status = #{coupon.status},
                is_del = #{coupon.isDel},
                update_time = current_timestamp
            where id = #{coupon.id}
              and is_del = 0
            """)
    int updateCoupon(@Param("coupon") StoreCoupon coupon);
}
