package com.jsy.crmeb.modern.service.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StoreCouponUserResponse {
    private Integer id;
    private Integer couponId;
    private Integer cid;
    private Integer uid;
    private String name;
    private BigDecimal money;
    private BigDecimal minPrice;
    private String type;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime useTime;
    private String nickname;
    private String avatar;
    private Boolean isValid;
    private Integer useType;
    private String primaryKey;
    private String validStr;
    private String useStartTimeStr;
    private String useEndTimeStr;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCouponId() { return couponId; }
    public void setCouponId(Integer couponId) { this.couponId = couponId; }
    public Integer getCid() { return cid; }
    public void setCid(Integer cid) { this.cid = cid; }
    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMoney() { return money; }
    public void setMoney(BigDecimal money) { this.money = money; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getUseTime() { return useTime; }
    public void setUseTime(LocalDateTime useTime) { this.useTime = useTime; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    public Integer getUseType() { return useType; }
    public void setUseType(Integer useType) { this.useType = useType; }
    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
    public String getValidStr() { return validStr; }
    public void setValidStr(String validStr) { this.validStr = validStr; }
    public String getUseStartTimeStr() { return useStartTimeStr; }
    public void setUseStartTimeStr(String useStartTimeStr) { this.useStartTimeStr = useStartTimeStr; }
    public String getUseEndTimeStr() { return useEndTimeStr; }
    public void setUseEndTimeStr(String useEndTimeStr) { this.useEndTimeStr = useEndTimeStr; }
}
