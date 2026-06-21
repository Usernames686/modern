package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponUserMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component("CouponOverdueTask")
public class CouponOverdueTask {
    private final StoreCouponUserMapper storeCouponUserMapper;

    public CouponOverdueTask(StoreCouponUserMapper storeCouponUserMapper) {
        this.storeCouponUserMapper = storeCouponUserMapper;
    }

    public void couponOverdue() {
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        storeCouponUserMapper.markExpiredUsableCoupons(tomorrowStart, LocalDateTime.now());
    }
}
