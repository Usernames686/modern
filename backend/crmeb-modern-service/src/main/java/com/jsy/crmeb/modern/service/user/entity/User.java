package com.jsy.crmeb.modern.service.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eb_user")
public class User {
    @TableId("uid")
    private Integer uid;
    private String account;
    private String pwd;
    @TableField("real_name")
    private String realName;
    private String birthday;
    @TableField("card_id")
    private String cardId;
    private String mark;
    @TableField("partner_id")
    private Integer partnerId;
    @TableField("group_id")
    private String groupId;
    @TableField("tag_id")
    private String tagId;
    private String nickname;
    private String avatar;
    private Integer sex;
    private String country;
    private String phone;
    @TableField("add_ip")
    private String addIp;
    @TableField("last_ip")
    private String lastIp;
    @TableField("now_money")
    private BigDecimal nowMoney;
    @TableField("brokerage_price")
    private BigDecimal brokeragePrice;
    private Integer integral;
    private Integer experience;
    @TableField("sign_num")
    private Integer signNum;
    private Integer status;
    private Integer level;
    @TableField("spread_uid")
    private Integer spreadUid;
    @TableField("spread_time")
    private LocalDateTime spreadTime;
    @TableField("user_type")
    private String userType;
    @TableField("is_promoter")
    private Integer isPromoter;
    @TableField("pay_count")
    private Integer payCount;
    @TableField("spread_count")
    private Integer spreadCount;
    private String addres;
    private Integer adminid;
    @TableField("login_type")
    private String loginType;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    @TableField("clean_time")
    private LocalDateTime cleanTime;
    private Integer subscribe;
    @TableField("is_logoff")
    private Integer isLogoff;

    public Integer getUid() { return uid; }
    public void setUid(Integer uid) { this.uid = uid; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public Integer getPartnerId() { return partnerId; }
    public void setPartnerId(Integer partnerId) { this.partnerId = partnerId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddIp() { return addIp; }
    public void setAddIp(String addIp) { this.addIp = addIp; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public BigDecimal getNowMoney() { return nowMoney; }
    public void setNowMoney(BigDecimal nowMoney) { this.nowMoney = nowMoney; }
    public BigDecimal getBrokeragePrice() { return brokeragePrice; }
    public void setBrokeragePrice(BigDecimal brokeragePrice) { this.brokeragePrice = brokeragePrice; }
    public Integer getIntegral() { return integral; }
    public void setIntegral(Integer integral) { this.integral = integral; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
    public Integer getSignNum() { return signNum; }
    public void setSignNum(Integer signNum) { this.signNum = signNum; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSpreadUid() { return spreadUid; }
    public void setSpreadUid(Integer spreadUid) { this.spreadUid = spreadUid; }
    public LocalDateTime getSpreadTime() { return spreadTime; }
    public void setSpreadTime(LocalDateTime spreadTime) { this.spreadTime = spreadTime; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Integer getIsPromoter() { return isPromoter; }
    public void setIsPromoter(Integer isPromoter) { this.isPromoter = isPromoter; }
    public Integer getPayCount() { return payCount; }
    public void setPayCount(Integer payCount) { this.payCount = payCount; }
    public Integer getSpreadCount() { return spreadCount; }
    public void setSpreadCount(Integer spreadCount) { this.spreadCount = spreadCount; }
    public String getAddres() { return addres; }
    public void setAddres(String addres) { this.addres = addres; }
    public Integer getAdminid() { return adminid; }
    public void setAdminid(Integer adminid) { this.adminid = adminid; }
    public String getLoginType() { return loginType; }
    public void setLoginType(String loginType) { this.loginType = loginType; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public LocalDateTime getCleanTime() { return cleanTime; }
    public void setCleanTime(LocalDateTime cleanTime) { this.cleanTime = cleanTime; }
    public Integer getSubscribe() { return subscribe; }
    public void setSubscribe(Integer subscribe) { this.subscribe = subscribe; }
    public Integer getIsLogoff() { return isLogoff; }
    public void setIsLogoff(Integer isLogoff) { this.isLogoff = isLogoff; }
}
