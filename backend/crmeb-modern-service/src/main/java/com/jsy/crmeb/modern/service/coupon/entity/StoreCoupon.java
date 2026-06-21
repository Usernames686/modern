package com.jsy.crmeb.modern.service.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eb_store_coupon")
public class StoreCoupon {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private BigDecimal money;
    @TableField("is_limited")
    private Boolean isLimited;
    private Integer total;
    @TableField("last_total")
    private Integer lastTotal;
    @TableField("use_type")
    private Integer useType;
    @TableField("primary_key")
    private String primaryKey;
    @TableField("min_price")
    private BigDecimal minPrice;
    @TableField("receive_start_time")
    private LocalDateTime receiveStartTime;
    @TableField("receive_end_time")
    private LocalDateTime receiveEndTime;
    @TableField("is_fixed_time")
    private Boolean isFixedTime;
    @TableField("use_start_time")
    private LocalDateTime useStartTime;
    @TableField("use_end_time")
    private LocalDateTime useEndTime;
    private Integer day;
    private Integer type;
    private Integer sort;
    private Boolean status;
    @TableField("is_del")
    private Boolean isDel;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public Boolean getIsLimited() {
        return isLimited;
    }

    public void setIsLimited(Boolean isLimited) {
        this.isLimited = isLimited;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getLastTotal() {
        return lastTotal;
    }

    public void setLastTotal(Integer lastTotal) {
        this.lastTotal = lastTotal;
    }

    public Integer getUseType() {
        return useType;
    }

    public void setUseType(Integer useType) {
        this.useType = useType;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public LocalDateTime getReceiveStartTime() {
        return receiveStartTime;
    }

    public void setReceiveStartTime(LocalDateTime receiveStartTime) {
        this.receiveStartTime = receiveStartTime;
    }

    public LocalDateTime getReceiveEndTime() {
        return receiveEndTime;
    }

    public void setReceiveEndTime(LocalDateTime receiveEndTime) {
        this.receiveEndTime = receiveEndTime;
    }

    public Boolean getIsFixedTime() {
        return isFixedTime;
    }

    public void setIsFixedTime(Boolean isFixedTime) {
        this.isFixedTime = isFixedTime;
    }

    public LocalDateTime getUseStartTime() {
        return useStartTime;
    }

    public void setUseStartTime(LocalDateTime useStartTime) {
        this.useStartTime = useStartTime;
    }

    public LocalDateTime getUseEndTime() {
        return useEndTime;
    }

    public void setUseEndTime(LocalDateTime useEndTime) {
        this.useEndTime = useEndTime;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getIsDel() {
        return isDel;
    }

    public void setIsDel(Boolean isDel) {
        this.isDel = isDel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
