package com.jsy.crmeb.modern.service.dashboard.dto;

import java.math.BigDecimal;

public class HomeOperatingDataResponse {
    private Integer notShippingOrderNum = 0;
    private Integer refundingOrderNum = 0;
    private Integer notWriteOffOrderNum = 0;
    private Integer vigilanceInventoryNum = 0;
    private Integer onSaleProductNum = 0;
    private Integer notSaleProductNum = 0;
    private Integer notAuditNum = 0;
    private BigDecimal totalRechargeAmount = BigDecimal.ZERO;

    public Integer getNotShippingOrderNum() { return notShippingOrderNum; }
    public void setNotShippingOrderNum(Integer notShippingOrderNum) { this.notShippingOrderNum = notShippingOrderNum; }
    public Integer getRefundingOrderNum() { return refundingOrderNum; }
    public void setRefundingOrderNum(Integer refundingOrderNum) { this.refundingOrderNum = refundingOrderNum; }
    public Integer getNotWriteOffOrderNum() { return notWriteOffOrderNum; }
    public void setNotWriteOffOrderNum(Integer notWriteOffOrderNum) { this.notWriteOffOrderNum = notWriteOffOrderNum; }
    public Integer getVigilanceInventoryNum() { return vigilanceInventoryNum; }
    public void setVigilanceInventoryNum(Integer vigilanceInventoryNum) { this.vigilanceInventoryNum = vigilanceInventoryNum; }
    public Integer getOnSaleProductNum() { return onSaleProductNum; }
    public void setOnSaleProductNum(Integer onSaleProductNum) { this.onSaleProductNum = onSaleProductNum; }
    public Integer getNotSaleProductNum() { return notSaleProductNum; }
    public void setNotSaleProductNum(Integer notSaleProductNum) { this.notSaleProductNum = notSaleProductNum; }
    public Integer getNotAuditNum() { return notAuditNum; }
    public void setNotAuditNum(Integer notAuditNum) { this.notAuditNum = notAuditNum; }
    public BigDecimal getTotalRechargeAmount() { return totalRechargeAmount; }
    public void setTotalRechargeAmount(BigDecimal totalRechargeAmount) { this.totalRechargeAmount = totalRechargeAmount; }
}
