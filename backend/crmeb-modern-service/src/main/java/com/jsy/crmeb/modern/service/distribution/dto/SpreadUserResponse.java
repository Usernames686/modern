package com.jsy.crmeb.modern.service.distribution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SpreadUserResponse {
    private Integer uid;
    private String realName;
    private String nickname;
    private String avatar;
    private String phone;
    private BigDecimal brokeragePrice;
    private Integer spreadUid;
    private String spreadNickname;
    private Integer payCount;
    private Integer spreadCount;
    private Integer spreadOrderNum;
    private BigDecimal spreadOrderTotalPrice;
    private BigDecimal totalBrokeragePrice;
    private BigDecimal extractCountPrice;
    private Integer extractCountNum;
    private BigDecimal freezeBrokeragePrice;
    private LocalDateTime promoterTime;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getBrokeragePrice() { return brokeragePrice; }
    public void setBrokeragePrice(BigDecimal brokeragePrice) { this.brokeragePrice = brokeragePrice; }
    public Integer getSpreadUid() { return spreadUid; }
    public void setSpreadUid(Integer spreadUid) { this.spreadUid = spreadUid; }
    public String getSpreadNickname() { return spreadNickname; }
    public void setSpreadNickname(String spreadNickname) { this.spreadNickname = spreadNickname; }
    public Integer getPayCount() { return payCount; }
    public void setPayCount(Integer payCount) { this.payCount = payCount; }
    public Integer getSpreadCount() { return spreadCount; }
    public void setSpreadCount(Integer spreadCount) { this.spreadCount = spreadCount; }
    public Integer getSpreadOrderNum() { return spreadOrderNum; }
    public void setSpreadOrderNum(Integer spreadOrderNum) { this.spreadOrderNum = spreadOrderNum; }
    public BigDecimal getSpreadOrderTotalPrice() { return spreadOrderTotalPrice; }
    public void setSpreadOrderTotalPrice(BigDecimal spreadOrderTotalPrice) { this.spreadOrderTotalPrice = spreadOrderTotalPrice; }
    public BigDecimal getTotalBrokeragePrice() { return totalBrokeragePrice; }
    public void setTotalBrokeragePrice(BigDecimal totalBrokeragePrice) { this.totalBrokeragePrice = totalBrokeragePrice; }
    public BigDecimal getExtractCountPrice() { return extractCountPrice; }
    public void setExtractCountPrice(BigDecimal extractCountPrice) { this.extractCountPrice = extractCountPrice; }
    public Integer getExtractCountNum() { return extractCountNum; }
    public void setExtractCountNum(Integer extractCountNum) { this.extractCountNum = extractCountNum; }
    public BigDecimal getFreezeBrokeragePrice() { return freezeBrokeragePrice; }
    public void setFreezeBrokeragePrice(BigDecimal freezeBrokeragePrice) { this.freezeBrokeragePrice = freezeBrokeragePrice; }
    public LocalDateTime getPromoterTime() { return promoterTime; }
    public void setPromoterTime(LocalDateTime promoterTime) { this.promoterTime = promoterTime; }
}
