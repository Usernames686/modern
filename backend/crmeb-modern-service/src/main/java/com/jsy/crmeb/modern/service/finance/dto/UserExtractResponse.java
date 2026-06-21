package com.jsy.crmeb.modern.service.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserExtractResponse {
    private Integer id;
    private Integer uid;
    private String realName;
    private String extractType;
    private String bankCode;
    private String bankAddress;
    private String alipayCode;
    private BigDecimal extractPrice;
    private String mark;
    private BigDecimal balance;
    private String failMsg;
    private Integer status;
    private String wechat;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime failTime;
    private String bankName;
    private String qrcodeUrl;
    private String nickName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getExtractType() { return extractType; }
    public void setExtractType(String extractType) { this.extractType = extractType; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankAddress() { return bankAddress; }
    public void setBankAddress(String bankAddress) { this.bankAddress = bankAddress; }
    public String getAlipayCode() { return alipayCode; }
    public void setAlipayCode(String alipayCode) { this.alipayCode = alipayCode; }
    public BigDecimal getExtractPrice() { return extractPrice; }
    public void setExtractPrice(BigDecimal extractPrice) { this.extractPrice = extractPrice; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getFailMsg() { return failMsg; }
    public void setFailMsg(String failMsg) { this.failMsg = failMsg; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getFailTime() { return failTime; }
    public void setFailTime(LocalDateTime failTime) { this.failTime = failTime; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getQrcodeUrl() { return qrcodeUrl; }
    public void setQrcodeUrl(String qrcodeUrl) { this.qrcodeUrl = qrcodeUrl; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
}
