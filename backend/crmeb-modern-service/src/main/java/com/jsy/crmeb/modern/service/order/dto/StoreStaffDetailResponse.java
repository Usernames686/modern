package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;

public class StoreStaffDetailResponse {
    private long count;
    private BigDecimal price = BigDecimal.ZERO;
    private String time;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
