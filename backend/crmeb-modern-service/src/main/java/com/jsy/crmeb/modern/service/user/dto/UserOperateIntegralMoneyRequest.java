package com.jsy.crmeb.modern.service.user.dto;

import java.math.BigDecimal;

public class UserOperateIntegralMoneyRequest {
    private Integer uid;
    private Integer integralType;
    private Integer integralValue = 0;
    private Integer moneyType;
    private BigDecimal moneyValue = BigDecimal.ZERO;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public Integer getIntegralType() { return integralType; }
    public void setIntegralType(Integer integralType) { this.integralType = integralType; }
    public Integer getIntegralValue() { return integralValue; }
    public void setIntegralValue(Integer integralValue) { this.integralValue = integralValue; }
    public Integer getMoneyType() { return moneyType; }
    public void setMoneyType(Integer moneyType) { this.moneyType = moneyType; }
    public BigDecimal getMoneyValue() { return moneyValue; }
    public void setMoneyValue(BigDecimal moneyValue) { this.moneyValue = moneyValue; }
}
