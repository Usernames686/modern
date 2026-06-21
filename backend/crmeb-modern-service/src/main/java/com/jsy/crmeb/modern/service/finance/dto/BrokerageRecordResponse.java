package com.jsy.crmeb.modern.service.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BrokerageRecordResponse {
    private Integer id;
    private Integer uid;
    private String linkId;
    private String linkType;
    private Integer type;
    private String title;
    private BigDecimal price;
    private BigDecimal balance;
    private String mark;
    private Integer status;
    private Integer frozenTime;
    private Long thawTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer brokerageLevel;
    private String userName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getFrozenTime() {
        return frozenTime;
    }

    public void setFrozenTime(Integer frozenTime) {
        this.frozenTime = frozenTime;
    }

    public Long getThawTime() {
        return thawTime;
    }

    public void setThawTime(Long thawTime) {
        this.thawTime = thawTime;
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

    public Integer getBrokerageLevel() {
        return brokerageLevel;
    }

    public void setBrokerageLevel(Integer brokerageLevel) {
        this.brokerageLevel = brokerageLevel;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
