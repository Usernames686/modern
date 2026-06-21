package com.jsy.crmeb.modern.service.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eb_user_extract")
public class UserExtract {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    @TableField("real_name")
    private String realName;
    @TableField("extract_type")
    private String extractType;
    @TableField("bank_code")
    private String bankCode;
    @TableField("bank_address")
    private String bankAddress;
    @TableField("alipay_code")
    private String alipayCode;
    @TableField("extract_price")
    private BigDecimal extractPrice;
    private String mark;
    private BigDecimal balance;
    @TableField("fail_msg")
    private String failMsg;
    private Integer status;
    private String wechat;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("fail_time")
    private LocalDateTime failTime;
    @TableField("bank_name")
    private String bankName;
    @TableField("qrcode_url")
    private String qrcodeUrl;

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
}
