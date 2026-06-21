package com.jsy.crmeb.modern.service.front.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class FrontUserExtractRequest {
    @JsonProperty("name")
    private String realName;
    private String extractType;
    @JsonProperty("cardum")
    private String bankCode;
    private String bankName;
    private String alipayCode;
    @JsonProperty("money")
    private BigDecimal extractPrice;
    private String wechat;
    private String mark;
    private String qrcodeUrl;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getExtractType() { return extractType; }
    public void setExtractType(String extractType) { this.extractType = extractType; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAlipayCode() { return alipayCode; }
    public void setAlipayCode(String alipayCode) { this.alipayCode = alipayCode; }
    public BigDecimal getExtractPrice() { return extractPrice; }
    public void setExtractPrice(BigDecimal extractPrice) { this.extractPrice = extractPrice; }
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public String getQrcodeUrl() { return qrcodeUrl; }
    public void setQrcodeUrl(String qrcodeUrl) { this.qrcodeUrl = qrcodeUrl; }
}
