package com.jsy.crmeb.modern.service.coupon.dto;

import java.math.BigDecimal;

public class StoreCouponSaveRequest {
    private Integer id;
    private String name;
    private BigDecimal money;
    private Boolean isLimited;
    private Integer total;
    private Integer useType;
    private String primaryKey;
    private BigDecimal minPrice;
    private Boolean isForever;
    private String receiveStartTime;
    private String receiveEndTime;
    private Boolean isFixedTime;
    private String useStartTime;
    private String useEndTime;
    private Integer day;
    private Integer type;
    private Integer sort;
    private Boolean status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMoney() { return money; }
    public void setMoney(BigDecimal money) { this.money = money; }
    public Boolean getIsLimited() { return isLimited; }
    public void setIsLimited(Boolean isLimited) { this.isLimited = isLimited; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getUseType() { return useType; }
    public void setUseType(Integer useType) { this.useType = useType; }
    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public Boolean getIsForever() { return isForever; }
    public void setIsForever(Boolean isForever) { this.isForever = isForever; }
    public String getReceiveStartTime() { return receiveStartTime; }
    public void setReceiveStartTime(String receiveStartTime) { this.receiveStartTime = receiveStartTime; }
    public String getReceiveEndTime() { return receiveEndTime; }
    public void setReceiveEndTime(String receiveEndTime) { this.receiveEndTime = receiveEndTime; }
    public Boolean getIsFixedTime() { return isFixedTime; }
    public void setIsFixedTime(Boolean isFixedTime) { this.isFixedTime = isFixedTime; }
    public String getUseStartTime() { return useStartTime; }
    public void setUseStartTime(String useStartTime) { this.useStartTime = useStartTime; }
    public String getUseEndTime() { return useEndTime; }
    public void setUseEndTime(String useEndTime) { this.useEndTime = useEndTime; }
    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
}
