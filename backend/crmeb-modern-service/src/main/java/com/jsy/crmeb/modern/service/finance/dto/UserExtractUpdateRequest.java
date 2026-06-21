package com.jsy.crmeb.modern.service.finance.dto;

import java.math.BigDecimal;

public class UserExtractUpdateRequest {
    private Integer id;
    private String realName;
    private String extractType;
    private String bankCode;
    private String bankAddress;
    private String alipayCode;
    private BigDecimal extractPrice;
    private String mark;
    private String wechat;
    private String bankName;
    private String qrcodeUrl;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
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
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getQrcodeUrl() { return qrcodeUrl; }
    public void setQrcodeUrl(String qrcodeUrl) { this.qrcodeUrl = qrcodeUrl; }
}
