package com.jsy.crmeb.modern.service.distribution.dto;

import java.math.BigDecimal;

public class RetailShopConfigRequest {
    private Integer brokerageFuncStatus;
    private Integer storeBrokerageQuota;
    private Integer storeBrokerageRatio;
    private Integer storeBrokerageTwo;
    private Integer brokerageBindind;
    private BigDecimal userExtractMinPrice;
    private String userExtractBank;
    private Integer extractTime;
    private Integer storeBrokerageIsBubble;

    public Integer getBrokerageFuncStatus() { return brokerageFuncStatus; }
    public void setBrokerageFuncStatus(Integer brokerageFuncStatus) { this.brokerageFuncStatus = brokerageFuncStatus; }
    public Integer getStoreBrokerageQuota() { return storeBrokerageQuota; }
    public void setStoreBrokerageQuota(Integer storeBrokerageQuota) { this.storeBrokerageQuota = storeBrokerageQuota; }
    public Integer getStoreBrokerageRatio() { return storeBrokerageRatio; }
    public void setStoreBrokerageRatio(Integer storeBrokerageRatio) { this.storeBrokerageRatio = storeBrokerageRatio; }
    public Integer getStoreBrokerageTwo() { return storeBrokerageTwo; }
    public void setStoreBrokerageTwo(Integer storeBrokerageTwo) { this.storeBrokerageTwo = storeBrokerageTwo; }
    public Integer getBrokerageBindind() { return brokerageBindind; }
    public void setBrokerageBindind(Integer brokerageBindind) { this.brokerageBindind = brokerageBindind; }
    public BigDecimal getUserExtractMinPrice() { return userExtractMinPrice; }
    public void setUserExtractMinPrice(BigDecimal userExtractMinPrice) { this.userExtractMinPrice = userExtractMinPrice; }
    public String getUserExtractBank() { return userExtractBank; }
    public void setUserExtractBank(String userExtractBank) { this.userExtractBank = userExtractBank; }
    public Integer getExtractTime() { return extractTime; }
    public void setExtractTime(Integer extractTime) { this.extractTime = extractTime; }
    public Integer getStoreBrokerageIsBubble() { return storeBrokerageIsBubble; }
    public void setStoreBrokerageIsBubble(Integer storeBrokerageIsBubble) { this.storeBrokerageIsBubble = storeBrokerageIsBubble; }
}
