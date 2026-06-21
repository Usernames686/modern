package com.jsy.crmeb.modern.service.order.dto;

import com.jsy.crmeb.modern.common.web.PageResponse;
import java.math.BigDecimal;

public class SystemWriteOffOrderResponse {
    private Long total = 0L;
    private BigDecimal orderTotalPrice = BigDecimal.ZERO;
    private BigDecimal refundTotalPrice = BigDecimal.ZERO;
    private Integer refundTotal = 0;
    private PageResponse<StoreOrderItemResponse> list;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public BigDecimal getOrderTotalPrice() {
        return orderTotalPrice;
    }

    public void setOrderTotalPrice(BigDecimal orderTotalPrice) {
        this.orderTotalPrice = orderTotalPrice;
    }

    public BigDecimal getRefundTotalPrice() {
        return refundTotalPrice;
    }

    public void setRefundTotalPrice(BigDecimal refundTotalPrice) {
        this.refundTotalPrice = refundTotalPrice;
    }

    public Integer getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(Integer refundTotal) {
        this.refundTotal = refundTotal;
    }

    public PageResponse<StoreOrderItemResponse> getList() {
        return list;
    }

    public void setList(PageResponse<StoreOrderItemResponse> list) {
        this.list = list;
    }
}
