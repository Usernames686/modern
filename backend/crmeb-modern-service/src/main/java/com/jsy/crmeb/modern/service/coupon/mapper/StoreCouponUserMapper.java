package com.jsy.crmeb.modern.service.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCouponUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreCouponUserMapper extends BaseMapper<StoreCouponUser> {
    @Update("""
            update eb_store_coupon_user
            set status = 2,
                update_time = #{updateTime}
            where status = 0
              and end_time is not null
              and end_time < #{tomorrowStart}
            """)
    int markExpiredUsableCoupons(
            @Param("tomorrowStart") LocalDateTime tomorrowStart,
            @Param("updateTime") LocalDateTime updateTime);
}
